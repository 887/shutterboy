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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
 * Phase D.1 — Collections root. Single [LazyVerticalGrid] hosts both
 * surfaces:
 *   - Section header + horizontal smart-album chip row at the top.
 *   - Section header + 2-column folder tiles below.
 *
 * Smart-album chip taps push [SmartAlbumDetail] (Phase D.3 body); folder
 * tile taps push [FolderDetail] (Phase D.2 body). The trailing `+ Manage`
 * chip routes to Settings → Library → Manage sources; until Phase I.3
 * lands that page the tap shows a snackbar.
 */
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

    val coroutineScope = rememberCoroutineScope()
    val manageMessage = stringResource(R.string.collections_manage_snackbar)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
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

        if (folders.isEmpty()) {
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
                items = folders,
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
