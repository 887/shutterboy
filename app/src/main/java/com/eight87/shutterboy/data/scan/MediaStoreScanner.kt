package com.eight87.shutterboy.data.scan

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase B.2 — scans both `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`
 * and `MediaStore.Video.Media.EXTERNAL_CONTENT_URI`, producing pre-EXIF
 * [ScannedPhoto]s. The type is named `ScannedPhoto` for historical
 * reasons — it's really `ScannedMedia` and carries both images and
 * videos. The video MIME prefix distinction is carried on [ScannedPhoto.mimeType].
 *
 * Folder rows come from `BUCKET_ID` + `BUCKET_DISPLAY_NAME` (deduplicated
 * by id; image + video rows can share the same bucket id).
 *
 * EXIF enrichment is a separate concern handled by [ExifEnricher] in B.3 — this
 * class doesn't open files, just reads MediaStore columns. Videos carry no
 * EXIF; the enricher skips video MIME types.
 *
 * Idempotency lives at the repository layer: this scanner returns the full set
 * each call; [com.eight87.shutterboy.data.repo.LibraryScanner] diffs the result
 * against the existing cache and applies a delta via `PhotoDao.replaceWithDelta`.
 *
 * Both volumes — images and video — share the same
 * `MediaStore.VOLUME_EXTERNAL_PRIMARY` generation token, so
 * [MediaStoreGenerationSource] covers both at the gate layer.
 */
open class MediaStoreScanner(
    private val context: Context,
) {
    /**
     * Returns every device-local image AND video MediaStore knows about,
     * no EXIF. Caller is responsible for permission state (READ_MEDIA_IMAGES
     * + READ_MEDIA_VIDEO on API 33+, READ_EXTERNAL_STORAGE on older). If a
     * permission is missing, that cursor query returns 0 rows; this surface
     * is silent on missing perms.
     */
    open suspend fun scanDeviceMediaStore(): ScanResult = withContext(Dispatchers.IO) {
        val photos = mutableListOf<ScannedPhoto>()
        val foldersById = LinkedHashMap<Long, ScannedFolder>()

        queryImages(photos, foldersById)
        queryVideos(photos, foldersById)

        ScanResult(photos = photos, folders = foldersById.values.toList())
    }

    private fun queryImages(
        photos: MutableList<ScannedPhoto>,
        foldersById: LinkedHashMap<Long, ScannedFolder>,
    ) {
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            IMAGE_PROJECTION,
            /* selection = */ null,
            /* selectionArgs = */ null,
            /* sortOrder = */ "${MediaStore.Images.Media.DATE_TAKEN} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val bucketId = cursor.getLong(bucketIdCol)
                val bucketName = cursor.getString(bucketNameCol) ?: "Unknown"
                val takenRaw = cursor.getLong(takenCol)
                val taken = if (takenRaw > 0) takenRaw else cursor.getLong(addedCol) * 1000

                photos += ScannedPhoto(
                    id = id,
                    contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                    displayName = cursor.getString(nameCol) ?: "image_$id",
                    dateTakenMs = taken,
                    dateAddedMs = cursor.getLong(addedCol) * 1000,
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    mimeType = cursor.getString(mimeCol) ?: "image/*",
                    folderId = bucketId,
                    folderDisplayName = bucketName,
                    source = ScannedPhoto.ScanSource.DEVICE_MEDIASTORE,
                )

                foldersById.getOrPut(bucketId) {
                    ScannedFolder(id = bucketId, displayName = bucketName, source = ScannedPhoto.ScanSource.DEVICE_MEDIASTORE)
                }
            }
        }
    }

    private fun queryVideos(
        photos: MutableList<ScannedPhoto>,
        foldersById: LinkedHashMap<Long, ScannedFolder>,
    ) {
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            VIDEO_PROJECTION,
            /* selection = */ null,
            /* selectionArgs = */ null,
            /* sortOrder = */ "${MediaStore.Video.Media.DATE_TAKEN} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
            val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val bucketId = cursor.getLong(bucketIdCol)
                val bucketName = cursor.getString(bucketNameCol) ?: "Unknown"
                val takenRaw = cursor.getLong(takenCol)
                val taken = if (takenRaw > 0) takenRaw else cursor.getLong(addedCol) * 1000

                photos += ScannedPhoto(
                    id = id,
                    contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                    displayName = cursor.getString(nameCol) ?: "video_$id",
                    dateTakenMs = taken,
                    dateAddedMs = cursor.getLong(addedCol) * 1000,
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    sizeBytes = cursor.getLong(sizeCol),
                    mimeType = cursor.getString(mimeCol) ?: "video/*",
                    folderId = bucketId,
                    folderDisplayName = bucketName,
                    source = ScannedPhoto.ScanSource.DEVICE_MEDIASTORE,
                )

                foldersById.getOrPut(bucketId) {
                    ScannedFolder(id = bucketId, displayName = bucketName, source = ScannedPhoto.ScanSource.DEVICE_MEDIASTORE)
                }
            }
        }
    }

    /**
     * What the scanner returns. Photos carry no EXIF yet; folders are
     * deduplicated by bucket id.
     */
    data class ScanResult(
        val photos: List<ScannedPhoto>,
        val folders: List<ScannedFolder>,
    )

    /** Per-folder roll-up the scanner emits. The repo layer turns these into
     *  `FolderEntity`s and computes counts + cover-photo ids from [photos]. */
    data class ScannedFolder(
        val id: Long,
        val displayName: String,
        val source: ScannedPhoto.ScanSource,
        val safTreeUri: Uri? = null,
    )

    private companion object {
        val IMAGE_PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        )

        val VIDEO_PROJECTION = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
        )
    }
}
