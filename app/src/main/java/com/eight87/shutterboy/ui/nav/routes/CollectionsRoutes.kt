package com.eight87.shutterboy.ui.nav.routes

import androidx.compose.runtime.Composable
import com.eight87.shutterboy.ui.collections.CollectionsScreen
import com.eight87.shutterboy.ui.collections.FolderDetailScreen
import com.eight87.shutterboy.ui.collections.SmartAlbumDetailScreen
import com.eight87.shutterboy.ui.nav.Collections
import com.eight87.shutterboy.ui.nav.FolderDetail
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.SmartAlbumDetail

/**
 * Per-destination Register extensions for the Collections surface area.
 * Phase D.1 lands the root + the two detail bodies.
 */
@Composable
fun Collections.Register(scope: RouteScope) {
    CollectionsScreen(scope = scope)
}

@Composable
fun FolderDetail.Register(scope: RouteScope) {
    FolderDetailScreen(destination = this, scope = scope)
}

@Composable
fun SmartAlbumDetail.Register(scope: RouteScope) {
    SmartAlbumDetailScreen(destination = this, scope = scope)
}
