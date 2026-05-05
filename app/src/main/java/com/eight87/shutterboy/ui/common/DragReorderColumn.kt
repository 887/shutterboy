package com.eight87.shutterboy.ui.common

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Phase E.4 — long-press to lift, drag to reorder, release to drop.
 * Adapted from tonearmboy's `DragReorderColumn` (D.18.4 / D.21.3 / D.27);
 * shutterboy reuses the same shape inside the smart-album + folder
 * reorder dialogs.
 *
 * Items render in a `Column` (no lazy virtualization — bounded row
 * counts only). The drag handle is the [Modifier] passed into
 * [rowContent]; rows attach it to whatever leading or trailing icon
 * acts as the lift target. Long-press on that handle flips the row
 * into dragging mode and the drag-Y translation re-orders the list in
 * place; release commits via [onReordered].
 *
 * Vertical-only drag, fixed row height. Computes the target index by
 * snapping the drag delta to whole row heights. Items animate to their
 * new position via offset modifiers; we don't use a lazy list here
 * because the dialog's row count is bounded (4 smart albums or a
 * realistic upper bound of a few dozen folders).
 *
 * The pattern is the same one documented in `android-skills` under
 * "compose-drag-and-drop" — the third-party library
 * `sh.calvin.reorderable` is functionally equivalent but adds a
 * dependency for ~150 lines of code we can write inline.
 */
@Composable
internal fun <T : Any> DragReorderColumn(
    items: List<T>,
    itemKey: (T) -> String,
    rowHeightDp: Int,
    onReordered: (List<T>) -> Unit,
    onDragStateChange: ((Boolean) -> Unit)? = null,
    rowContent: @Composable (T, Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val rowPx = with(density) { rowHeightDp.dp.toPx() }
    var working by remember(items) { mutableStateOf(items) }
    LaunchedEffect(items) { working = items }

    var draggingIndex by remember { mutableStateOf(-1) }
    var dragOffsetPx by remember { mutableStateOf(0f) }

    Column(modifier = Modifier.fillMaxWidth()) {
        working.forEachIndexed { index, item ->
            val itemId = itemKey(item)
            // `key(itemId)` ties composable identity to the item, not the
            // position — the running pointer-input coroutine survives a
            // reorder-during-drag, so the gesture doesn't die at the swap.
            key(itemId) {
                val isDragging = index == draggingIndex
                val translateY = if (isDragging) dragOffsetPx else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeightDp.dp)
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset { IntOffset(0, translateY.roundToInt()) },
                ) {
                    val handleModifier = Modifier.pointerInput(itemId) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                val currentIdx = working.indexOf(item)
                                if (currentIdx >= 0) {
                                    draggingIndex = currentIdx
                                    dragOffsetPx = 0f
                                    onDragStateChange?.invoke(true)
                                }
                            },
                            onDrag = { _, drag ->
                                dragOffsetPx += drag.y
                                val current = draggingIndex
                                if (current >= 0) {
                                    val targetDelta = (dragOffsetPx / rowPx).roundToInt()
                                    val target = (current + targetDelta).coerceIn(0, working.size - 1)
                                    if (target != current) {
                                        val swapped = working.toMutableList()
                                        val moved = swapped.removeAt(current)
                                        swapped.add(target, moved)
                                        working = swapped
                                        draggingIndex = target
                                        dragOffsetPx -= (target - current) * rowPx
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingIndex = -1
                                dragOffsetPx = 0f
                                onDragStateChange?.invoke(false)
                                if (working != items) onReordered(working)
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                dragOffsetPx = 0f
                                onDragStateChange?.invoke(false)
                                working = items
                            },
                        )
                    }
                    rowContent(item, handleModifier)
                }
            }
        }
    }
}
