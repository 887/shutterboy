package com.eight87.shutterboy.ui.nav.routes

import androidx.compose.runtime.Composable
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.Photos
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.photos.PhotosScreen
import com.eight87.shutterboy.ui.nav.ExternalPhoto
import com.eight87.shutterboy.ui.viewer.ExternalPhotoViewerScreen
import com.eight87.shutterboy.ui.viewer.PhotoViewerScreen

/**
 * Per-destination Register extensions for the Photos surface area. Adding
 * a new Photos-tab destination is one new @Composable extension here.
 */
@Composable
fun Photos.Register(scope: RouteScope) {
    PhotosScreen(scope = scope)
}

/**
 * Phase C.6 — fullscreen viewer route registered. Phase F replaces the
 * placeholder body inside [PhotoViewerScreen] with the real pager.
 */
@Composable
fun PhotoViewer.Register(scope: RouteScope) {
    PhotoViewerScreen(destination = this, scope = scope)
}

/**
 * External-open viewer — pushed when MainActivity receives a VIEW
 * intent for an image or video URI from outside the app.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ExternalPhoto.Register(scope: RouteScope) {
    ExternalPhotoViewerScreen(
        uriString = uri,
        mime = mime,
        onBack = { scope.backStack.pop() },
    )
}
