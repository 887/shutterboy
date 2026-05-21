package com.eight87.shutterboy.data.settings

import com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel
import kotlinx.coroutines.flow.Flow

/**
 * Display preferences for the Photos timeline. Covers two knobs:
 *  - which [PhotosZoomLevel] the timeline boots at (defaults to `Items`)
 *  - which [ThumbnailQuality] Coil requests at the grid tile (defaults
 *    to `Medium`)
 *
 * Both live on the same facet because they're settled together on the
 * Look-and-Feel sub-page (main.md I.2). Theme persistence stays on
 * [ThemePreferences] — different concern, different storage.
 */
interface DisplayPreferences {
    fun observeDefaultGridDensity(): Flow<PhotosZoomLevel>
    fun observeThumbnailQuality(): Flow<ThumbnailQuality>

    suspend fun setDefaultGridDensity(level: PhotosZoomLevel)
    suspend fun setThumbnailQuality(quality: ThumbnailQuality)
}

/**
 * Coil request-size for grid thumbnails. The values drive
 * `ImageRequest.Builder.size(...)` at the tile call-site; bigger →
 * crisper but slower + more memory.
 */
/**
 * Quality is now a **multiplier on the actual cell px** computed at
 * runtime, not a fixed pixel value. The grid measures its column
 * width from the device + zoom level and asks Coil for
 * `cellPx × multiplier`, so:
 *
 * - Medium (1.0×) is the auto-default — decode-at-display-size,
 *   crisp without waste, scales correctly from a budget MediaTek
 *   1080p phone (cells ~260 px) up to a tablet in landscape
 *   (cells ~500-700 px).
 * - Low (0.5×) — for slow devices where decode throughput beats
 *   crispness. Tiles will look slightly soft at native res.
 * - High (1.5×) — supersample for picky users on fast devices
 *   (some upscale during pinch-zoom etc).
 */
enum class ThumbnailQuality(val multiplier: Float) {
    Low(0.5f),
    Medium(1.0f),
    High(1.5f),
}
