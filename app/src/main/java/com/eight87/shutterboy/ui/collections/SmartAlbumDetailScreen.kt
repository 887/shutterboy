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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.SmartAlbumId
import com.eight87.shutterboy.ui.multiselect.SelectionState
import com.eight87.shutterboy.ui.multiselect.SelectionTopBar
import com.eight87.shutterboy.ui.multiselect.rememberSelectionDeleteHandler
import com.eight87.shutterboy.ui.multiselect.rememberSelectionHolder
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.SmartAlbumDetail
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream

/**
 * Phase D.3 + H.2 — smart-album-scoped timeline. Same density-zoomed grid +
 * scrubber + sticky-banner stack as the Photos tab (via
 * [GalleryTimelineFrame]), filtered via `observeSmartAlbum(id)`. Smart
 * albums have a fixed v1 order so no sort overflow lands here yet.
 *
 * An unrecognised `storageKey` (e.g. a destination round-tripped through
 * `SavedStateHandle` from an older build) renders the back-button + a
 * generic title and an empty body. Phase H.2 layers selection mode chrome.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAlbumDetailScreen(
    destination: SmartAlbumDetail,
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val albumId = remember(destination) {
        SmartAlbumId.fromStorageKey(destination.storageKey)
    }
    val title = albumId?.let { stringResource(it.labelRes()) }
        ?: stringResource(R.string.smart_album_detail_title_unknown)

    val selectionHolder = rememberSelectionHolder()
    val deleteHandler = rememberSelectionDeleteHandler(scope.photoDeleter) {
        selectionHolder.exit()
    }

    BackHandler(enabled = selectionHolder.state is SelectionState.Active) {
        selectionHolder.exit()
    }

    deleteHandler.Render()

    if (albumId == null) {
        Scaffold(
            topBar = {
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
                )
            },
            modifier = modifier.fillMaxSize(),
        ) { _ -> }
        return
    }

    val stream = remember(scope, albumId) {
        PhotoStream { scope.smartAlbumSource.observeSmartAlbum(albumId) }
    }
    val allPhotos by stream.observe().collectAsStateWithLifecycle(initialValue = emptyList())

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
                    title = { Text(text = title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.backStack.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cd_folder_detail_back),
                            )
                        }
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
