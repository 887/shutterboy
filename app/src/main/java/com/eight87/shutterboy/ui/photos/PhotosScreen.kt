package com.eight87.shutterboy.ui.photos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material.icons.outlined.ViewQuilt
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.sort.PhotoSort
import com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel
import com.eight87.shutterboy.ui.photos.grid.cycleNext
import com.eight87.shutterboy.ui.multiselect.SelectionState
import com.eight87.shutterboy.ui.multiselect.SelectionTopBar
import com.eight87.shutterboy.ui.multiselect.rememberSelectionDeleteHandler
import com.eight87.shutterboy.ui.multiselect.rememberSelectionHolder
import com.eight87.shutterboy.ui.multiselect.rememberSelectionMoveHandler
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.Photos
import com.eight87.shutterboy.ui.nav.RootTopBar
import com.eight87.shutterboy.ui.nav.rootSwipe
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Search
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream
import kotlinx.coroutines.launch

/**
 * Phase C.2 + C.3 + C.6 + D.3.5 + E.2 + H.2 — Photos tab body. Scaffold +
 * TopAppBar (title + sort-overflow) + dispatch. The pinch-zoom + density-
 * grid + scrubber + sticky-banner stack lives in `GalleryTimelineFrame`;
 * this screen supplies the `PhotoStream` (sort baked in from the persisted
 * `photos_sort`) and the tap-to-viewer wiring.
 *
 * Phase H.2 layers selection mode on top: long-press enters Active, the
 * TopAppBar swaps for `SelectionTopBar`, tap toggles, back-button exits
 * selection (and only on the next press leaves the screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val sort: PhotoSort by scope.sortPreferences.observePhotosSort()
        .collectAsStateWithLifecycle(initialValue = PhotoSort.Default)
    val initialDensity: PhotosZoomLevel by scope.displayPreferences.observeDefaultGridDensity()
        .collectAsStateWithLifecycle(initialValue = PhotosZoomLevel.Items)
    val coroutineScope = rememberCoroutineScope()
    // Subscribe to the app-scoped photo feed (held hot by AppGraph) so
    // navigating to the viewer and back doesn't tear down the Room
    // subscription. New subscriptions get the cached list instantly —
    // no black-screen gap on back.
    val photosFeed = scope.graph.photosFeed
    val stream = remember(photosFeed) { PhotoStream { photosFeed } }
    val allPhotos by photosFeed.collectAsStateWithLifecycle()
    // Density state hoisted here so the column-count action button in
    // the top bar can drive it. Seeded from the user's persisted
    // default density; pinch gestures inside GalleryTimelineFrame also
    // route back into this state via onLevelChange.
    var zoomLevel by remember(initialDensity) {
        mutableStateOf<com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel>(initialDensity)
    }

    val selectionHolder = rememberSelectionHolder()
    val deleteHandler = rememberSelectionDeleteHandler(scope.photoDeleter) {
        selectionHolder.exit()
    }
    val moveHandler = rememberSelectionMoveHandler(
        photoMover = scope.photoMover,
        folderSource = scope.folderSource,
        snackbar = scope.snackbar,
        sourceFolderId = null,
    ) { selectionHolder.exit() }

    BackHandler(enabled = selectionHolder.state is SelectionState.Active) {
        selectionHolder.exit()
    }

    deleteHandler.Render()
    moveHandler.Render()

    Scaffold(
        topBar = {
            val active = selectionHolder.state
            if (active is SelectionState.Active) {
                SelectionTopBar(
                    count = active.selectedIds.size,
                    onClose = { selectionHolder.exit() },
                    onSelectAll = {
                        selectionHolder.selectAll(allPhotos.orEmpty().map { it.id })
                    },
                    onMove = { moveHandler.request(active.selectedIds) },
                    onDelete = { deleteHandler.request(active.selectedIds) },
                )
            } else {
                androidx.compose.foundation.layout.Column {
                    RootTopBar(
                        current = Photos,
                        onSelect = { dest -> scope.backStack.selectTab(dest) },
                    ) {
                        // Search sits directly left of the Photos tab.
                        // Sort lives on Settings → Photos → Default sort
                        // (canonical surface); the kebab overflow was
                        // unused here, so it's gone. Slideshow can be
                        // started from FolderDetail's overflow + Search
                        // results — Photos timeline doesn't need its own
                        // entry point until a real use case shows up.
                        com.eight87.shutterboy.ui.photos.grid.ColumnCountButton(
                            level = zoomLevel,
                            onLevelChange = { zoomLevel = it },
                        )
                    }
                    com.eight87.shutterboy.ui.nav.ScanProgressStrip(
                        scanner = scope.libraryScanner,
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectionHolder.state !is SelectionState.Active) {
                // Two stacked FABs. Density cycle on top (quick access
                // to gallery layout — Items / Days / Months / Years),
                // Search on the bottom. Small density FAB so it reads
                // as a secondary action against the primary Search.
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                ) {
                    androidx.compose.material3.SmallFloatingActionButton(
                        onClick = { zoomLevel = zoomLevel.cycleNext() },
                    ) {
                        Icon(
                            imageVector = densityIcon(zoomLevel),
                            contentDescription = null,
                        )
                    }
                    androidx.compose.material3.FloatingActionButton(
                        onClick = { scope.backStack.push(Search) },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.cd_search_open),
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.Start,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        GalleryTimelineFrame(
            stream = stream,
            onPhotoTap = { photoId, backingIds ->
                if (selectionHolder.state is SelectionState.Active) {
                    selectionHolder.toggle(photoId)
                } else {
                    val key = scope.graph.stashBackingIds(backingIds)
                    scope.backStack.push(PhotoViewer(photoId.value, key))
                }
            },
            selectionState = selectionHolder.state,
            onPhotoLongPress = { photoId -> selectionHolder.enterActive(photoId) },
            initialZoomLevel = initialDensity,
            level = zoomLevel,
            onLevelChange = { zoomLevel = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .rootSwipe(
                    current = Photos,
                    onSwitchTab = { scope.backStack.selectTab(it) },
                ),
        )
    }
}

private fun densityIcon(level: PhotosZoomLevel) = when (level) {
    PhotosZoomLevel.Items -> Icons.Outlined.GridView
    PhotosZoomLevel.Days -> Icons.Outlined.ViewModule
    PhotosZoomLevel.Months -> Icons.Outlined.ViewQuilt
    PhotosZoomLevel.Years -> Icons.AutoMirrored.Outlined.ViewList
}
