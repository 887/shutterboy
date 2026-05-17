package com.eight87.shutterboy.ui.collections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.SourceType
import com.eight87.shutterboy.domain.sort.PhotoSort
import com.eight87.shutterboy.ui.multiselect.SelectionState
import com.eight87.shutterboy.ui.multiselect.SelectionTopBar
import com.eight87.shutterboy.ui.multiselect.rememberSelectionDeleteHandler
import com.eight87.shutterboy.ui.multiselect.rememberSelectionHolder
import com.eight87.shutterboy.ui.multiselect.rememberSelectionMoveHandler
import com.eight87.shutterboy.ui.nav.FolderDetail
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Slideshow
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream
import com.eight87.shutterboy.ui.sort.SortOverflowAction
import kotlinx.coroutines.launch

/**
 * Phase D.2 + E.2 + H.2 — folder-scoped timeline. Same density-zoomed grid +
 * scrubber + sticky-banner stack as the Photos tab (via
 * [GalleryTimelineFrame]), filtered repository-side via
 * `observePhotosInFolder(folderId, sort)`. TopAppBar carries the folder
 * display name + back arrow + a sort-overflow action. Phase H.2 swaps the
 * TopAppBar for a selection bar when selection mode is active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    destination: FolderDetail,
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val folderId = remember(destination) { FolderId(destination.folderIdValue) }
    val folder by scope.folderSource.observeFolder(folderId)
        .collectAsStateWithLifecycle(initialValue = null)
    val sort: PhotoSort by scope.sortPreferences.observeFolderSort(folderId)
        .collectAsStateWithLifecycle(initialValue = PhotoSort.Default)
    val coroutineScope = rememberCoroutineScope()
    val title = folder?.displayName ?: stringResource(R.string.folder_detail_title_default)
    val stream = remember(scope, folderId, sort) {
        PhotoStream { scope.photoSource.observePhotosInFolder(folderId, sort) }
    }
    val allPhotos by stream.observe().collectAsStateWithLifecycle(initialValue = emptyList())

    val selectionHolder = rememberSelectionHolder()
    val deleteHandler = rememberSelectionDeleteHandler(scope.photoDeleter) {
        selectionHolder.exit()
    }
    val moveHandler = rememberSelectionMoveHandler(
        photoMover = scope.photoMover,
        folderSource = scope.folderSource,
        snackbar = scope.snackbar,
        sourceFolderId = folderId,
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
                    onSelectAll = { selectionHolder.selectAll(allPhotos.map { it.id }) },
                    onMove = { moveHandler.request(active.selectedIds) },
                    onDelete = { deleteHandler.request(active.selectedIds) },
                )
            } else {
                TopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.backStack.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cd_folder_detail_back),
                            )
                        }
                    },
                    actions = {
                        SortOverflowAction(
                            sort = sort,
                            onSortChanged = { newSort ->
                                coroutineScope.launch {
                                    scope.sortPreferences.setFolderSort(folderId, newSort)
                                }
                            },
                            onStartSlideshow = {
                                val ids = allPhotos.map { it.id.value }
                                if (ids.isNotEmpty()) {
                                    scope.backStack.push(Slideshow(backingIds = ids))
                                }
                            },
                        )
                    },
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        val sourceType = folder?.sourceType ?: SourceType.DEVICE
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
            emptyState = { mod -> EmptyFolderState(sourceType = sourceType, modifier = mod) },
        )
    }
}
