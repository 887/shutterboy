package com.eight87.shutterboy.ui.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.eight87.shutterboy.AppGraph
import com.eight87.shutterboy.data.settings.ThumbnailQuality
import com.eight87.shutterboy.ui.nav.routes.Register
import com.eight87.shutterboy.ui.photos.grid.LocalThumbnailQuality
import com.eight87.shutterboy.ui.settings.catalog.FlashRowController
import com.eight87.shutterboy.ui.settings.catalog.LocalHighlightedSettingId

/**
 * R.E.3 — root composable. Top-bar destination switcher (per-screen
 * via [RootTopBar]) + `NavDisplay` dispatch. Adding a new destination
 * is one entry line below + one `Register` extension in `routes/`.
 *
 * The legacy bottom [androidx.compose.material3.NavigationBar] is gone —
 * destination icons sit alongside the search affordance at the top of
 * each root screen instead.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
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

    val thumbnailQuality by graph.displayPreferences.observeThumbnailQuality()
        .collectAsStateWithLifecycle(initialValue = ThumbnailQuality.Medium)

    CompositionLocalProvider(
        LocalThumbnailQuality provides thumbnailQuality,
        // I.7 — wire the settings-search flash channel into composition
        // so any [SettingsRow] whose id matches `FlashRowController.state`
        // briefly highlights on arrival from the search overlay.
        LocalHighlightedSettingId provides FlashRowController.state,
    ) {
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
            // Scan progress strip moved into each root screen's RootTopBar
            // (renders BELOW the buttons row, like tonearmboy puts it
            // below its library top bar). The outer shell no longer hosts
            // it here.
            // F.7 — wrap the NavDisplay in a SharedTransitionLayout and
            // expose its SharedTransitionScope via a composition local so
            // the Photos timeline tile and the viewer page can hook into
            // a single shared-element transition across the two routes.
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    NavDisplay(
                        backStack = backStack.backStack,
                        onBack = { backStack.pop() },
                        modifier = Modifier.fillMaxSize(),
                        entryProvider = entryProvider {
                            entry<Photos> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<Collections> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<Settings> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<SettingsPhotos> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<SettingsLibrary> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<SettingsLookAndFeel> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<SettingsManageSources> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<SettingsSearch> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<Search> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<SettingsAbout> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<Licenses> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<PhotoViewer> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<FolderDetail> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<Favorites> { WithNavAnimatedContentScope { it.Register(scope) } }
                            entry<Slideshow> { WithNavAnimatedContentScope { it.Register(scope) } }
                        },
                    )
                }
            }
        }
    }
    }
}
