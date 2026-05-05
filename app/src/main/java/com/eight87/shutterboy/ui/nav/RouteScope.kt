package com.eight87.shutterboy.ui.nav

import androidx.compose.material3.SnackbarHostState
import com.eight87.shutterboy.AppGraph
import com.eight87.shutterboy.data.repo.FolderSource
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.data.repo.SmartAlbumSource
import com.eight87.shutterboy.data.settings.CustomOrderPreferences
import com.eight87.shutterboy.data.settings.SortPreferences

/**
 * R.E.1 — bundle of dependencies handed to every per-destination
 * `Register(scope)` extension. Adding a new destination means one new file
 * with one new `Register` extension; the navigation dispatcher in
 * [ShutterboyApp] is closed against modification.
 *
 * Each per-tab destination reads through narrow facets (R.A locked) — never
 * the wholesale `AppGraph`. The `graph` reference is held here only as a
 * convenience for routes that need multiple facets at once; new routes
 * should prefer the typed properties below.
 */
interface RouteScope {
    val graph: AppGraph
    val backStack: ShutterboyBackStack
    val snackbar: SnackbarHostState

    // Narrow facet reads — destinations consume these instead of the wholesale graph.
    val photoSource: PhotoSource
    val folderSource: FolderSource
    val smartAlbumSource: SmartAlbumSource
    val sortPreferences: SortPreferences
    val customOrderPreferences: CustomOrderPreferences
}
