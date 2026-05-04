package com.eight87.shutterboy.domain

import android.net.Uri

/**
 * Cache-faithful read shape — what UI consumes from the repository. The scan-only
 * superset (raw EXIF blob, multi-value tag splits, source provenance) lives in
 * [com.eight87.shutterboy.data.scan.ScannedPhoto] and never crosses the data-layer
 * boundary. Domain-vs-scan split is locked per refactor-solid R.F.4.
 */
data class Photo(
    val id: PhotoId,
    val contentUri: Uri,
    val displayName: String,
    val dateTakenMs: Long,
    val dateAddedMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val mimeType: String,
    val folderId: FolderId,
    val exifLensModel: String? = null,
    val exifFocalLength: Float? = null,
    val exifIso: Int? = null,
    val exifAperture: Float? = null,
    val exifShutterSpeedSec: Float? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@JvmInline
value class PhotoId(val value: Long)
