package com.eight87.shutterboy.ui.photos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eight87.shutterboy.domain.sort.PhotoSort
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.photos.grid.PhotosGrid

/**
 * Phase C.2 — Photos tab body. Scaffold + dispatch only (R.D ceiling
 * ~200 LOC). Sub-pieces (grid, band, scrubber, empty-state) live under
 * `ui/photos/grid/` and `ui/photos/multiselect/`.
 *
 * Density zoom (C.3) and year-scrubber (C.4) layer on top in their own
 * commits; this composable currently dispatches to the default-density
 * Items grid only.
 */
@Composable
fun PhotosScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    PhotosGrid(
        photoSource = scope.photoSource,
        sort = PhotoSort.Default,
        onPhotoTap = { _ ->
            // Phase F wires the fullscreen viewer route. For C.2 the tap is
            // a no-op so the grid renders cleanly without dragging in unwired
            // navigation.
        },
        modifier = modifier.fillMaxSize(),
    )
}
