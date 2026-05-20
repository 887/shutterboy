package com.eight87.shutterboy.ui.nav

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Root destinations the user can swipe between, in the order they
 * appear in the top bar. Photos ← → Favorites ← → Collections. The
 * Settings destination is intentionally excluded — it's an
 * always-reachable-via-icon hub, not a content tab.
 *
 * No wraparound: swiping right at the leftmost destination is a no-op,
 * and so is swiping left at the rightmost.
 */
val rootSwipeOrder: List<Destination> = listOf(Photos, Favorites, Collections)

/**
 * Apply to the outer container of a root screen to enable horizontal
 * fling-to-switch-tab. [detectHorizontalDragGestures] only claims the
 * gesture after horizontal touch-slop has been crossed, so a
 * predominantly-vertical drag still bubbles to the grid's scroll
 * detector untouched.
 */
@Composable
fun Modifier.rootSwipe(
    current: Destination,
    onSwitchTab: (Destination) -> Unit,
): Modifier {
    val idx = rootSwipeOrder.indexOf(current)
    if (idx < 0) return this
    val left = rootSwipeOrder.getOrNull(idx - 1)
    val right = rootSwipeOrder.getOrNull(idx + 1)
    if (left == null && right == null) return this
    val configuration = LocalConfiguration.current
    val threshold = with(LocalDensity.current) {
        // ~quarter of the screen width is enough commitment to read as
        // intentional. Below that we treat the gesture as scroll-noise.
        configuration.screenWidthDp.dp.toPx() * 0.25f
    }
    val accumulator = remember(current) { mutableFloatStateOf(0f) }
    return this.pointerInput(current, left, right, threshold) {
        detectHorizontalDragGestures(
            onDragStart = { accumulator.floatValue = 0f },
            onDragCancel = { accumulator.floatValue = 0f },
            onDragEnd = {
                val travel = accumulator.floatValue
                accumulator.floatValue = 0f
                when {
                    travel <= -threshold && right != null -> onSwitchTab(right)
                    travel >= threshold && left != null -> onSwitchTab(left)
                }
            },
        ) { change, dragAmount ->
            accumulator.floatValue += dragAmount
            change.consume()
        }
    }
}
