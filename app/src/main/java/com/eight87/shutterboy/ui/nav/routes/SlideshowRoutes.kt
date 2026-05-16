package com.eight87.shutterboy.ui.nav.routes

import androidx.compose.runtime.Composable
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Slideshow
import com.eight87.shutterboy.ui.slideshow.SlideshowScreen

/**
 * Phase J.1 — per-destination Register extension for the fullscreen
 * slideshow surface. J.3 (overflow-menu wiring on Photos / FolderDetail
 * / SmartAlbumDetail / Search) is deferred — the route is registered
 * here so siblings can push it once H-phase selection mode settles.
 */
@Composable
fun Slideshow.Register(scope: RouteScope) {
    SlideshowScreen(destination = this, scope = scope)
}
