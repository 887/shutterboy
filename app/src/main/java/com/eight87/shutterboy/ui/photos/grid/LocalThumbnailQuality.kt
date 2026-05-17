package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.runtime.staticCompositionLocalOf
import com.eight87.shutterboy.data.settings.ThumbnailQuality

/**
 * Look-and-Feel I.2 — thumbnail-quality knob, threaded through the
 * composition so individual tiles ([PhotoThumbnail], [CoverTile]) read
 * the current setting without a constructor param everywhere. Provided
 * once at the app root from `DisplayPreferences.observeThumbnailQuality`.
 *
 * Default `Medium` so tests / previews compose without explicit setup.
 */
val LocalThumbnailQuality = staticCompositionLocalOf { ThumbnailQuality.Medium }
