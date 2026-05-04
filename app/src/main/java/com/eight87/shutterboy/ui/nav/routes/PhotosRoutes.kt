package com.eight87.shutterboy.ui.nav.routes

import androidx.compose.runtime.Composable
import com.eight87.shutterboy.ui.nav.Photos
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.photos.PhotosScreen

/**
 * Per-destination Register extension for the Photos tab. Adding a new
 * Photos-tab destination (e.g. PhotoDetail in Phase F) is one new
 * @Composable extension here.
 */
@Composable
fun Photos.Register(scope: RouteScope) {
    PhotosScreen(scope = scope)
}
