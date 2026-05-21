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
 * Always-0.5× variant used as the **first-pass** decode in the
 * progressive-quality strategy: tiles paint the cheap low decode
 * immediately so scroll is fast, then upgrade to [LocalGridTargetPx]
 * after a short dwell on screen. When the user has picked
 * `ThumbnailQuality.Low`, this equals [LocalGridTargetPx] and the
 * cell skips the upgrade.
 */
val LocalGridLowPx = staticCompositionLocalOf { 128 }

/**
 * Compute the decode target-px for a grid tile given its column count
 * and a [multiplier]. The base value auto-derives from device screen
 * width / columns / gaps, so a 1080p phone with 4 columns lands at
 * ~260 px, a tablet portrait at ~360 px, a tablet landscape at
 * ~600 px — each device asks Coil for exactly what its cells
 * actually render.
 *
 * Default [multiplier] is the user's
 * [com.eight87.shutterboy.data.settings.ThumbnailQuality] setting
 * (Low 0.5× / Medium 1.0× / High 1.5×). Pass an explicit value for
 * the progressive-decode low tier (always 0.5×).
 */
@Composable
fun rememberGridTargetPx(
    columns: Int,
    multiplier: Float = LocalThumbnailQuality.current.multiplier,
): Int {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    return remember(columns, configuration.screenWidthDp, multiplier) {
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val sidePaddingPx = with(density) { 8.dp.toPx() }
        val gapsPx = with(density) { ((columns - 1) * 2).dp.toPx() }
        val cellPx = ((screenWidthPx - sidePaddingPx - gapsPx) / columns)
            .coerceAtLeast(64f)
        (cellPx * multiplier).toInt().coerceAtLeast(48)
    }
}
