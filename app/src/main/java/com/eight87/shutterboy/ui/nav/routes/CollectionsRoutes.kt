package com.eight87.shutterboy.ui.nav.routes

import androidx.compose.runtime.Composable
import com.eight87.shutterboy.ui.collections.CollectionsScreen
import com.eight87.shutterboy.ui.collections.FolderDetailScreen
import com.eight87.shutterboy.ui.nav.Collections
import com.eight87.shutterboy.ui.nav.FolderDetail
import com.eight87.shutterboy.ui.nav.RouteScope

/**
 * Per-destination Register extensions for the Collections surface area.
 */
@Composable
fun Collections.Register(scope: RouteScope) {
    CollectionsScreen(scope = scope)
}

@Composable
fun FolderDetail.Register(scope: RouteScope) {
    FolderDetailScreen(destination = this, scope = scope)
}
