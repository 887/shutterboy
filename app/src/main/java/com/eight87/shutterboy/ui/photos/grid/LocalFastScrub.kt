package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Aves-style fast-scrub gate. While the user is dragging the scrollbar
 * thumb (or actively scrubbing without pausing) this is `true` and
 * thumbnail composables render the placeholder only — no Coil decode,
 * no disk read. Flipping back to `false` (drag released, or paused
 * >500ms on a position) lets the visible tiles load normally on the
 * next composition.
 *
 * Owned by [PhotosGrid]; read by [PhotoThumbnail] / [CoverTile].
 */
val LocalFastScrub = staticCompositionLocalOf { false }
