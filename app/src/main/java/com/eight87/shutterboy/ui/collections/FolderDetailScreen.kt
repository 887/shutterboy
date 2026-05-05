package com.eight87.shutterboy.ui.collections

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
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.SourceType
import com.eight87.shutterboy.ui.nav.FolderDetail
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream

/**
 * Phase D.2 — folder-scoped timeline. Same density-zoomed grid + scrubber
 * + sticky-banner stack as the Photos tab (via [GalleryTimelineFrame]),
 * filtered repository-side via `observePhotosInFolder(folderId)` (R.F.12
 * — never client-side `.filter`). TopAppBar title is the folder's
 * display name; falls back to a generic label while the folder lookup
 * is in flight.
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
    val title = folder?.displayName ?: stringResource(R.string.folder_detail_title_default)
    val stream = remember(scope, folderId) {
        PhotoStream { sort -> scope.photoSource.observePhotosInFolder(folderId, sort) }
    }

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
    ) { innerPadding ->
        val sourceType = folder?.sourceType ?: SourceType.DEVICE
        GalleryTimelineFrame(
            stream = stream,
            onPhotoTap = { photoId, backingIds ->
                scope.backStack.push(PhotoViewer(photoId.value, backingIds))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            emptyState = { mod -> EmptyFolderState(sourceType = sourceType, modifier = mod) },
        )
    }
}
