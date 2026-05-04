package com.eight87.shutterboy.data.scan

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase B.2 — scans `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` and produces
 * pre-EXIF [ScannedPhoto]s. Folder rows come from `BUCKET_ID` +
 * `BUCKET_DISPLAY_NAME` (deduplicated by id).
 *
 * EXIF enrichment is a separate concern handled by [ExifEnricher] in B.3 — this
 * class doesn't open files, just reads MediaStore columns.
 *
 * Idempotency lives at the repository layer: this scanner returns the full set
 * each call; [com.eight87.shutterboy.data.repo.LibraryScanner] diffs the result
 * against the existing cache and applies a delta via `PhotoDao.replaceWithDelta`.
 */
class MediaStoreScanner(
    private val context: Context,
) {
    /**
     * Returns every device-local image MediaStore knows about, no EXIF.
     * Caller is responsible for permission state (READ_MEDIA_IMAGES on API 33+,
     * READ_EXTERNAL_STORAGE on older). If the permission is missing, the
     * cursor query returns 0 rows; this surface is silent on missing perms.
     */
    suspend fun scanDeviceMediaStore(): ScanResult = withContext(Dispatchers.IO) {
        val photos = mutableListOf<ScannedPhoto>()
        val foldersById = LinkedHashMap<Long, ScannedFolder>()

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            PROJECTION,
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
                // MediaStore returns 0 when DATE_TAKEN is missing — fall back to DATE_ADDED * 1000.
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

        ScanResult(photos = photos, folders = foldersById.values.toList())
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
        val PROJECTION = arrayOf(
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
    }
}
