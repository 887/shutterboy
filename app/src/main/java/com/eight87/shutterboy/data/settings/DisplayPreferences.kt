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
enum class ThumbnailQuality(val targetPx: Int) {
    Low(192),
    Medium(384),
    High(768),
}
