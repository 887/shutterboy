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
 * Top-bar action button that cycles the gallery's column count.
 * Icon morphs with the current density: 4 columns shows a dense grid,
 * 1 column shows a single list row. Tap cycles
 * Items → Days → Months → Years → Items.
 *
 * Mirrors tonearmboy's same-shape button so both apps feel like
 * siblings.
 */
@Composable
fun ColumnCountButton(
    level: PhotosZoomLevel,
    onLevelChange: (PhotosZoomLevel) -> Unit,
) {
    IconButton(onClick = { onLevelChange(level.cycleNext()) }) {
        Icon(
            imageVector = when (level) {
                PhotosZoomLevel.Items -> Icons.Outlined.GridView
                PhotosZoomLevel.Days -> Icons.Outlined.ViewModule
                PhotosZoomLevel.Months -> Icons.Outlined.ViewQuilt
                PhotosZoomLevel.Years -> Icons.AutoMirrored.Outlined.ViewList
            },
            contentDescription = null,
        )
    }
}

private fun PhotosZoomLevel.cycleNext(): PhotosZoomLevel = when (this) {
    PhotosZoomLevel.Items -> PhotosZoomLevel.Days
    PhotosZoomLevel.Days -> PhotosZoomLevel.Months
    PhotosZoomLevel.Months -> PhotosZoomLevel.Years
    PhotosZoomLevel.Years -> PhotosZoomLevel.Items
}
