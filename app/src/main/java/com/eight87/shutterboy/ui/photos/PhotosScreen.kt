package com.eight87.shutterboy.ui.photos

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.eight87.shutterboy.domain.sort.PhotoSort
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.photos.grid.PhotosGrid
import com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel
import com.eight87.shutterboy.ui.photos.grid.ZoomAccumulator

/**
 * Phase C.2 + C.3 + C.6 — Photos tab body. Scaffold + dispatch only (R.D
 * ceiling ~200 LOC; this file is well under). Sub-pieces (grid, bands,
 * cover tiles, scrubber, sticky-header banner, empty-state, zoom-level
 * enum) live under `ui/photos/grid/`.
 *
 * Owns the [PhotosZoomLevel] state and the pinch-to-cycle gesture handler;
 * routes the level into [PhotosGrid] which dispatches per-cell rendering.
 * Tap → push [PhotoViewer] with the tapped id + the current backing list
 * (Phase F replaces the placeholder body).
 */
@Composable
fun PhotosScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    var accumulator by remember { mutableStateOf(ZoomAccumulator()) }
    val transformable = rememberTransformableState { zoomChange, _, _ ->
        accumulator = accumulator.apply(zoomChange)
    }

    PhotosGrid(
        photoSource = scope.photoSource,
        sort = PhotoSort.Default,
        level = accumulator.level,
        onPhotoTap = { photoId, backingIds ->
            scope.backStack.push(PhotoViewer(photoId.value, backingIds))
        },
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformable),
    )
}
