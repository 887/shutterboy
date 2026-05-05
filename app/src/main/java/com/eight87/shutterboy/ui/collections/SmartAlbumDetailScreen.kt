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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.SmartAlbumId
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.SmartAlbumDetail
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream

/**
 * Phase D.3 — smart-album-scoped timeline. Same density-zoomed grid +
 * scrubber + sticky-banner stack as the Photos tab (via
 * [GalleryTimelineFrame]), filtered via `observeSmartAlbum(id)` (sort
 * ignored — smart albums have a fixed order in v1). TopAppBar title is
 * the album's localised label. An unrecognised storageKey (e.g. a
 * destination round-tripped through `SavedStateHandle` from an older
 * build) renders the back-button + a generic title and an empty body.
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
        if (albumId == null) {
            return@Scaffold
        }
        val stream = remember(scope, albumId) {
            PhotoStream { _ -> scope.smartAlbumSource.observeSmartAlbum(albumId) }
        }
        GalleryTimelineFrame(
            stream = stream,
            onPhotoTap = { photoId, backingIds ->
                scope.backStack.push(PhotoViewer(photoId.value, backingIds))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

