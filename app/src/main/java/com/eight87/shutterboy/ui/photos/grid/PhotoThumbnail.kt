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
import androidx.compose.material3.Icon
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
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
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
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.contentUri)
                .size(Size(targetPx, targetPx))
                .build(),
            contentDescription = photo.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(2.dp))
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
