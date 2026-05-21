package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Grid-scoped target px for thumbnail decodes. Provided by the
 * outer grid composable so per-cell composables ([PhotoThumbnail],
 * [CoverTile]) read one number without each one re-deriving from
 * screen width + density. Defaults to 256 for callers outside any
 * grid (tests, previews); production paths always provide via
 * [rememberGridTargetPx].
 */
val LocalGridTargetPx = staticCompositionLocalOf { 256 }

/**
 * Compute the decode target-px for a grid tile given its column count.
 * Auto-derives from device screen width, divided by columns, minus
 * `LazyVerticalGrid` padding + inter-cell gaps — then scaled by the
 * user's [com.eight87.shutterboy.data.settings.ThumbnailQuality]
 * multiplier (Low 0.5× / Medium 1.0× / High 1.5×).
 *
 * Result is the same regardless of whether you're on a 1080p phone,
 * a 1440p phone, or a 2048-px tablet in landscape — each device asks
 * Coil for exactly the number of pixels its cells actually render.
 */
@Composable
fun rememberGridTargetPx(columns: Int): Int {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val multiplier = LocalThumbnailQuality.current.multiplier
    return remember(columns, configuration.screenWidthDp, multiplier) {
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        // 4 dp start + 4 dp end content padding on the LazyVerticalGrid.
        val sidePaddingPx = with(density) { 8.dp.toPx() }
        // 2 dp horizontal arrangement gap between cells.
        val gapsPx = with(density) { ((columns - 1) * 2).dp.toPx() }
        val cellPx = ((screenWidthPx - sidePaddingPx - gapsPx) / columns)
            .coerceAtLeast(64f)
        (cellPx * multiplier).toInt().coerceAtLeast(48)
    }
}
