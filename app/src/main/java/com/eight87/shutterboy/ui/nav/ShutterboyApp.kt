package com.eight87.shutterboy.ui.nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.eight87.shutterboy.AppGraph
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.routes.Register

/**
 * R.E.3 — root composable. Bottom-nav scaffold + per-destination
 * `Register(scope)` dispatch. Adding a new destination is one entry
 * line below + one `Register` extension in `routes/`.
 *
 * LOC ceiling: 150. If this file grows past that, a route renderer
 * leaked back inline; extract.
 */
@Composable
fun ShutterboyApp(graph: AppGraph) {
    val backStack = remember { ShutterboyBackStack(rootKey = Photos) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberRouteScope(graph = graph, backStack = backStack, snackbar = snackbar)

    // incremental-scan first-collect hook — gates the cold-start rescan on
    // MediaStore.getGeneration() + the SAF tree fingerprint. Runs post-first-
    // frame in a LaunchedEffect so the splash dismiss isn't blocked and the
    // critical path stays clean (cold-start-perf A.2).
    LaunchedEffect(Unit) {
        graph.libraryScanner.scanIfChanged()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                rootDestinations.forEach { dest ->
                    val selected = backStack.currentTab == dest
                    val (icon, label, cd) = navIconLabelCd(dest, selected)
                    NavigationBarItem(
                        selected = selected,
                        onClick = { backStack.selectTab(dest) },
                        icon = { Icon(imageVector = icon, contentDescription = cd) },
                        label = { Text(text = label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        // m3-expressive B.5 — outer Scaffold yields the status-bar inset
        // to inner-screen TopAppBars so the inset doesn't get double-applied.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Non-blocking scan progress signal — surfaces ScanProgress.Running
            // as a thin indeterminate strip at the top of the app shell.
            ScanProgressStrip(scanner = graph.libraryScanner)
            NavDisplay(
                backStack = backStack.backStack,
                onBack = { backStack.pop() },
                modifier = Modifier.fillMaxSize(),
                entryProvider = entryProvider {
                    entry<Photos> { it.Register(scope) }
                    entry<Collections> { it.Register(scope) }
                    entry<Settings> { it.Register(scope) }
                    entry<SettingsAbout> { it.Register(scope) }
                    entry<PhotoViewer> { it.Register(scope) }
                    entry<FolderDetail> { it.Register(scope) }
                    entry<SmartAlbumDetail> { it.Register(scope) }
                },
            )
        }
    }
}

@Composable
private fun navIconLabelCd(
    dest: Destination,
    selected: Boolean,
): Triple<ImageVector, String, String> = when (dest) {
    Photos -> Triple(
        if (selected) Icons.Filled.Image else Icons.Outlined.Image,
        stringResource(R.string.nav_photos),
        stringResource(R.string.cd_nav_photos),
    )
    Collections -> Triple(
        if (selected) Icons.Filled.Collections else Icons.Outlined.Collections,
        stringResource(R.string.nav_collections),
        stringResource(R.string.cd_nav_collections),
    )
    Settings -> Triple(
        if (selected) Icons.Filled.Settings else Icons.Outlined.Settings,
        stringResource(R.string.nav_settings),
        stringResource(R.string.cd_nav_settings),
    )
    // Pushed-not-rooted destinations should never reach the bottom-nav icon
    // resolver; the entry exhaustiveness is structural only.
    SettingsAbout,
    is PhotoViewer,
    is FolderDetail,
    is SmartAlbumDetail ->
        error("$dest is not a bottom-nav root destination")
}
