package com.eight87.shutterboy.ui.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import com.eight87.shutterboy.ui.nav.Collections
import com.eight87.shutterboy.ui.nav.Favorites
import com.eight87.shutterboy.ui.nav.FolderDetail
import com.eight87.shutterboy.ui.nav.RootTopBar
import com.eight87.shutterboy.ui.nav.RouteScope
import kotlinx.coroutines.launch

/**
 * Collections root — folder tiles with user-pinned ordering. Top bar
 * is the shared [RootTopBar] (destination switcher + overflow); the
 * overflow opens the folder reorder dialog.
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
    val favoriteIds by scope.favoriteCommands.observeFavoriteIds()
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val folderOrder by scope.customOrderPreferences.observeFolderOrder()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val orderedFolders = remember(folders, folderOrder) {
        applyCustomOrder(folders, folderOrder) { it.id }
    }

    val coroutineScope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    var showReorderFolders by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            RootTopBar(
                current = Collections,
                onSelect = { dest -> scope.backStack.selectTab(dest) },
            ) {
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
                            Text(stringResource(R.string.collections_overflow_reorder_folders))
                        },
                        onClick = {
                            menuOpen = false
                            showReorderFolders = true
                        },
                    )
                }
            }
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
            if (favoriteIds.isNotEmpty()) {
                item(
                    span = { GridItemSpan(maxLineSpan) },
                    key = "section-favorites",
                    contentType = "section_header",
                ) {
                    SectionHeader(text = stringResource(R.string.collections_section_favorites))
                }
                item(
                    key = "favorites-tile",
                    contentType = "favorites_tile",
                ) {
                    FavoritesTile(
                        count = favoriteIds.size,
                        onClick = { scope.backStack.push(Favorites) },
                    )
                }
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
private fun FavoritesTile(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = stringResource(R.string.favorites_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.collections_favorites_tile_subtitle,
                        count,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
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
