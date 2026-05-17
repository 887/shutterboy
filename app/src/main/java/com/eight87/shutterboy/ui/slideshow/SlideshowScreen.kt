package com.eight87.shutterboy.ui.slideshow

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.data.settings.SlideshowPreferences
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Slideshow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

/**
 * Phase J.1 — fullscreen slideshow.
 *
 *   - Full-screen `HorizontalPager` at [PageSize.Fill] over the route's
 *     backing photo-id list.
 *   - Advances on a timer via a `LaunchedEffect(playing)` loop; default
 *     dwell is [DEFAULT_DWELL_MS] (4 s). Tap toggles `playing`; back-button
 *     exits.
 *   - Black background, no chrome. A small "Paused" badge fades in when
 *     `playing` is false (see [SlideshowPausedBadge]).
 *
 * J.2 (Ken Burns) + J.3 (overflow scope-picker integration on Photos /
 * FolderDetail / SmartAlbumDetail / Search) are deferred — the route is
 * registered here so siblings can wire the overflow entry once the
 * H-phase selection-mode work settles.
 */
@Composable
fun SlideshowScreen(
    destination: Slideshow,
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    val kenBurnsEnabled by scope.slideshowPreferences.observeKenBurnsEnabled()
        .collectAsStateWithLifecycle(initialValue = true)
    SlideshowContent(
        backingIds = destination.backingIds,
        photoSource = scope.photoSource,
        onBack = { scope.backStack.pop() },
        kenBurnsEnabled = kenBurnsEnabled,
        modifier = modifier,
    )
}

/**
 * Test-friendly inner composable — takes a [PhotoSource] directly so
 * Robolectric tests can mount it with a fake.
 */
@Composable
internal fun SlideshowContent(
    backingIds: List<Long>,
    photoSource: PhotoSource,
    onBack: () -> Unit,
    dwellMs: Long = DEFAULT_DWELL_MS,
    kenBurnsEnabled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(initialPage = 0) { backingIds.size }
    var playing by remember { mutableStateOf(true) }

    BackHandler(enabled = true) { onBack() }

    LaunchedEffect(playing, backingIds.size, dwellMs) {
        if (!playing || backingIds.size <= 1) return@LaunchedEffect
        while (playing) {
            delay(dwellMs)
            val next = SlideshowAdvanceMath.nextPage(
                currentPage = pagerState.currentPage,
                pageCount = backingIds.size,
            )
            pagerState.animateScrollToPage(next)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { playing = !playing })
            }
            .testTag(SLIDESHOW_PAGER_TAG),
    ) {
        if (backingIds.isEmpty()) {
            Text(
                text = stringResource(R.string.viewer_empty),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fill,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                SlideshowPage(
                    photoId = backingIds[page],
                    photoSource = photoSource,
                    kenBurnsEnabled = kenBurnsEnabled,
                    dwellMs = dwellMs,
                )
            }
        }

        SlideshowPausedBadge(
            visible = !playing,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .testTag(SLIDESHOW_PAUSED_BADGE_TAG),
        )
    }
}

@Composable
private fun SlideshowPage(
    photoId: Long,
    photoSource: PhotoSource,
    kenBurnsEnabled: Boolean,
    dwellMs: Long,
) {
    val flow = remember(photoId) { photoSource.observePhotoById(photoId) }
    val photo: Photo? by flow.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val kenBurns = if (kenBurnsEnabled) {
        rememberKenBurnsModifier(photoId = photoId, dwellMs = dwellMs)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("$SLIDESHOW_PAGE_TAG_PREFIX$photoId"),
        contentAlignment = Alignment.Center,
    ) {
        val model = photo?.contentUri
        if (model != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(true)
                    .memoryCacheKey("slideshow-$photoId")
                    .diskCacheKey("slideshow-$photoId")
                    .build(),
                contentDescription = photo?.displayName,
                contentScale = ContentScale.Fit,
                placeholder = ColorPainter(Color.Black),
                error = ColorPainter(Color.Black),
                modifier = Modifier.fillMaxSize().then(kenBurns),
            )
        }
    }
}

internal const val SLIDESHOW_PAGER_TAG = "slideshow_pager"
internal const val SLIDESHOW_PAGE_TAG_PREFIX = "slideshow_page_"
internal const val DEFAULT_DWELL_MS: Long = 4000L
