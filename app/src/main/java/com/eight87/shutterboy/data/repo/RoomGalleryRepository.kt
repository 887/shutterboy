package com.eight87.shutterboy.data.repo

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.ContentUris
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import com.eight87.shutterboy.data.scan.MediaStoreScanner
import com.eight87.shutterboy.data.scan.ScannedPhoto
import com.eight87.shutterboy.data.settings.ScanConfigSource
import com.eight87.shutterboy.domain.DeleteRequest
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.LibrarySnapshot
import com.eight87.shutterboy.domain.MediaChange
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
) : PhotoSource,
    FolderSource,
    SmartAlbumSource,
    PhotoSearch,
    LibraryScanner,
    FavoriteCommands,
    PhotoDeleter,
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

    // --- FolderSource ---

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeFolder(id: FolderId): Flow<Folder?> =
        folderDao.observeById(id.value).map { it?.toDomain() }

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
        return runCatching {
            // 1. Scan device media-store + SAF trees
            val device = mediaStoreScanner.scanDeviceMediaStore()
            val safUris = scanConfig.safSourceUris.first()
            val saf = safSourceManager.scanSafTrees(safUris)

            val combinedPhotos: List<ScannedPhoto> = device.photos + saf.photos
            val combinedFolders = device.folders + saf.folders

            // 2. Diff vs cache: only new photos need EXIF; existing rows keep cached EXIF.
            val cachedIds = photoDao.allIds().toSet()
            val toEnrich = combinedPhotos.filter { it.id !in cachedIds }
            val enriched = exifEnricher.enrichBatch(toEnrich)
            val byId = combinedPhotos.associateBy { it.id }.toMutableMap()
            for (e in enriched) byId[e.id] = e

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

            // 4. Apply delta
            val seenIds = photoEntities.map(PhotoEntity::id).toSet()
            val toDeletePhotos = (cachedIds - seenIds).toList()
            photoDao.replaceWithDelta(toUpsert = photoEntities, toDelete = toDeletePhotos)

            val cachedFolderIds = folderDao.allIds().toSet()
            val seenFolderIds = folderEntities.map(FolderEntity::id).toSet()
            val toDeleteFolders = cachedFolderIds - seenFolderIds
            folderDao.upsertAll(folderEntities)
            for (id in toDeleteFolders) folderDao.deleteById(id)

            val deltaCount = photoEntities.size - cachedIds.intersect(seenIds).size + toDeletePhotos.size
            val snap = LibrarySnapshot(
                photos = photoEntities.map { it.toDomain() },
                folders = folderEntities.map { it.toDomain() },
                deltaCount = deltaCount,
            )
            _scanProgress.value = ScanProgress.Done(deltaCount)
            snap
        }.getOrElse { e ->
            _scanProgress.value = ScanProgress.Failed(e.message ?: e::class.simpleName ?: "scan failed")
            LibrarySnapshot(photos = emptyList(), folders = emptyList(), deltaCount = 0)
        }
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
}
