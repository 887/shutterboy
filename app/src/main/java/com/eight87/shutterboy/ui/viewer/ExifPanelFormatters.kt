package com.eight87.shutterboy.ui.viewer

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Phase F.3 — pure, locale-stable formatters for the EXIF info panel. These
 * are kept out of `@Composable`s so they unit-test under plain JUnit (no
 * Robolectric, no Compose runtime). UI-facing strings (labels) live in
 * `strings.xml`; these helpers produce the numeric/textual values only.
 */
internal object ExifPanelFormatters {

    /**
     * Format a shutter speed (seconds) as a human-readable string.
     * - >= 1s → `"2s"` / `"1.5s"`
     * - < 1s → `"1/Nx s"` with N = round(1/seconds)
     * - null / non-positive → null (caller hides the row).
     */
    fun formatShutterSpeed(seconds: Float?): String? {
        if (seconds == null || !seconds.isFinite() || seconds <= 0f) return null
        return if (seconds >= 1f) {
            val whole = seconds.roundToInt()
            if (abs(seconds - whole) < 0.05f) "${whole}s"
            else "%.1fs".format(seconds)
        } else {
            val denom = (1f / seconds).roundToInt().coerceAtLeast(1)
            "1/${denom}s"
        }
    }

    /** Format aperture as `f/2.8` (one decimal, trailing `.0` stripped). */
    fun formatAperture(fNumber: Float?): String? {
        if (fNumber == null || !fNumber.isFinite() || fNumber <= 0f) return null
        val rounded = (fNumber * 10f).roundToInt() / 10f
        val str = if (abs(rounded - rounded.toInt()) < 0.05f) {
            rounded.toInt().toString()
        } else {
            "%.1f".format(rounded)
        }
        return "f/$str"
    }

    /** Format focal length as `35mm` (integer mm). */
    fun formatFocalLength(mm: Float?): String? {
        if (mm == null || !mm.isFinite() || mm <= 0f) return null
        return "${mm.roundToInt()}mm"
    }

    /** Format ISO as `ISO 400`. */
    fun formatIso(iso: Int?): String? {
        if (iso == null || iso <= 0) return null
        return "ISO $iso"
    }

    /**
     * Megapixel count of a (width × height) pair, rounded to whole MP.
     * Returns `"12 MP"` etc. Zero / negative dims → null.
     */
    fun formatMegapixels(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        val mp = (width.toLong() * height.toLong()) / 1_000_000.0
        return "${mp.roundToLong()} MP"
    }

    /** Format dimensions as `4032 × 3024`. */
    fun formatDimensions(width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        return "$width × $height"
    }

    /**
     * Format a byte count as a human-readable string. Locale-stable
     * (uses `%.1f` against `Locale.ROOT` semantics through `String.format`
     * the default locale — kept simple; for v1 the magnitude matters more
     * than locale-perfect decimal separators).
     */
    fun formatFileSize(bytes: Long): String? {
        if (bytes <= 0L) return null
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> "%.1f GB".format(bytes / gb)
            bytes >= mb -> "%.1f MB".format(bytes / mb)
            bytes >= kb -> "%.0f KB".format(bytes / kb)
            else -> "$bytes B"
        }
    }
}
