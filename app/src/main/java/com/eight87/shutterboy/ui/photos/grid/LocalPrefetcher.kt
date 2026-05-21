package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Grid-scoped handle to the active [ThumbnailPrefetcher]. Lets cells
 * submit themselves to the LIFO TOP on first composition — guarantees
 * that what's actually being drawn wins the prefetcher queue over any
 * scheduler-batched look-ahead from the previous sample.
 *
 * `null` outside of a grid that provides it; callers no-op in that
 * case (covers tests, previews, future surfaces).
 */
val LocalPrefetcher = staticCompositionLocalOf<ThumbnailPrefetcher?> { null }
