package com.eight87.shutterboy.data.scan

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Phase B.3 — reads capture metadata from a photo's content URI and folds it
 * into a [ScannedPhoto]. Runs off-Main on [Dispatchers.IO]. Failures are
 * silent: a photo without parseable EXIF returns the input unchanged.
 *
 * Only the canonical capture metadata is extracted (lens model, focal length,
 * ISO, aperture, shutter speed, GPS). Orientation is intentionally NOT pulled
 * — Coil 3 handles orientation transparently when displaying thumbnails. The
 * raw EXIF blob is not stored; only the per-field cached values.
 */
class ExifEnricher(
    private val context: Context,
) {
    /** Enrich a single photo. Returns input unchanged on read failure. */
    suspend fun enrich(scanned: ScannedPhoto): ScannedPhoto = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(scanned.contentUri)?.use { stream ->
                val exif = ExifInterface(stream)
                val gps = exif.latLong
                scanned.copy(
                    exifLensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)
                        ?: exif.getAttribute(ExifInterface.TAG_MODEL),
                    exifFocalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, -1.0)
                        .takeIf { it > 0 }?.toFloat(),
                    exifIso = exif.getAttributeInt(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, -1)
                        .takeIf { it > 0 },
                    exifAperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, -1.0)
                        .takeIf { it > 0 }?.toFloat(),
                    exifShutterSpeedSec = parseShutterSpeed(exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)),
                    latitude = gps?.getOrNull(0),
                    longitude = gps?.getOrNull(1),
                )
            }
        }.getOrNull() ?: scanned
    }

    /**
     * Enrich a batch of photos in sequence. The repository's responsibility is
     * to call this only on photos that aren't already enriched in the cache —
     * EXIF reads are I/O-heavy.
     */
    suspend fun enrichBatch(scanned: List<ScannedPhoto>): List<ScannedPhoto> =
        scanned.map { enrich(it) }

    /**
     * EXIF stores exposure time as a fractional seconds value: "1/250", "0.0033",
     * etc. We normalise to a Float in seconds. Returns null if unparseable.
     */
    private fun parseShutterSpeed(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val slashIdx = raw.indexOf('/')
        return runCatching {
            if (slashIdx > 0) {
                val num = raw.substring(0, slashIdx).toFloat()
                val den = raw.substring(slashIdx + 1).toFloat()
                if (den != 0f) num / den else null
            } else {
                raw.toFloat()
            }
        }.getOrNull()
    }
}
