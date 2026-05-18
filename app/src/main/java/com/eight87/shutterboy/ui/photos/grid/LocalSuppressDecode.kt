package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Aves-style "don't decode while moving" gate. While the grid is
 * scrolling (fling or thumb scrub), `true` here tells thumbnail
 * composables to render the placeholder instead of firing a Coil
 * request — the in-between scroll positions never accumulate stale
 * decodes in the queue. When the scroll settles back to `false`, the
 * currently visible tiles all fire their requests at once and the
 * 8-worker decoder pool drains them in parallel.
 *
 * Provided by [PhotosGrid]. Defaults to `false` so tests and previews
 * keep loading without explicit setup.
 */
val LocalSuppressDecode = staticCompositionLocalOf { false }
