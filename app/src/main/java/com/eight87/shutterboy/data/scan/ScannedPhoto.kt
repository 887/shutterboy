package com.eight87.shutterboy.data.scan

import android.net.Uri

/**
 * Scan-only superset — what [MediaStoreScanner] / [com.eight87.shutterboy.data.saf.SafSourceManager]
 * produce, before mapping to the cache-faithful domain [com.eight87.shutterboy.domain.Photo].
 *
 * Fields that exist here but NOT on `Photo`:
 *  - [folderDisplayName] — the scanner already has it from MediaStore's
 *    `BUCKET_DISPLAY_NAME`; the domain `Photo` carries only [folderId] and the
 *    `Folder` table is the source of truth for folder names.
 *  - [source] — provenance of the scan (DEVICE_MEDIASTORE vs SAF_TREE). Used by
 *    the scanner to route the photo to the right `FolderEntity.sourceType`; not
 *    needed by UI consumers.
 *
 * Tonearmboy R.F.4 lesson: keeping the scan-only fields off the domain type
 * stops UI from accidentally reaching into them.
 */
data class ScannedPhoto(
    val id: Long,
    val contentUri: Uri,
    val displayName: String,
    val dateTakenMs: Long,
    val dateAddedMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val folderId: Long,
    val folderDisplayName: String,
    val source: ScanSource,
    val exifLensModel: String? = null,
    val exifFocalLength: Float? = null,
    val exifIso: Int? = null,
    val exifAperture: Float? = null,
    val exifShutterSpeedSec: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
) {
    enum class ScanSource { DEVICE_MEDIASTORE, SAF_TREE }
}
