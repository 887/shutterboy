package com.eight87.shutterboy.data.tagging

import android.util.Log
import com.eight87.shutterboy.data.db.PhotoDao
import com.eight87.shutterboy.data.db.PhotoTagDao
import com.eight87.shutterboy.data.db.PhotoTagEntity
import com.eight87.shutterboy.data.db.PhotoTagStatusEntity
import com.eight87.shutterboy.data.settings.TaggingPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/** What the tagger is doing right now, for the Settings screen. */
sealed interface TaggingRunState {
    /** Enabled + available, nothing left to do (or not started yet). */
    data object Idle : TaggingRunState
    /** Master toggle is off. */
    data object Disabled : TaggingRunState
    /** Device has neither Play Services labeling nor AICore captioning. */
    data object Unsupported : TaggingRunState
    /** A pass is running. */
    data class Running(val processed: Int, val total: Int) : TaggingRunState
}

/** Aggregate counts + approximate on-disk footprint for the Settings screen. */
data class TaggingStats(
    val processedCount: Int,
    val totalPhotos: Int,
    val tagCount: Int,
    val approxBytes: Long,
)

/**
 * Smart Tags orchestration. Runs the on-device tagger as a DOWNSTREAM
 * background pass — it is deliberately NOT wired into the scan loop, so
 * scanning keeps filling the gallery at full speed. The pass is triggered
 * after each scan completes (and once at startup) by [AppGraph], walks
 * photos that have no status row, decodes a small bitmap, tags it, and
 * records a status marker so each photo is processed exactly once.
 *
 * Single-flight (one pass at a time), cancellable (toggling off / wiping /
 * re-tagging stops it promptly), and resilient (a bad image is skipped, not
 * fatal — the tagger never throws).
 */
class AiTaggingService(
    private val tagger: ImageTagger,
    private val bitmapSource: TaggingBitmapSource,
    private val photoDao: PhotoDao,
    private val tagDao: PhotoTagDao,
    private val preferences: TaggingPreferences,
    private val scope: CoroutineScope,
) {
    private val _runState = MutableStateFlow<TaggingRunState>(TaggingRunState.Idle)
    val runState: StateFlow<TaggingRunState> = _runState

    @Volatile private var job: Job? = null

    init {
        // React to the master toggle: enabling kicks a pass, disabling
        // stops the current one. distinctUntilChanged so we don't churn.
        preferences.observeEnabled()
            .distinctUntilChanged()
            .onEach { enabled ->
                if (enabled) start() else stop(TaggingRunState.Disabled)
            }
            .launchIn(scope)
    }

    /** One-shot device-capability probe for the Settings screen. */
    suspend fun probeAvailability(): TaggerAvailability = tagger.availability()

    fun observeStats(): Flow<TaggingStats> = combine(
        tagDao.observeProcessedCount(),
        tagDao.observeTotalPhotos(),
        tagDao.observeTagCount(),
        tagDao.observeLabelByteSum(),
    ) { processed, total, tags, labelBytes ->
        val approx = labelBytes +
            tags.toLong() * TAG_ROW_OVERHEAD_BYTES +
            processed.toLong() * STATUS_ROW_BYTES
        TaggingStats(
            processedCount = processed,
            totalPhotos = total,
            tagCount = tags,
            approxBytes = approx,
        )
    }

    /** Start (or no-op if already running) a background tagging pass. */
    @Synchronized
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch { runPass() }
    }

    /** Wipe processing markers + tags, then re-run from scratch. */
    fun retagAll() {
        scope.launch {
            job?.cancelAndJoin()
            tagDao.wipeStatus()
            tagDao.wipeTags()
            start()
        }
    }

    /** Stop any pass and delete all tag data. */
    fun wipe() {
        scope.launch {
            job?.cancelAndJoin()
            tagDao.wipeStatus()
            tagDao.wipeTags()
            _runState.value =
                if (preferences.observeEnabled().first()) TaggingRunState.Idle
                else TaggingRunState.Disabled
        }
    }

    private fun stop(state: TaggingRunState) {
        job?.cancel()
        job = null
        _runState.value = state
    }

    private suspend fun CoroutineScope.runPass() {
        if (!preferences.observeEnabled().first()) {
            _runState.value = TaggingRunState.Disabled
            return
        }
        val availability = tagger.availability()
        if (!availability.any) {
            _runState.value = TaggingRunState.Unsupported
            return
        }
        val wantCaption =
            availability.captionsAvailable && preferences.observeCaptionsEnabled().first()

        val total = tagDao.observeTotalPhotos().first()
        var processed = tagDao.observeProcessedCount().first()
        _runState.value = TaggingRunState.Running(processed, total)

        while (isActive) {
            val ids = tagDao.untaggedPhotoIds(CHUNK)
            if (ids.isEmpty()) break
            val byId = photoDao.byIds(ids).associateBy { it.id }
            for (id in ids) {
                if (!isActive) return
                // Cheap re-check so toggling off mid-pass stops within an image.
                if (!preferences.observeEnabled().first()) {
                    _runState.value = TaggingRunState.Disabled
                    return
                }
                tagOne(id, byId[id]?.contentUri, wantCaption)
                processed += 1
                _runState.value = TaggingRunState.Running(processed, total)
                yield()
            }
        }
        if (isActive) _runState.value = TaggingRunState.Idle
    }

    private suspend fun tagOne(photoId: Long, contentUri: String?, wantCaption: Boolean) {
        val candidates = if (contentUri == null) {
            emptyList()
        } else {
            val bitmap = bitmapSource.decode(contentUri)
            if (bitmap == null) emptyList() else tagger.analyze(bitmap, wantCaption)
        }
        if (candidates.isNotEmpty()) {
            tagDao.upsertTags(
                candidates.map {
                    PhotoTagEntity(
                        photoId = photoId,
                        label = it.label,
                        confidence = it.confidence,
                        source = it.source.stored,
                    )
                },
            )
        }
        // Always mark processed — even zero-tag photos — so we don't
        // re-analyse them on every scan.
        tagDao.markStatus(
            PhotoTagStatusEntity(
                photoId = photoId,
                status = "done",
                taggedAtMs = System.currentTimeMillis(),
                engine = ENGINE,
            ),
        )
        Log.v(TAG, "tagged photo $photoId: ${candidates.size} candidate(s)")
    }

    private companion object {
        const val TAG = "shutterboy.tagging"
        const val ENGINE = "mlkit-1"
        const val CHUNK = 64
        // The tags share the single SQLite file, so an exact isolated size
        // isn't queryable. These per-row constants turn row counts into an
        // honest "≈" footprint alongside the actual label text bytes.
        const val TAG_ROW_OVERHEAD_BYTES = 48L
        const val STATUS_ROW_BYTES = 48L
    }
}
