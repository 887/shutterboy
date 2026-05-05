package com.eight87.shutterboy.ui.photos

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream

/**
 * Phase C.2 + C.3 + C.6 + D.3.5 — Photos tab body. Scaffold + dispatch
 * only. The pinch-zoom + density-grid + scrubber + sticky-banner stack
 * lives in [GalleryTimelineFrame]; this screen supplies the [PhotoStream]
 * and the tap-to-viewer wiring.
 */
@Composable
fun PhotosScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val stream = remember(scope) {
        PhotoStream { sort -> scope.photoSource.observePhotos(sort) }
    }

    GalleryTimelineFrame(
        stream = stream,
        onPhotoTap = { photoId, backingIds ->
            scope.backStack.push(PhotoViewer(photoId.value, backingIds))
        },
        modifier = modifier.fillMaxSize(),
    )
}
