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
    /**
     * Sample rate (ms) for the grid's prefetch scheduler. Lower =
     * faster reaction to scroll changes but more thrash; higher =
     * workers get more drain time but slower window updates.
     * Clamped to [PREFETCH_SAMPLE_MIN_MS]..[PREFETCH_SAMPLE_MAX_MS].
     */
    fun observePrefetchSampleRateMs(): Flow<Int>
    /**
     * Sample rate used when the OS reports power-save mode active.
     * Slower default (200 ms) so the scheduler does less work — fewer
     * keep-set rebuilds, fewer cancel-resubmit cycles — when the
     * device is trying to conserve.
     */
    fun observePrefetchSampleRateBatterySaverMs(): Flow<Int>

    suspend fun setDefaultGridDensity(level: PhotosZoomLevel)
    suspend fun setThumbnailQuality(quality: ThumbnailQuality)
    suspend fun setPrefetchSampleRateMs(ms: Int)
    suspend fun setPrefetchSampleRateBatterySaverMs(ms: Int)

    companion object {
        const val PREFETCH_SAMPLE_DEFAULT_MS: Int = 100
        const val PREFETCH_SAMPLE_BATTERY_SAVER_DEFAULT_MS: Int = 200
        const val PREFETCH_SAMPLE_MIN_MS: Int = 10
        const val PREFETCH_SAMPLE_MAX_MS: Int = 1000
    }
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
