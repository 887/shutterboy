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
import com.eight87.shutterboy.domain.sort.PhotoSort

/**
 * Phase D.3.5 — engine wrapper that hosts the [PhotosZoomLevel] state +
 * the pinch-to-cycle gesture and routes both into [PhotosGrid]. The three
 * timeline surfaces (Photos / FolderDetail / SmartAlbumDetail) all
 * delegate to this; the per-screen file then is just the surface
 * scaffolding (TopAppBar / back arrow / title) plus the [PhotoStream]
 * that picks *which* photos.
 *
 * Sort hard-coded to [PhotoSort.Default] for now; Phase E adds the
 * per-tab + per-folder sort sheet.
 */
@Composable
fun GalleryTimelineFrame(
    stream: PhotoStream,
    onPhotoTap: (PhotoId, List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var accumulator by remember { mutableStateOf(ZoomAccumulator()) }
    val transformable = rememberTransformableState { zoomChange, _, _ ->
        accumulator = accumulator.apply(zoomChange)
    }
    PhotosGrid(
        stream = stream,
        sort = PhotoSort.Default,
        level = accumulator.level,
        onPhotoTap = onPhotoTap,
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformable),
    )
}
