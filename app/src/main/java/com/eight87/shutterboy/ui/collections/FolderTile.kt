package com.eight87.shutterboy.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.Photo

/**
 * Phase D.1 — folder tile in the Collections grid. Square cover image
 * (Coil `AsyncImage` against the cover photo's `contentUri`) sits above
 * the folder name + photo-count line. Tile cover comes from
 * `FolderSource.observeFolderCovers()`; folders without a cover render a
 * placeholder swatch so layout stays stable while a scan is mid-flight.
 */
@Composable
internal fun FolderTile(
    folder: Folder,
    cover: Photo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover.contentUri,
                    contentDescription = folder.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
            }
        }
        Text(
            text = folder.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.collections_folder_count, folder.photoCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
