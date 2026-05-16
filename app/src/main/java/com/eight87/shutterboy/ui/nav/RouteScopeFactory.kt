package com.eight87.shutterboy.ui.nav

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eight87.shutterboy.AppGraph

/**
 * R.E builder. Constructs a [RouteScope] for the current render pass so the
 * navigation dispatcher hands it down to every `Register(scope)` extension.
 */
@Composable
fun rememberRouteScope(
    graph: AppGraph,
    backStack: ShutterboyBackStack,
    snackbar: SnackbarHostState,
): RouteScope {
    return remember(graph, backStack, snackbar) {
        object : RouteScope {
            override val graph = graph
            override val backStack = backStack
            override val snackbar = snackbar
            override val photoSource = graph.photoSource
            override val photoDeleter = graph.photoDeleter
            override val folderSource = graph.folderSource
            override val smartAlbumSource = graph.smartAlbumSource
            override val photoSearch = graph.photoSearch
            override val libraryScanner = graph.libraryScanner
            override val sortPreferences = graph.sortPreferences
            override val customOrderPreferences = graph.customOrderPreferences
            override val themePreferences = graph.themePreferences
        }
    }
}
