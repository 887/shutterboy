package com.eight87.shutterboy.ui.nav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.eight87.shutterboy.AppGraph
import com.eight87.shutterboy.ui.nav.routes.Register

/**
 * R.E.3 — root composable. Top-bar destination switcher (per-screen
 * via [RootTopBar]) + `NavDisplay` dispatch. Adding a new destination
 * is one entry line below + one `Register` extension in `routes/`.
 *
 * The legacy bottom [androidx.compose.material3.NavigationBar] is gone —
 * destination icons sit alongside the search affordance at the top of
 * each root screen instead.
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
        snackbarHost = { SnackbarHost(snackbar) },
        // m3-expressive B.5 — outer Scaffold yields the status-bar inset
        // to inner-screen TopAppBars so the inset doesn't get double-applied.
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Column(
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
                    entry<SettingsPhotos> { it.Register(scope) }
                    entry<SettingsLibrary> { it.Register(scope) }
                    entry<Search> { it.Register(scope) }
                    entry<SettingsAbout> { it.Register(scope) }
                    entry<Licenses> { it.Register(scope) }
                    entry<PhotoViewer> { it.Register(scope) }
                    entry<FolderDetail> { it.Register(scope) }
                    entry<Slideshow> { it.Register(scope) }
                },
            )
        }
    }
}
