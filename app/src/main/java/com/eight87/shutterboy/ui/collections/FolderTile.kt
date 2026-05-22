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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.Folder
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.ui.photos.grid.LocalThumbnailQuality
import com.eight87.shutterboy.ui.photos.grid.ThumbnailPrefetcher

/**
 * Phase D.1 — folder tile in the Collections grid. Square cover image
 * (Coil [AsyncImage] against the cover photo's `contentUri`) sits above
 * the folder name + photo-count line. Lean tile pattern matching
 * [com.eight87.shutterboy.ui.photos.grid.PhotoThumbnail]:
 * memoised [ImageRequest], thumb cache-key alignment so a cover that's
 * already in the photos-grid memory cache hits instantly, and a stable
 * `(FolderId) -> Unit` click callback so every tile shares one lambda
 * identity (no per-cell recomposition on parent recompose).
 */
@Composable
internal fun FolderTile(
    folder: Folder,
    cover: Photo?,
    onClick: (FolderId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val targetPx = com.eight87.shutterboy.ui.photos.grid.LocalGridTargetPx.current
    val coverId = cover?.id?.value
    val coverUri = cover?.contentUri
    val request = remember(coverId, targetPx) {
        if (coverId == null || coverUri == null) {
            null
        } else {
            val cacheKey = "thumb-$coverId-$targetPx"
            ImageRequest.Builder(context)
                .data(coverUri)
                .size(Size(targetPx, targetPx))
                .precision(Precision.INEXACT)
                .memoryCacheKey(cacheKey)
                .diskCacheKey(cacheKey)
                .build()
        }
    }
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick(folder.id) }
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(placeholderColor),
        ) {
            if (request != null) {
                AsyncImage(
                    model = request,
                    contentDescription = folder.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
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
