package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.theme.SelectionAccent

/**
 * Lean grid tile. The per-cell composition is intentionally minimal so
 * cells entering the viewport during fast scroll don't pile measure +
 * layout + composition-local-read work onto the main thread:
 *
 * - No shared-element transition scope reads. The viewer crossfade
 *   covers the open animation now.
 * - No LocalSuppressDecode / per-cell memory-cache probe. The scrubber
 *   no longer scrolls the grid mid-drag so there's nothing to suppress.
 * - No `inSelectionMode` plumbing — the selection chrome reads from
 *   `selected` alone and the parent threads selection-state via its
 *   own `selected` lookup.
 * - Static spinner placeholder (no infinite animation tied to Compose's
 *   clock) lives behind the AsyncImage. The opaque thumbnail covers
 *   it once Coil paints; Compose's overdraw culling does the rest.
 */
@Composable
fun PhotoThumbnail(
    photo: Photo,
    selected: Boolean,
    onTap: (PhotoId) -> Unit,
    onLongPress: (PhotoId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val targetPx = LocalThumbnailQuality.current.targetPx
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val request = remember(photo.id.value, targetPx) {
        val cacheKey = "thumb-${photo.id.value}-$targetPx"
        val tinyKey = "thumb-${photo.id.value}-${ThumbnailPrefetcher.TINY_PX}"
        ImageRequest.Builder(context)
            .data(photo.contentUri)
            .size(Size(targetPx, targetPx))
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .placeholderMemoryCacheKey(tinyKey)
            .build()
    }
    val transparentPainter = remember { ColorPainter(Color.Transparent) }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag(photoThumbnailTag(photo.id.value))
            .background(placeholderColor)
            .combinedClickable(
                onClick = { onTap(photo.id) },
                onLongClick = { onLongPress(photo.id) },
            ),
    ) {
        CircularProgressIndicator(
            progress = { 0.75f },
            modifier = Modifier
                .align(Alignment.Center)
                .size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AsyncImage(
            model = request,
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            placeholder = transparentPainter,
            error = transparentPainter,
            fallback = transparentPainter,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(2.dp))
                .let { if (selected) it.alpha(0.55f) else it },
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 3.dp, color = SelectionAccent, shape = RoundedCornerShape(2.dp)),
            )
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.cd_multiselect_checkmark),
                    tint = SelectionAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Stable per-thumbnail test tag so Robolectric / mobile-mcp can find the tile. */
fun photoThumbnailTag(id: Long): String = "photo_thumbnail_$id"
