package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/**
 * Top-bar action that cycles the **column count** of the gallery grid
 * (1 → 2 → 3 → 4 → 5 → 1). Independent of the grouping level (see
 * [PhotosZoomLevel]) — grouping is picked via the FAB dropdown menu.
 *
 * Icon morphs with the count so it reads as a density hint at a
 * glance: fewer columns = list-like, more columns = dense grid.
 */
@Composable
fun ColumnCountButton(
    count: Int,
    onCountChange: (Int) -> Unit,
) {
    IconButton(onClick = { onCountChange(cycleColumns(count)) }) {
        Icon(
            imageVector = when (count) {
                1 -> Icons.AutoMirrored.Outlined.ViewList
                2 -> Icons.Outlined.ViewQuilt
                3 -> Icons.Outlined.ViewModule
                else -> Icons.Outlined.GridView
            },
            contentDescription = null,
        )
    }
}

private const val MIN_COLUMNS = 1
private const val MAX_COLUMNS = 5

private fun cycleColumns(count: Int): Int {
    val next = count + 1
    return if (next > MAX_COLUMNS) MIN_COLUMNS else next
}

/** Kept for backward compatibility with PhotosScreen's old FAB code. */
fun PhotosZoomLevel.cycleNext(): PhotosZoomLevel = when (this) {
    PhotosZoomLevel.Items -> PhotosZoomLevel.Days
    PhotosZoomLevel.Days -> PhotosZoomLevel.Months
    PhotosZoomLevel.Months -> PhotosZoomLevel.Years
    PhotosZoomLevel.Years -> PhotosZoomLevel.Items
}
