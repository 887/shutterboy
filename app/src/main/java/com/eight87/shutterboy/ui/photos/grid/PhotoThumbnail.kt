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
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    val targetPx = LocalGridTargetPx.current
    val lowPx = LocalGridLowPx.current
    val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val prefetcher = LocalPrefetcher.current
    // Progressive-quality. On compose we render at `lowPx` (always
    // 0.5× cell, cheap) so scroll is fast and tiles paint instantly.
    // After 250 ms of dwell on screen we upgrade to `targetPx` (the
    // user's quality setting). Tiles that scroll off before 250 ms
    // never trigger the upgrade decode — no wasted work during fast
    // scrolling.
    //
    // When the user has set quality to Low, `lowPx == targetPx` and
    // the upgrade is a no-op; the cell just renders at low.
    val needsUpgrade = lowPx != targetPx
    var useTarget by remember(photo.id.value, targetPx, lowPx) {
        // If the target tier is already in cache from a scheduler
        // look-ahead, render it directly — skip the low intermediate.
        val loader = coil3.SingletonImageLoader.get(context)
        val targetCached = loader.memoryCache
            ?.get(coil3.memory.MemoryCache.Key("thumb-${photo.id.value}-$targetPx")) != null
        mutableStateOf(targetCached || !needsUpgrade)
    }
    LaunchedEffect(photo.id.value, targetPx, prefetcher) {
        prefetcher?.submit(photo.id.value, photo.contentUri, targetPx)
    }
    LaunchedEffect(photo.id.value, useTarget, needsUpgrade) {
        if (!useTarget && needsUpgrade) {
            kotlinx.coroutines.delay(250L)
            useTarget = true
        }
    }
    val request = remember(photo.id.value, targetPx, lowPx, useTarget) {
        val px = if (useTarget) targetPx else lowPx
        val cacheKey = "thumb-${photo.id.value}-$px"
        val lowKey = "thumb-${photo.id.value}-$lowPx"
        ImageRequest.Builder(context)
            .data(photo.contentUri)
            .size(Size(px, px))
            .precision(Precision.INEXACT)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .apply {
                // While the target decode is in flight, show the
                // already-decoded low bitmap instead of the spinner.
                if (useTarget && needsUpgrade) {
                    placeholderMemoryCacheKey(lowKey)
                }
            }
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
        // Video play-icon overlay. White circle with a black triangle —
        // reads against any background colour. Sits in the bottom-right
        // so it doesn't conflict with the selection check in the
        // top-left.
        if (photo.mimeType.startsWith("video/")) {
            Icon(
                imageVector = Icons.Filled.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .size(22.dp),
            )
        }
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
