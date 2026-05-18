package com.eight87.shutterboy.data.repo

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.eight87.shutterboy.data.db.FolderDao
import com.eight87.shutterboy.data.db.FolderEntity
import com.eight87.shutterboy.data.db.PhotoDao
import com.eight87.shutterboy.data.db.PhotoEntity
import com.eight87.shutterboy.data.db.PhotoFavoriteDao
import com.eight87.shutterboy.data.db.PhotoFavoriteEntity
import com.eight87.shutterboy.data.db.PhotoSearchDao
import com.eight87.shutterboy.data.db.toDomain
import com.eight87.shutterboy.data.db.toEntity
import com.eight87.shutterboy.data.saf.SafSourceManager
import com.eight87.shutterboy.data.scan.ExifEnricher
import com.eight87.shutterboy.data.scan.MediaImagesPermission
import com.eight87.shutterboy.data.scan.MediaStoreGenerationSource
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.scan.ScannedPhoto
import com.eight87.shutterboy.data.settings.NoOpRecentSearchesPreferences
import com.eight87.shutterboy.data.settings.RecentSearchesPreferences
import com.eight87.shutterboy.data.settings.ScanConfigSource
import com.eight87.shutterboy.data.settings.ScanGatePreferences
import com.eight87.shutterboy.domain.DeleteRequest
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.LibrarySnapshot
import com.eight87.shutterboy.domain.MediaChange
import com.eight87.shutterboy.domain.MoveRequest
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.domain.ScanProgress
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Phase B.5 — concrete implementation behind every facet interface.
 * `AppGraph` exposes the eight facets separately so consumers depend only on
 * what they read. This class is the single Room/MediaStore binding point.
 *
 * Sort routing in [observePhotos] / [observePhotosInFolder] pushes the sort
 * to the database via Room's `@RawQuery` + `SimpleSQLiteQuery`, with the
 * `ORDER BY` fragment sourced from the sealed `PhotoSort` variants — never
 * user input, so injection-safe by construction.
 */
class RoomGalleryRepository(
    private val context: Context,
    private val photoDao: PhotoDao,
    private val folderDao: FolderDao,
    private val favoriteDao: PhotoFavoriteDao,
    private val searchDao: PhotoSearchDao,
    private val mediaStoreScanner: MediaStoreScanner,
    private val exifEnricher: ExifEnricher,
    private val safSourceManager: SafSourceManager,
    private val scanConfig: ScanConfigSource,
    private val scanGate: ScanGatePreferences,
    private val mediaStoreGeneration: MediaStoreGenerationSource =
        MediaStoreGenerationSource.Default(context),
    private val recentSearchesPrefs: RecentSearchesPreferences = NoOpRecentSearchesPreferences,
) : PhotoSource,
    FolderSource,
    PhotoSearch,
    LibraryScanner,
    FavoriteCommands,
    PhotoDeleter,
    PhotoMover,
    MediaChangeSource {

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)

    // R.F.25 — single-flight guard so cold-start LaunchedEffect racing with the
    // permission-grant onGranted (or rapid Reset-cache taps) doesn't fire two
    // concurrent scans. DAO writes are idempotent so the end-state was always
    // consistent, but duplicate MediaStore + SAF + EXIF I/O and interleaved
    // progress emissions are wasted work that hurts UX.
    private val scanMutex = Mutex()

    // --- PhotoSource ---

    override fun observePhotos(sort: PhotoSort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM photos ORDER BY ${sort.sqlOrderBy}",
        )
        return photoDao.observeAll(query)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    override fun observePhotosInFolder(folderId: FolderId, sort: PhotoSort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM photos WHERE folder_id = ? ORDER BY ${sort.sqlOrderBy}",
            arrayOf<Any>(folderId.value),
        )
        return photoDao.observeAll(query)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(Dispatchers.Default)
    }

    override suspend fun photosByIds(ids: List<Long>): List<Photo> =
        photoDao.byIds(ids).map { it.toDomain() }

    override fun observePhotoById(id: Long): Flow<Photo?> =
        photoDao.observeById(id).map { it?.toDomain() }

    // --- FolderSource ---

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeFolder(id: FolderId): Flow<Folder?> =
        folderDao.observeById(id.value).map { it?.toDomain() }

    override fun observeFolderCovers(): Flow<Map<FolderId, Photo>> =
        folderDao.observeAll().map { folderRows ->
            val coverIdToFolder: Map<Long, FolderId> = folderRows
                .mapNotNull { it.coverPhotoId?.let { cover -> cover to FolderId(it.id) } }
                .toMap()
            if (coverIdToFolder.isEmpty()) {
                emptyMap()
            } else {
                val photos = photoDao.byIds(coverIdToFolder.keys.toList())
                photos.associate { entity ->
                    val folderId = coverIdToFolder.getValue(entity.id)
                    folderId to entity.toDomain()
                }
            }
        }


    // --- PhotoSearch ---

    override fun searchPhotos(query: String): Flow<List<Photo>> {
        val sanitized = query.trim()
        if (sanitized.isBlank()) return flowOf(emptyList())
        val ftsExpr = sanitized.split(Regex("\\s+"))
            .joinToString(" AND ") { token ->
                val safe = token.replace(Regex("[\"\\^*]"), "")
                if (safe.isBlank()) "" else "$safe*"
            }
            .trim()
        return if (ftsExpr.isBlank()) flowOf(emptyList())
        else searchDao.observeMatching(ftsExpr).map { rows -> rows.map { it.toDomain() } }
    }

    override fun recentSearches(): Flow<List<String>> = recentSearchesPrefs.observe()
    override suspend fun recordSearch(query: String) {
        recentSearchesPrefs.record(query)
    }

    // --- LibraryScanner ---

    override suspend fun runScan(): LibrarySnapshot {
        _scanProgress.value = ScanProgress.Running(processed = 0, total = null)
        return runCatching { executeScan() }
            .onSuccess { snap -> _scanProgress.value = ScanProgress.Done(snap.deltaCount) }
            .getOrElse { e ->
                _scanProgress.value = ScanProgress.Failed(e.message ?: e::class.simpleName ?: "scan failed")
                LibrarySnapshot(photos = emptyList(), folders = emptyList(), deltaCount = 0)
            }
    }

    override suspend fun scanIfChanged(): LibrarySnapshot {
        val volume = MediaStore.VOLUME_EXTERNAL_PRIMARY
        val currentGen = mediaStoreGeneration.current(volume)
        val persistedGen = scanGate.observeMediaStoreGeneration(volume).first()
        val safUris = scanConfig.safSourceUris.first()
        val currentSaf = safSourceManager.fingerprint(safUris)
        val persistedSaf = scanGate.observeSafFingerprint().first()

        val mediaStoreMatches =
            currentGen != MediaStoreGenerationSource.ALWAYS_RESCAN && currentGen == persistedGen
        val safMatches = currentSaf == persistedSaf
        val permGranted = MediaImagesPermission.isGranted(context)
        Log.i(
            "shutterboy",
            "scanIfChanged: currentGen=$currentGen persistedGen=$persistedGen " +
                "mediaStoreMatches=$mediaStoreMatches safMatches=$safMatches permGranted=$permGranted",
        )

        if (mediaStoreMatches && safMatches) {
            Log.i("shutterboy", "scanIfChanged: skipped scan, gate matches")
            _scanProgress.value = ScanProgress.Done(0)
            return LibrarySnapshot(photos = emptyList(), folders = emptyList(), deltaCount = 0)
        }

        _scanProgress.value = ScanProgress.Running(processed = 0, total = null)
        return try {
            val snap = executeScan()
            Log.i("shutterboy", "scanIfChanged: scan complete deltaCount=${snap.deltaCount} photos=${snap.photos.size} folders=${snap.folders.size}")
            // Persist tokens ONLY after the scan succeeds — a crashed scan
            // must re-run next boot. Persist the MediaStore generation token
            // ONLY when the read permission is granted: a permission-denied
            // scan returns 0 rows silently, so persisting the token would
            // poison the gate and we'd never rescan after the user grants
            // the permission later. The SAF fingerprint is permission-
            // independent (per-tree URI grants are persistent) and always
            // persisted.
            if (MediaImagesPermission.isGranted(context)) {
                scanGate.setMediaStoreGeneration(volume, currentGen)
            }
            scanGate.setSafFingerprint(currentSaf)
            _scanProgress.value = ScanProgress.Done(snap.deltaCount)
            snap
        } catch (e: Throwable) {
            Log.e("shutterboy", "scanIfChanged: scan failed", e)
            _scanProgress.value = ScanProgress.Failed(e.message ?: e::class.simpleName ?: "scan failed")
            LibrarySnapshot(photos = emptyList(), folders = emptyList(), deltaCount = 0)
        }
    }

    override suspend fun forceRescan(): LibrarySnapshot {
        scanGate.clear()
        return scanIfChanged()
    }

    /**
     * Phase I.3.d — destructive reset. Wipes every Room row this
     * repository owns (favorites first because of FK against photos,
     * then photos which cascades into the `photo_fts` content shadow,
     * then folders, plus a defensive `photo_fts` truncate) and clears
     * the cold-start scan gate. Then re-runs the full scan via the
     * normal [scanIfChanged] path, which now sees an empty gate + empty
     * DB and re-walks everything from scratch.
     *
     * Sequential (not a single SQLite transaction) — the DAO layer
     * doesn't expose a `withTransaction` block from here, and this is
     * a user-initiated wipe so a mid-flight crash leaves the cache
     * empty, which is the same observable state as "we never
     * succeeded": the next launch's `scanIfChanged` rebuilds from
     * zero.
     */
    override suspend fun resetAndRescan(): LibrarySnapshot {
        Log.i("shutterboy", "resetAndRescan: wiping Room cache + scan gate")
        _scanProgress.value = ScanProgress.Running(processed = 0, total = null)
        // FK order: favorites first (depend on photos), then photos
        // (cascades into the `photo_fts` content shadow), then folders.
        // `searchDao.deleteAll()` is a defensive second pass over the
        // FTS shadow — already empty after the photos truncate, but
        // keeps the wipe explicit rather than implicit.
        favoriteDao.deleteAll()
        photoDao.deleteAll()
        searchDao.deleteAll()
        folderDao.deleteAll()
        scanGate.clear()
        return scanIfChanged()
    }

    private suspend fun executeScan(): LibrarySnapshot = scanMutex.withLock {
        // Run the whole scan body on IO so the per-item EXIF enrichment loop
        // doesn't bounce 30k+ times back to the caller's dispatcher (the
        // cold-start LaunchedEffect / lifecycleScope.launch are Main).
        // Each `enrich()` already withContext(IO)s internally, but without
        // this wrapper every iteration round-trips back through Main, which
        // is what locks up the UI on a real-phone library of 30k photos.
        withContext(Dispatchers.IO) {
        // 1. Scan device media-store + SAF trees
        val device = mediaStoreScanner.scanDeviceMediaStore()
        val safUris = scanConfig.safSourceUris.first()
        val saf = safSourceManager.scanSafTrees(safUris)
        // R.F.26 — externally-revoked SAF URIs detected by the manager get
        // pruned from the persisted set so the next observation drops them
        // from the Manage Sources list. No-op when nothing was revoked.
        if (saf.revokedUris.isNotEmpty()) {
            scanConfig.pruneRevoked(saf.revokedUris)
        }

        val combinedPhotos: List<ScannedPhoto> = device.photos + saf.photos
        val combinedFolders = device.folders + saf.folders

        val total = combinedPhotos.size
        // Emit a fresh Running with total once we know it. Subsequent
        // emissions happen as enrichment walks each item, throttled to
        // ~5 Hz (SCAN_PROGRESS_THROTTLE_MS) to keep the UI thread free
        // without losing visible fidelity. Tonearmboy R.F lesson.
        _scanProgress.value = ScanProgress.Running(processed = 0, total = total, currentTitle = null)

        // 2. Diff vs cache: only new photos need EXIF; existing rows keep cached EXIF.
        val cachedIds = photoDao.allIds().toSet()
        val toEnrich = combinedPhotos.filter { it.id !in cachedIds }
        val byId = combinedPhotos.associateBy { it.id }.toMutableMap()

        var processed = 0
        var lastEmitMs = 0L
        var lastEmittedProcessed = -1
        var lastEmittedTitle: String? = null
        // Walk every combined photo so the bar fills the whole library,
        // not just the to-enrich slice. Enrichment is the slow per-item
        // I/O work; cached items pass through without an open().
        val toEnrichIds = toEnrich.map { it.id }.toHashSet()
        for (photo in combinedPhotos) {
            if (photo.id in toEnrichIds) {
                val enriched = exifEnricher.enrich(photo)
                byId[enriched.id] = enriched
            }
            processed += 1
            val now = android.os.SystemClock.uptimeMillis()
            val terminal = processed == total
            val timeOk = terminal || now - lastEmitMs >= SCAN_PROGRESS_THROTTLE_MS
            // Skip the emission if the cadence allows but nothing changed
            // since the last emit. Saves a no-op StateFlow set + a wasted
            // Compose recomposition on 30k-photo cold scans. Always fire
            // the terminal `processed == total` emission so the bar lands
            // on 100% and the strip collapses cleanly.
            val changed = terminal ||
                processed != lastEmittedProcessed ||
                photo.displayName != lastEmittedTitle
            if (timeOk && changed) {
                lastEmitMs = now
                lastEmittedProcessed = processed
                lastEmittedTitle = photo.displayName
                _scanProgress.value = ScanProgress.Running(
                    processed = processed,
                    total = total,
                    currentTitle = photo.displayName,
                )
            }
        }

        // 3. Map to entities
        val photoEntities = byId.values.map { it.toEntity() }
        val folderEntities = combinedFolders.map { f ->
            val photoCount = combinedPhotos.count { it.folderId == f.id }
            val cover = combinedPhotos.firstOrNull { it.folderId == f.id }?.id
            FolderEntity(
                id = f.id,
                displayName = f.displayName,
                sourceType = if (f.source == ScannedPhoto.ScanSource.SAF_TREE) "SAF" else "DEVICE",
                safTreeUri = f.safTreeUri?.toString(),
                photoCount = photoCount,
                coverPhotoId = cover,
            )
        }

        // 4. Apply delta. FK order: upsert folders BEFORE photos (photos.folder_id
        //    references folders.id), then delete dangling photos, then dangling
        //    folders last so no photo still references one we're about to drop.
        val cachedFolderIds = folderDao.allIds().toSet()
        val seenFolderIds = folderEntities.map(FolderEntity::id).toSet()
        val toDeleteFolders = cachedFolderIds - seenFolderIds
        folderDao.upsertAll(folderEntities)

        val seenIds = photoEntities.map(PhotoEntity::id).toSet()
        val toDeletePhotos = (cachedIds - seenIds).toList()
        photoDao.replaceWithDelta(toUpsert = photoEntities, toDelete = toDeletePhotos)

        for (id in toDeleteFolders) folderDao.deleteById(id)

        val deltaCount = photoEntities.size - cachedIds.intersect(seenIds).size + toDeletePhotos.size
        LibrarySnapshot(
            photos = photoEntities.map { it.toDomain() },
            folders = folderEntities.map { it.toDomain() },
            deltaCount = deltaCount,
        )
        } // withContext(IO)
    }

    override fun scanProgress(): Flow<ScanProgress> = _scanProgress

    // --- FavoriteCommands ---

    override suspend fun toggleFavorite(photoId: PhotoId): Boolean {
        val current = favoriteDao.isFavorite(photoId.value)
        if (current) favoriteDao.remove(photoId.value)
        else favoriteDao.add(PhotoFavoriteEntity(photoId.value))
        return !current
    }

    override suspend fun isFavorite(photoId: PhotoId): Boolean =
        favoriteDao.isFavorite(photoId.value)

    override fun observeIsFavorite(photoId: PhotoId): Flow<Boolean> =
        favoriteDao.observeIsFavorite(photoId.value)

    override fun observeFavoritePhotos(): Flow<List<Photo>> =
        favoriteDao.observeFavoritePhotos().map { rows -> rows.map { it.toDomain() } }

    override fun observeFavoriteIds(): Flow<Set<Long>> =
        favoriteDao.observeFavoritePhotos().map { rows -> rows.mapTo(HashSet()) { it.id } }

    // --- PhotoDeleter (three-branch SDK split mirroring tonearmboy's TrackDeleter) ---

    override suspend fun deletePhotos(ids: List<PhotoId>): DeleteRequest {
        if (ids.isEmpty()) return DeleteRequest.Immediate(deletedCount = 0)
        val resolver = context.contentResolver
        val uris = ids.map {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.value)
        }
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                runCatching {
                    val pi: PendingIntent = MediaStore.createDeleteRequest(resolver, uris)
                    DeleteRequest.Consent(pi)
                }.getOrElse { DeleteRequest.Failure(it.message ?: "createDeleteRequest failed") }
            }
            else -> {
                runCatching {
                    var deleted = 0
                    for (uri in uris) {
                        deleted += resolver.delete(uri, null, null)
                    }
                    DeleteRequest.Immediate(deleted)
                }.getOrElse { DeleteRequest.Failure(it.message ?: "delete failed") }
            }
        }
    }

    // --- PhotoMover (H.4 — RELATIVE_PATH update with consent on API 30+) ---

    override suspend fun moveToFolder(
        ids: List<PhotoId>,
        targetRelativePath: String,
    ): MoveRequest {
        if (ids.isEmpty()) return MoveRequest.Direct(movedCount = 0)
        val resolver = context.contentResolver
        val uris = ids.map {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.value)
        }
        // Normalize: ensure trailing slash, strip any leading slash.
        val normalized = targetRelativePath.trimStart('/').let {
            if (it.endsWith('/')) it else "$it/"
        }

        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                runCatching {
                    val pi: PendingIntent =
                        MediaStore.createWriteRequest(resolver, uris)
                    // Stash target path in an instance map keyed by uri set so the
                    // post-consent applyMove can find it. For v1 we keep the model
                    // simple: the UI calls applyMoveAfterConsent() with the same
                    // target when the consent dialog returns OK.
                    pendingMoveTarget = normalized
                    pendingMoveUris = uris
                    MoveRequest.Consent(pi)
                }.getOrElse {
                    MoveRequest.Failure(it.message ?: "createWriteRequest failed")
                }
            }
            else -> {
                runCatching {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.RELATIVE_PATH, normalized)
                    }
                    var moved = 0
                    for (uri in uris) {
                        val n = resolver.update(uri, values, null, null)
                        if (n > 0) moved += n
                    }
                    MoveRequest.Direct(moved)
                }.getOrElse { MoveRequest.Failure(it.message ?: "move failed") }
            }
        }
    }

    /**
     * Phase H.4 — apply the pending RELATIVE_PATH update after the user
     * grants consent via the IntentSender launched from [moveToFolder]'s
     * `Consent` branch. Returns the moved count.
     */
    override suspend fun applyMoveAfterConsent(): Int {
        val target = pendingMoveTarget ?: return 0
        val uris = pendingMoveUris ?: return 0
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.RELATIVE_PATH, target)
        }
        var moved = 0
        for (uri in uris) {
            runCatching {
                val n = resolver.update(uri, values, null, null)
                if (n > 0) moved += n
            }
        }
        pendingMoveTarget = null
        pendingMoveUris = null
        return moved
    }

    @Volatile private var pendingMoveTarget: String? = null
    @Volatile private var pendingMoveUris: List<android.net.Uri>? = null

    // --- MediaChangeSource ---

    override fun observeChanges(): Flow<MediaChange> = callbackFlow {
        val resolver: ContentResolver = context.contentResolver
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                trySend(
                    MediaChange(
                        timestampMs = System.currentTimeMillis(),
                        source = MediaChange.ChangeSource.DEVICE_MEDIASTORE,
                    )
                )
            }
        }
        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    companion object {
        /**
         * Tonearmboy D.22.1 — minimum spacing between per-item progress
         * emissions on `_scanProgress`. 200 ms = ~5 Hz, the slowest
         * cadence that still looks live to a human watching the bar
         * advance. Below this, every photo triggers a Compose recomposition
         * of the caption / bar, eating real UI-thread time on a fast disk.
         */
        // 500 ms cadence — at 30k photos the 200 ms cadence was emitting
        // ~150 Compose recomposes during a single cold scan, enough to
        // contend with main-thread paints on a real phone. 500 ms still
        // reads as live (2 ticks/sec) and quarters the recomposition cost.
        // Combined with the no-op-skip + the IO-dispatcher wrap around the
        // whole scan body, this is what makes a 30k-photo first scan stop
        // freezing the UI on a real device.
        internal const val SCAN_PROGRESS_THROTTLE_MS = 500L
    }
}
