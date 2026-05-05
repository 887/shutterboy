package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LINGER_MS = 600L
private val ScrubberStripWidth = 28.dp

/**
 * Phase C.4 — right-edge year-timeline scrubber. The strip fades in while
 * the grid is scrolling and lingers for [LINGER_MS] after the scroll stops.
 * Drag along the strip — the y-fraction maps to a [YearMarker] via
 * [yearAtFraction], and the grid jumps via
 * [LazyGridState.scrollToItem]. Tap-to-jump is supported in addition to
 * drag.
 *
 * The drag bubble (year label) appears beside the user's finger only while
 * a drag is active, and disappears on release — keeps the chrome
 * unobtrusive when the grid is just being browsed.
 *
 * Caller-supplied [markers] is the output of [extractYearMarkers] and
 * should be remembered against the timeline by the parent so this
 * composable doesn't recompute on every recomposition.
 */
@Composable
internal fun YearScrubber(
    markers: List<YearMarker>,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
) {
    if (markers.isEmpty()) return
    val coroutineScope = rememberCoroutineScope()
    val scrolling = gridState.isScrollInProgress
    var dragging by remember { mutableStateOf(false) }
    var lingering by remember { mutableStateOf(false) }
    var bubbleYear by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            lingering = true
        } else {
            delay(LINGER_MS)
            lingering = false
        }
    }

    val visible = scrolling || dragging || lingering

    fun jumpToFraction(fraction: Float) {
        val marker = yearAtFraction(markers, fraction) ?: return
        bubbleYear = marker.year
        coroutineScope.launch { gridState.scrollToItem(marker.timelineIndex) }
    }

    Box(modifier = modifier.fillMaxHeight()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Box(
                modifier = Modifier
                    .width(ScrubberStripWidth)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                    .pointerInput(markers, gridState) {
                        detectVerticalDragGestures(
                            onDragStart = { offset ->
                                dragging = true
                                jumpToFraction((offset.y / size.height.toFloat()).coerceIn(0f, 1f))
                            },
                            onDragEnd = {
                                dragging = false
                                bubbleYear = null
                            },
                            onDragCancel = {
                                dragging = false
                                bubbleYear = null
                            },
                            onVerticalDrag = { change, _ ->
                                jumpToFraction(
                                    (change.position.y / size.height.toFloat()).coerceIn(0f, 1f),
                                )
                            },
                        )
                    }
                    .pointerInput(markers, gridState) {
                        detectTapGestures { offset ->
                            jumpToFraction((offset.y / size.height.toFloat()).coerceIn(0f, 1f))
                        }
                    },
            )
        }

        val year = bubbleYear
        if (dragging && year != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = ScrubberStripWidth + 16.dp)
                    .wrapContentWidth()
                    .wrapContentHeight(),
            ) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }
    }
}
