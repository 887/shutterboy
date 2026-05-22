package com.eight87.shutterboy.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.FavoriteCommands
import com.eight87.shutterboy.ui.nav.Favorites
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RootTopBar
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.ShutterboyBackStack
import com.eight87.shutterboy.ui.nav.rootSwipe
import com.eight87.shutterboy.ui.photos.grid.GalleryTimelineFrame
import com.eight87.shutterboy.ui.photos.grid.PhotoStream

/**
 * Phase G.3 — Favorites destination. Same density-zoomed timeline as
 * [FolderDetailScreen], fed by [FavoriteCommands.observeFavoritePhotos].
 * No sort overflow (favorites is a flat set, sorted newest-first at the
 * DAO level), no selection / move / delete in v1 — those affordances
 * stay on the source surface, the heart toggle in the viewer is the
 * single in / out mechanism.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    FavoritesScreenContent(
        favoriteCommands = scope.favoriteCommands,
        backStack = scope.backStack,
        onBack = { scope.backStack.pop() },
        onPhotoTap = { photoId, backingIds ->
            val key = scope.graph.stashBackingIds(backingIds)
            scope.backStack.push(PhotoViewer(photoId.value, key))
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FavoritesScreenContent(
    favoriteCommands: FavoriteCommands,
    backStack: ShutterboyBackStack? = null,
    onBack: () -> Unit = {},
    onPhotoTap: (com.eight87.shutterboy.domain.PhotoId, List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stream = remember(favoriteCommands) {
        PhotoStream { favoriteCommands.observeFavoritePhotos() }
    }
    var zoomLevel by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf<com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel>(
            com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel.Items,
        )
    }
    var favoritesColumns by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(4)
    }
    Scaffold(
        topBar = {
            if (backStack != null) {
                RootTopBar(
                    current = Favorites,
                    onSelect = { dest -> backStack.selectTab(dest) },
                ) {
                    com.eight87.shutterboy.ui.photos.grid.ColumnCountButton(
                        count = favoritesColumns,
                        onCountChange = { favoritesColumns = it },
                    )
                }
            } else {
                // Legacy back-arrow header — kept for tests + any
                // call-site that still pushes Favorites onto the
                // detail stack.
                TopAppBar(
                    title = { Text(text = stringResource(R.string.favorites_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription =
                                    stringResource(R.string.cd_favorites_back),
                            )
                        }
                    },
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        val swipeModifier =
            if (backStack != null) {
                Modifier.rootSwipe(
                    current = com.eight87.shutterboy.ui.nav.Favorites,
                    onSwitchTab = { backStack.selectTab(it) },
                )
            } else {
                Modifier
            }
        GalleryTimelineFrame(
            stream = stream,
            onPhotoTap = onPhotoTap,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(swipeModifier),
            emptyState = { mod -> FavoritesEmptyState(modifier = mod) },
            level = zoomLevel,
            onLevelChange = { zoomLevel = it },
            columns = favoritesColumns,
        )
    }
}

@Composable
private fun FavoritesEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.favorites_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.favorites_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}
