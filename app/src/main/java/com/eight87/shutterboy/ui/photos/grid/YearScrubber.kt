package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val LINGER_MS = 1200L
private val ScrubberStripWidth = 56.dp

/**
 * Right-edge year scrubber — Aves / tonearmboy FastScrollbar shape.
 *
 * Every [YearMarker] renders as a pill on the strip; the pill matching
 * the current scroll position is emphasized (primary-tinted, bold).
 * Drag the strip to seek; tap a year directly to jump.
 *
 * Pills are laid out via a [Column] with [Arrangement.SpaceBetween], so
 * they distribute evenly along the strip regardless of how many photos
 * any one year contains (matches Aves's behaviour — equal-weight tap
 * targets per year). The strip fades in during scroll and lingers for
 * [LINGER_MS] after stop.
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

    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            lingering = true
        } else {
            delay(LINGER_MS)
            lingering = false
        }
    }
    val visible = scrolling || dragging || lingering

    // Current year = the marker whose timeline index covers the topmost
    // visible item. Derived from the grid state so the bubble emphasis
    // tracks scroll in real time.
    val currentYear by remember(markers) {
        derivedStateOf {
            val firstIdx = gridState.firstVisibleItemIndex
            markers.lastOrNull { it.timelineIndex <= firstIdx }?.year
                ?: markers.firstOrNull()?.year
        }
    }

    fun jumpToFraction(fraction: Float) {
        val marker = yearAtFraction(markers, fraction) ?: return
        coroutineScope.launch { gridState.scrollToItem(marker.timelineIndex) }
    }

    fun jumpToYear(year: Int) {
        val marker = markers.firstOrNull { it.year == year } ?: return
        coroutineScope.launch { gridState.scrollToItem(marker.timelineIndex) }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxHeight(),
    ) {
        Box(
            modifier = Modifier
                .width(ScrubberStripWidth)
                .fillMaxHeight()
                .padding(end = 4.dp)
                .pointerInput(markers, gridState) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            jumpToFraction((offset.y / size.height.toFloat()).coerceIn(0f, 1f))
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                        onVerticalDrag = { change, _ ->
                            jumpToFraction(
                                (change.position.y / size.height.toFloat()).coerceIn(0f, 1f),
                            )
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                markers.forEach { marker ->
                    YearPill(
                        text = marker.year.toString(),
                        emphasized = marker.year == currentYear,
                        modifier = Modifier.clickable { jumpToYear(marker.year) },
                    )
                }
            }
        }
    }
}

@Composable
private fun YearPill(
    text: String,
    emphasized: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (emphasized) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.88f)
    }
    val fg = if (emphasized) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
            color = fg,
        )
    }
}
