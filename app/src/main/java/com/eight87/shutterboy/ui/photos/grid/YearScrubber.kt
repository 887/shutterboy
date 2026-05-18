package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private const val LINGER_MS = 1200L
private val ScrubberStripWidth = 180.dp
private val ThumbVisibleWidth = 22.dp
private val ThumbTouchWidth = 32.dp
private val PillRightInset = 36.dp

/**
 * Right-edge floating labels — Aves shape, no draggable scrollbar.
 *
 * Two kinds of pills render over the grid (touches in the strip area
 * fall through to the photos beneath; only the pills themselves are
 * clickable):
 *
 *  - **Year pills** — one per [YearMarker], pinned at the marker's
 *    density-weighted fraction of the strip so they crowd where the
 *    photos crowd. Tap to jump to that year.
 *  - **Current-scroll pill** — tracks the current `firstVisibleItem`
 *    fraction, labelled with the active month/year ([stickyHeaderLabel]).
 *    Same pill style/size as the year pills; sits at the y of the
 *    current scroll position the way Aves shows the scrubber bubble.
 *
 * Pills fade in while scrolling and linger [LINGER_MS] after stop.
 */
@Composable
internal fun YearScrubber(
    markers: List<YearMarker>,
    timeline: List<TimelineDisplayItem>,
    level: PhotosZoomLevel,
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    onScrubbingChange: (Boolean) -> Unit = {},
) {
    val totalTimelineSize = timeline.size
    if (markers.isEmpty() || totalTimelineSize <= 0) return
    val coroutineScope = rememberCoroutineScope()
    val scrolling = gridState.isScrollInProgress
    var lingering by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(scrolling, dragging) {
        if (scrolling || dragging) {
            lingering = true
        } else {
            delay(LINGER_MS)
            lingering = false
        }
    }
    val visible = scrolling || lingering || dragging

    val currentYear by remember(markers) {
        derivedStateOf {
            val firstIdx = gridState.firstVisibleItemIndex
            markers.lastOrNull { it.timelineIndex <= firstIdx }?.year
                ?: markers.firstOrNull()?.year
        }
    }

    val locale = LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    val currentLabel by remember(timeline, level, locale) {
        derivedStateOf {
            stickyHeaderLabel(timeline, gridState.firstVisibleItemIndex, level, locale)
        }
    }
    val scrollFraction by remember(timeline) {
        derivedStateOf {
            val total = totalTimelineSize.toFloat()
            if (total <= 0f) 0f
            else (gridState.firstVisibleItemIndex.toFloat() / total).coerceIn(0f, 1f)
        }
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
        BoxWithConstraints(
            modifier = Modifier
                .width(ScrubberStripWidth)
                .fillMaxHeight()
                .padding(end = 4.dp),
        ) {
            val trackHeightDp = maxHeight
            // Aves-style thin scrollbar: a vertical thumb on the right edge
            // showing viewport-to-content ratio. Non-interactive — visual
            // only; the year pills are still the tap targets.
            val viewportFraction by remember(totalTimelineSize) {
                derivedStateOf {
                    val info = gridState.layoutInfo
                    val visible = info.visibleItemsInfo.size.toFloat()
                    if (totalTimelineSize <= 0 || visible <= 0f) 0f
                    else (visible / totalTimelineSize).coerceIn(0.04f, 1f)
                }
            }
            val thumbHeightDp = trackHeightDp * viewportFraction
            val maxThumbOffset = trackHeightDp - thumbHeightDp
            // Aves-style chunky thumb with up/down chevrons; wider invisible
            // touch target around it for easy grabbing.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = maxThumbOffset * scrollFraction)
                    .width(ThumbTouchWidth)
                    .height(thumbHeightDp.coerceAtLeast(48.dp))
                    .pointerInput(totalTimelineSize, maxThumbOffset) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragging = true
                                onScrubbingChange(true)
                            },
                            onDragEnd = {
                                dragging = false
                                onScrubbingChange(false)
                            },
                            onDragCancel = {
                                dragging = false
                                onScrubbingChange(false)
                            },
                        ) { change, dragAmount ->
                            change.consume()
                            val maxOffsetPx = maxThumbOffset.toPx()
                            if (maxOffsetPx <= 0f) return@detectVerticalDragGestures
                            val currentFraction =
                                gridState.firstVisibleItemIndex.toFloat() / totalTimelineSize
                            val newFraction =
                                (currentFraction + dragAmount / maxOffsetPx).coerceIn(0f, 1f)
                            val target = (newFraction * totalTimelineSize).toInt()
                                .coerceIn(0, totalTimelineSize - 1)
                            coroutineScope.launch { gridState.scrollToItem(target) }
                        }
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Column(
                    modifier = Modifier
                        .width(ThumbVisibleWidth)
                        .height(thumbHeightDp.coerceAtLeast(48.dp))
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                        )
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            markers.forEach { marker ->
                // Density-weighted Y: where this year's first photo sits
                // relative to the full timeline. Pills sit to the LEFT of
                // the thumb so a thumb-grab doesn't hide them under the
                // finger.
                val fraction = (marker.timelineIndex.toFloat() / totalTimelineSize)
                    .coerceIn(0f, 1f)
                YearPill(
                    text = marker.year.toString(),
                    emphasized = false,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = -PillRightInset, y = trackHeightDp * fraction)
                        .clickable { jumpToYear(marker.year) },
                )
            }
            val floatingLabel = currentLabel
            if (!floatingLabel.isNullOrBlank() &&
                floatingLabel != currentYear?.toString()
            ) {
                YearPill(
                    text = floatingLabel,
                    emphasized = true,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = -PillRightInset, y = trackHeightDp * scrollFraction),
                )
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
