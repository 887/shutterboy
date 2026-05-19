package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.ui.multiselect.SelectionState

/**
 * Phase D.3.5 — engine wrapper that hosts the [PhotosZoomLevel] state +
 * the pinch-to-cycle gesture and routes both into [PhotosGrid]. The three
 * timeline surfaces (Photos / FolderDetail / SmartAlbumDetail) all
 * delegate to this; the per-screen file then is just the surface
 * scaffolding (TopAppBar / back arrow / title / sort overflow) plus the
 * [PhotoStream] that picks *which* photos. The sort choice is baked into
 * the [PhotoStream] closure (Phase E.2) — this engine doesn't know about
 * sort.
 */
@Composable
fun GalleryTimelineFrame(
    stream: PhotoStream,
    onPhotoTap: (PhotoId, List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    emptyState: (@Composable (Modifier) -> Unit)? = null,
    selectionState: SelectionState = SelectionState.Idle,
    onPhotoLongPress: (PhotoId) -> Unit = {},
    initialZoomLevel: PhotosZoomLevel = PhotosZoomLevel.Items,
    level: PhotosZoomLevel? = null,
    onLevelChange: (PhotosZoomLevel) -> Unit = {},
) {
    // Pinch state stays internal; if a parent supplies `level` directly
    // (e.g. via the top-bar column-cycle button) the parent wins and the
    // accumulator is reset on each external change.
    var accumulator by remember(initialZoomLevel, level) {
        mutableStateOf(ZoomAccumulator(level = level ?: initialZoomLevel))
    }
    val transformable = rememberTransformableState { zoomChange, _, _ ->
        val next = accumulator.apply(zoomChange)
        accumulator = next
        if (next.level != accumulator.level) onLevelChange(next.level)
    }
    val currentLevel = level ?: accumulator.level
    PhotosGrid(
        stream = stream,
        level = currentLevel,
        onPhotoTap = onPhotoTap,
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformable),
        emptyState = emptyState,
        selectionState = selectionState,
        onPhotoLongPress = onPhotoLongPress,
    )
}
