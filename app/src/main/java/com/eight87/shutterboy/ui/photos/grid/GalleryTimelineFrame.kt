package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.ui.multiselect.SelectionState

/**
 * Engine wrapper that routes a [PhotoStream] + density [level] into
 * [PhotosGrid]. The three timeline surfaces (Photos / FolderDetail /
 * Favorites) all delegate to this; the per-screen file is just the
 * surface scaffolding (TopAppBar / sort / column-count button) plus
 * the [PhotoStream] that picks *which* photos.
 *
 * Pinch-to-cycle-density was dropped — the [ColumnCountButton] in the
 * top bar covers the same need, and the `Modifier.transformable` it
 * installed was eating horizontal pointer events before the
 * root-tab swipe modifier (applied by the caller) could see them.
 */
@Composable
fun GalleryTimelineFrame(
    stream: PhotoStream,
    onPhotoTap: (PhotoId, List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    emptyState: (@Composable (Modifier) -> Unit)? = null,
    selectionState: SelectionState = SelectionState.Idle,
    onPhotoLongPress: (PhotoId) -> Unit = {},
    @Suppress("UNUSED_PARAMETER") initialZoomLevel: PhotosZoomLevel = PhotosZoomLevel.Items,
    level: PhotosZoomLevel? = null,
    @Suppress("UNUSED_PARAMETER") onLevelChange: (PhotosZoomLevel) -> Unit = {},
    columns: Int? = null,
) {
    val activeLevel = level ?: PhotosZoomLevel.Items
    PhotosGrid(
        stream = stream,
        level = activeLevel,
        columns = columns ?: activeLevel.defaultColumns(),
        onPhotoTap = onPhotoTap,
        modifier = modifier.fillMaxSize(),
        emptyState = emptyState,
        selectionState = selectionState,
        onPhotoLongPress = onPhotoLongPress,
    )
}
