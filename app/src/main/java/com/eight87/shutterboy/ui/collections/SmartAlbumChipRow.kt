package com.eight87.shutterboy.ui.collections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.SmartAlbumId

private val ChipSize = 104.dp

/**
 * Phase D.1 — horizontal LazyRow of smart-album chips. Each chip is a
 * round-cornered cover-photo tile + a label below. The catalogue order is
 * fixed at v1 ([SmartAlbumId.defaultOrder]); Phase E.4 lands user-pinned
 * reordering via DataStore-persisted [SmartAlbumId.storageKey] list.
 *
 * Trailing `+ Manage` chip routes to Settings → Library → Manage sources;
 * until Phase I.3 lands that surface, the tap shows a snackbar via
 * [onManageTap].
 */
@Composable
internal fun SmartAlbumChipRow(
    covers: Map<SmartAlbumId, Photo?>,
    onChipTap: (SmartAlbumId) -> Unit,
    onManageTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(SmartAlbumId.defaultOrder, key = { it.storageKey }) { id ->
            SmartAlbumChip(
                id = id,
                cover = covers[id],
                onClick = { onChipTap(id) },
            )
        }
        item(key = "chip-manage", contentType = "chip_manage") {
            ManageChip(onClick = onManageTap)
        }
    }
}

@Composable
private fun SmartAlbumChip(
    id: SmartAlbumId,
    cover: Photo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(id = id.labelRes())
    Surface(
        modifier = modifier.width(ChipSize),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .size(ChipSize)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.BottomStart,
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover.contentUri,
                    contentDescription = label,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.surface,
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ManageChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ChipSize)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.collections_chip_manage),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

internal fun SmartAlbumId.labelRes(): Int = when (this) {
    SmartAlbumId.Camera -> R.string.collections_chip_camera
    SmartAlbumId.Screenshots -> R.string.collections_chip_screenshots
    SmartAlbumId.Favorites -> R.string.collections_chip_favorites
    SmartAlbumId.Recents -> R.string.collections_chip_recents
}
