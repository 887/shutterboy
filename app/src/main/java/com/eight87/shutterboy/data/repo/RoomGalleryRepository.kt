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
import com.eight87.shutterboy.domain.SmartAlbumId
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
) : PhotoSource,
    FolderSource,
    SmartAlbumSource,
    PhotoSearch,
    LibraryScanner,
    FavoriteCommands,
    PhotoDeleter,
    PhotoMover,
    MediaChangeSource {

    private val _scanProgress = MutableStateFlow<ScanProgress>(ScanProgress.Idle)

    // --- PhotoSource ---

    override fun observePhotos(sort: PhotoSort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM photos ORDER BY ${sort.sqlOrderBy}",
        )
        return photoDao.observeAll(query).map { rows -> rows.map { it.toDomain() } }
    }

    override fun observePhotosInFolder(folderId: FolderId, sort: PhotoSort): Flow<List<Photo>> {
        val query = SimpleSQLiteQuery(
            "SELECT * FROM photos WHERE folder_id = ? ORDER BY ${sort.sqlOrderBy}",
            arrayOf<Any>(folderId.value),
        )
        return photoDao.observeAll(query).map { rows -> rows.map { it.toDomain() } }
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

    // --- SmartAlbumSource (B.6 sealed dispatch) ---

    override fun observeSmartAlbum(id: SmartAlbumId): Flow<List<Photo>> =
        when (id) {
            SmartAlbumId.Camera ->
                photoDao.observeInBucketByName("Camera")
                    .map { rows -> rows.map { it.toDomain() } }
            SmartAlbumId.Screenshots ->
                photoDao.observeInBucketByName("Screenshots")
                    .map { rows -> rows.map { it.toDomain() } }
            SmartAlbumId.Favorites ->
                favoriteDao.observeFavoritePhotos()
                    .map { rows -> rows.map { it.toDomain() } }
            SmartAlbumId.Recents ->
                photoDao.observeRecents(System.currentTimeMillis() - SmartAlbumId.Recents.WINDOW_MS)
                    .map { rows -> rows.map { it.toDomain() } }
        }

    override fun observeSmartAlbumCovers(): Flow<Map<SmartAlbumId, Photo?>> {
        val flows: List<Flow<Pair<SmartAlbumId, Photo?>>> =
            SmartAlbumId.defaultOrder.map { id ->
                observeSmartAlbum(id).map { id to it.firstOrNull() }
            }
        return combine(flows) { pairs -> pairs.toMap() }
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

    override fun recentSearches(): Flow<List<String>> = flowOf(emptyList())
    override suspend fun recordSearch(query: String) { /* Phase G persists */ }

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

    private suspend fun executeScan(): LibrarySnapshot {
        // 1. Scan device media-store + SAF trees
        val device = mediaStoreScanner.scanDeviceMediaStore()
        val safUris = scanConfig.safSourceUris.first()
        val saf = safSourceManager.scanSafTrees(safUris)

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
            if (processed == total || now - lastEmitMs >= SCAN_PROGRESS_THROTTLE_MS) {
                lastEmitMs = now
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
        return LibrarySnapshot(
            photos = photoEntities.map { it.toDomain() },
            folders = folderEntities.map { it.toDomain() },
            deltaCount = deltaCount,
        )
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
        internal const val SCAN_PROGRESS_THROTTLE_MS = 200L
    }
}
