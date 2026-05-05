package com.eight87.shutterboy.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.SmartAlbumId
import com.eight87.shutterboy.ui.nav.FolderDetail
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.SmartAlbumDetail
import kotlinx.coroutines.launch

/**
 * Phase D.1 + E.4 — Collections root. Single [LazyVerticalGrid] hosts
 * both surfaces:
 *   - Section header + horizontal smart-album chip row at the top.
 *   - Section header + 2-column folder tiles below.
 *
 * Smart-album chip taps push [SmartAlbumDetail]; folder tile taps push
 * [FolderDetail]. The trailing `+ Manage` chip surfaces a snackbar
 * (Phase I.3 lands the real Manage sources page).
 *
 * **Custom order (Phase E.4):** chip + folder ordering layers user-pinned
 * persistence ([com.eight87.shutterboy.data.settings.CustomOrderPreferences])
 * over the natural ordering via [applyCustomOrder]. New items (a fresh
 * folder from a scan) land at the end, preserving the user's pin. The
 * top-bar overflow opens [ReorderListDialog] for either surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionsScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val folders by scope.folderSource.observeFolders()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val folderCovers by scope.folderSource.observeFolderCovers()
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    val smartCovers: Map<SmartAlbumId, Photo?> by scope.smartAlbumSource
        .observeSmartAlbumCovers()
        .collectAsStateWithLifecycle(initialValue = emptyMap())
    val smartAlbumOrder by scope.customOrderPreferences.observeSmartAlbumOrder()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val folderOrder by scope.customOrderPreferences.observeFolderOrder()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val orderedAlbums = remember(smartAlbumOrder) {
        applyCustomOrder(SmartAlbumId.defaultOrder, smartAlbumOrder) { it }
    }
    val orderedFolders = remember(folders, folderOrder) {
        applyCustomOrder(folders, folderOrder) { it.id }
    }

    val coroutineScope = rememberCoroutineScope()
    val manageMessage = stringResource(R.string.collections_manage_snackbar)

    var menuOpen by remember { mutableStateOf(false) }
    var showReorderAlbums by remember { mutableStateOf(false) }
    var showReorderFolders by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.nav_collections)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.cd_more_options),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.collections_overflow_reorder_smart_albums))
                            },
                            onClick = {
                                menuOpen = false
                                showReorderAlbums = true
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.collections_overflow_reorder_folders))
                            },
                            onClick = {
                                menuOpen = false
                                showReorderFolders = true
                            },
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "section-smart-albums",
                contentType = "section_header",
            ) {
                SectionHeader(text = stringResource(R.string.collections_section_smart_albums))
            }

            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "row-smart-albums",
                contentType = "smart_album_row",
            ) {
                SmartAlbumChipRow(
                    albums = orderedAlbums,
                    covers = smartCovers,
                    onChipTap = { id -> scope.backStack.push(SmartAlbumDetail(id.storageKey)) },
                    onManageTap = {
                        coroutineScope.launch { scope.snackbar.showSnackbar(manageMessage) }
                    },
                )
            }

            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "section-folders",
                contentType = "section_header",
            ) {
                SectionHeader(text = stringResource(R.string.collections_section_folders))
            }

            if (orderedFolders.isEmpty()) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "empty-folders",
                    contentType = "empty_state",
                ) {
                    EmptyFoldersBlock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                }
            } else {
                items(
                    items = orderedFolders,
                    key = { it.id.value },
                    contentType = { "folder_tile" },
                ) { folder ->
                    FolderTile(
                        folder = folder,
                        cover = folderCovers[folder.id],
                        onClick = { scope.backStack.push(FolderDetail(folder.id.value)) },
                    )
                }
            }
        }
    }

    if (showReorderAlbums) {
        ReorderListDialog(
            title = stringResource(R.string.collections_reorder_smart_albums_title),
            items = orderedAlbums,
            keyOf = { it.storageKey },
            labelOf = { stringResource(it.labelRes()) },
            onApply = { newOrder ->
                coroutineScope.launch {
                    scope.customOrderPreferences.setSmartAlbumOrder(newOrder)
                }
            },
            onDismiss = { showReorderAlbums = false },
        )
    }

    if (showReorderFolders) {
        ReorderListDialog(
            title = stringResource(R.string.collections_reorder_folders_title),
            items = orderedFolders,
            keyOf = { it.id.value.toString() },
            labelOf = { it.displayName },
            onApply = { newOrder ->
                coroutineScope.launch {
                    scope.customOrderPreferences.setFolderOrder(newOrder.map { it.id })
                }
            },
            onDismiss = { showReorderFolders = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
private fun EmptyFoldersBlock(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.collections_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.collections_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}
