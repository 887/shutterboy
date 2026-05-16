package com.eight87.shutterboy.ui.photos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.sort.PhotoSort
import com.eight87.shutterboy.ui.multiselect.SelectionState
import com.eight87.shutterboy.ui.multiselect.SelectionTopBar
import com.eight87.shutterboy.ui.multiselect.rememberSelectionDeleteHandler
import com.eight87.shutterboy.ui.multiselect.rememberSelectionHolder
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Search
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream
import com.eight87.shutterboy.ui.sort.SortOverflowAction
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
    val coroutineScope = rememberCoroutineScope()
    val stream = remember(scope, sort) {
        PhotoStream { scope.photoSource.observePhotos(sort) }
    }
    val allPhotos by stream.observe().collectAsStateWithLifecycle(initialValue = emptyList())

    val selectionHolder = rememberSelectionHolder()
    val deleteHandler = rememberSelectionDeleteHandler(scope.photoDeleter) {
        selectionHolder.exit()
    }

    BackHandler(enabled = selectionHolder.state is SelectionState.Active) {
        selectionHolder.exit()
    }

    deleteHandler.Render()

    Scaffold(
        topBar = {
            val active = selectionHolder.state
            if (active is SelectionState.Active) {
                SelectionTopBar(
                    count = active.selectedIds.size,
                    onClose = { selectionHolder.exit() },
                    onSelectAll = { selectionHolder.selectAll(allPhotos.map { it.id }) },
                    onDelete = { deleteHandler.request(active.selectedIds) },
                )
            } else {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.photos_top_title)) },
                    actions = {
                        IconButton(onClick = { scope.backStack.push(Search) }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(R.string.cd_search_open),
                            )
                        }
                        SortOverflowAction(
                            sort = sort,
                            onSortChanged = { newSort ->
                                coroutineScope.launch {
                                    scope.sortPreferences.setPhotosSort(newSort)
                                }
                            },
                        )
                    },
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        GalleryTimelineFrame(
            stream = stream,
            onPhotoTap = { photoId, backingIds ->
                if (selectionHolder.state is SelectionState.Active) {
                    selectionHolder.toggle(photoId)
                } else {
                    scope.backStack.push(PhotoViewer(photoId.value, backingIds))
                }
            },
            selectionState = selectionHolder.state,
            onPhotoLongPress = { photoId -> selectionHolder.enterActive(photoId) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
