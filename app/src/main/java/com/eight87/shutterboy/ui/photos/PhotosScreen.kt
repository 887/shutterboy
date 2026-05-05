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
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.photos.grid.PhotosGrid
import com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel
import com.eight87.shutterboy.ui.photos.grid.ZoomAccumulator

/**
 * Phase C.2 + C.3 — Photos tab body. Scaffold + dispatch only (R.D ceiling
 * ~200 LOC; this file is well under). Sub-pieces (grid, bands, cover tiles,
 * empty-state, zoom-level enum) live under `ui/photos/grid/`.
 *
 * Owns the [PhotosZoomLevel] state and the pinch-to-cycle gesture handler;
 * routes the level into [PhotosGrid] which dispatches per-cell rendering.
 * Year-scrubber (C.4) layers in next.
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
        onPhotoTap = { _ ->
            // Phase F wires the fullscreen viewer route. For now the tap is
            // a no-op so the grid renders cleanly without dragging in unwired
            // navigation.
        },
        modifier = modifier
            .fillMaxSize()
            .transformable(state = transformable),
    )
}
