package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import com.eight87.shutterboy.R
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.theme.SelectionAccent
import com.eight87.shutterboy.ui.nav.LocalAnimatedContentScopeOrNull
import com.eight87.shutterboy.ui.nav.LocalSharedTransitionScope
import com.eight87.shutterboy.ui.nav.photoSharedElementKey

/**
 * Phase H.2 + m3-expressive F.3 pattern 2 — single-thumbnail tile with
 * multi-select chrome.
 *
 * Three render modes determined by [inSelectionMode] + [selected]:
 *  - idle (`inSelectionMode = false`): plain Coil thumbnail; single-tap fires
 *    [onTap]; long-press fires [onLongPress] to enter selection mode (the
 *    caller seeds the selection with this photo).
 *  - selection-mode unselected: same plain render; tap toggles via [onTap].
 *  - selection-mode selected: 3-dp [SelectionAccent] border around the
 *    thumbnail, content dimmed to alpha 0.55, filled-check badge top-left.
 *
 * Active-state colours are pinned ([SelectionAccent]) so a Custom-seed theme
 * can't drift them onto a clashing hue (m3-expressive Finding 11).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoThumbnail(
    photo: Photo,
    selected: Boolean,
    inSelectionMode: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .testTag(photoThumbnailTag(photo.id.value))
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        val context = LocalContext.current
        val targetPx = LocalThumbnailQuality.current.targetPx
        // F.7 — grid tile half of the shared-element transition. Wraps
        // the Coil thumbnail in `Modifier.sharedElement(...)` when both
        // the SharedTransitionScope (from ShutterboyApp's
        // SharedTransitionLayout) and the AnimatedContentScope (from
        // Navigation3's NavDisplay per-entry AnimatedContent) are in
        // scope. Falls back to a plain modifier in unit tests, in
        // mounts that bypass the nav graph, or if the experimental API
        // throws — the existing crossfade on the viewer side covers the
        // no-shared-element case.
        val sharedScope = LocalSharedTransitionScope.current
        val animatedScope = LocalAnimatedContentScopeOrNull.current
        val sharedModifier: Modifier =
            if (sharedScope != null && animatedScope != null) {
                val contentState = with(sharedScope) {
                    rememberSharedContentState(key = photoSharedElementKey(photo.id.value))
                }
                runCatching {
                    with(sharedScope) {
                        Modifier.sharedElement(
                            sharedContentState = contentState,
                            animatedVisibilityScope = animatedScope,
                        )
                    }
                }.getOrDefault(Modifier)
            } else {
                Modifier
            }
        // Background tile that's always visible behind the AsyncImage.
        // While the image is loading and the cache miss falls through
        // the transparent placeholder, this provides the dark fill +
        // shows the CircularProgressIndicator (tonearmboy-style "actually
        // loading" affordance). Once AsyncImage paints the opaque
        // bitmap, it covers both.
        val placeholderColor = MaterialTheme.colorScheme.surfaceContainerHigh
        val transparentPainter = remember {
            androidx.compose.ui.graphics.painter.ColorPainter(
                androidx.compose.ui.graphics.Color.Transparent,
            )
        }
        // Aves-style suppress-decode-while-moving — but ONLY for tiles
        // that aren't already in the memory cache. Already-decoded
        // bitmaps stay shown during scroll (memory-cache hits are free);
        // only NEW tiles scrolling into view get the placeholder until
        // motion stops, so the queue doesn't flood with in-between
        // requests.
        val suppressDecode = LocalSuppressDecode.current
        val cacheKey = remember(photo.id.value, targetPx) {
            MemoryCache.Key("thumb-${photo.id.value}-$targetPx")
        }
        val inMemoryCache = remember(suppressDecode, cacheKey) {
            SingletonImageLoader.get(context).memoryCache?.get(cacheKey) != null
        }
        // Only paint the placeholder + spinner when the cell is NOT
        // already cached. CircularProgressIndicator runs an infinite
        // animation tied to Compose's clock; with ~28 visible tiles in
        // 4-col portrait that's ~28 animation tickers all forcing
        // recompositions at 120 Hz forever, even after the photo paints
        // on top. Gating on !inMemoryCache cuts that to "only cells
        // genuinely still loading", which is ~0 once you've scrolled
        // past them once.
        if (!inMemoryCache) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(placeholderColor),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Memoize the request build so we don't allocate a fresh
        // ImageRequest + Builder + string per recomposition. During a
        // fast scroll, 30 visible tiles × 60 Hz recompose rate without
        // this would be ~1800 builder allocations/sec — GC stalls
        // exactly when smoothness matters most.
        val request = remember(photo.id.value, targetPx) {
            val cacheKeyStr = "thumb-${photo.id.value}-$targetPx"
            val tinyKeyStr = "thumb-${photo.id.value}-${ThumbnailPrefetcher.TINY_PX}"
            ImageRequest.Builder(context)
                .data(photo.contentUri)
                .size(Size(targetPx, targetPx))
                // INEXACT lets a larger cached bitmap satisfy this request
                // without a redecode — every cell that's been seen at any
                // density is a free memory-cache hit instead of a fresh
                // decode + texture upload.
                .precision(Precision.INEXACT)
                .memoryCacheKey(cacheKeyStr)
                .diskCacheKey(cacheKeyStr)
                // Progressive preview: when AsyncImage mounts, it first
                // checks the TINY tier (96 px, populated by the
                // prefetcher) and renders it upscaled. The target-px
                // bitmap then decodes underneath and swaps in. So tiles
                // never show fully-grey — they fade from blurry-but-
                // recognisable to sharp.
                .placeholderMemoryCacheKey(tinyKeyStr)
                .build()
        }
        AsyncImage(
            model = if (suppressDecode && !inMemoryCache) null else request,
            // Note: request below is built with placeholderMemoryCacheKey
            // matching the memoryCacheKey, so if our prefetcher has
            // populated the cache the cached bitmap is used as the
            // placeholder synchronously — eliminating the one-frame
            // grey flash that happens before AsyncImage's normal cache
            // lookup resolves.
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            placeholder = transparentPainter,
            error = transparentPainter,
            fallback = transparentPainter,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(2.dp))
                // No background here — the spinner Box behind already
                // fills the tile with the placeholder color. If we
                // painted again here, AsyncImage's transparent
                // placeholder would never reveal the spinner.
                .then(sharedModifier)
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

        // inSelectionMode is currently informational; reserved for future
        // dim-the-non-selected affordances. Kept on the signature so callers
        // don't need to thread state through later (Phase H+).
        @Suppress("UNUSED_EXPRESSION") inSelectionMode
    }
}

/** Stable per-thumbnail test tag so Robolectric / mobile-mcp can find the tile. */
fun photoThumbnailTag(id: Long): String = "photo_thumbnail_$id"
