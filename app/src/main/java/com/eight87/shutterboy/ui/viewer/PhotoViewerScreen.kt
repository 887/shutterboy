package com.eight87.shutterboy.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.ui.nav.PhotoViewer
import com.eight87.shutterboy.ui.nav.RouteScope
import kotlinx.coroutines.flow.flowOf
import androidx.compose.ui.platform.LocalContext

/**
 * Phase F.1 + F.3 — fullscreen photo viewer.
 *
 *   - `HorizontalPager` ranges over the backing photo-id list (passed via
 *     the route destination), one Coil 3 [AsyncImage] per page at
 *     [PageSize.Fill].
 *   - Each page subscribes to [PhotoSource.observePhotoById] for its row,
 *     so a delete / favourite-toggle on the underlying photo refreshes
 *     without re-pushing the route.
 *   - The TopAppBar shows the current photo's display name plus a back
 *     arrow that pops to the source surface (smart album / folder /
 *     timeline) — single back stack model, the caller pushed us, pop
 *     returns there.
 *   - Info action toggles an EXIF bottom-sheet keyed against the current
 *     page (`F.3`).
 *
 * Phase F sub-steps F.2 (chrome actions), F.4 (delete), F.5 (share),
 * F.6 (edit handoff), F.7 (shared-element transition), F.8 (extra
 * Robolectric coverage of delete branching) land in follow-up commits;
 * the route shape stays stable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    destination: PhotoViewer,
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    PhotoViewerContent(
        backingIds = destination.backingIds,
        initialPhotoId = destination.photoIdValue,
        photoSource = scope.photoSource,
        onBack = { scope.backStack.pop() },
        modifier = modifier,
    )
}

/**
 * Test-friendly inner composable — takes a [PhotoSource] directly so unit
 * tests can mount it with a fake. Visible for use by `PhotoViewerScreenSmokeTest`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PhotoViewerContent(
    backingIds: List<Long>,
    initialPhotoId: Long,
    photoSource: PhotoSource,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialPage = backingIds.indexOf(initialPhotoId).coerceAtLeast(0)
    val pageCount = backingIds.size.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    var infoVisible by remember { mutableStateOf(false) }

    // Current page's photo — reactive read, drives both the TopAppBar title
    // and the info-sheet body.
    val currentId: Long? = backingIds.getOrNull(pagerState.currentPage)
    val currentPhotoFlow = remember(currentId) {
        if (currentId == null) flowOf(null) else photoSource.observePhotoById(currentId)
    }
    val currentPhoto: Photo? by currentPhotoFlow.collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentPhoto?.displayName
                            ?: stringResource(R.string.viewer_loading_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_viewer_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { infoVisible = true },
                        enabled = currentPhoto != null,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = stringResource(R.string.cd_viewer_info),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
                .testTag(VIEWER_PAGER_TAG),
        ) {
            if (pageCount == 0) {
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
                    val pageId = backingIds[page]
                    PhotoPage(
                        photoId = pageId,
                        photoSource = photoSource,
                    )
                }
            }
        }
    }

    if (infoVisible) {
        currentPhoto?.let { photo ->
            ExifInfoPanel(
                photo = photo,
                onDismiss = { infoVisible = false },
            )
        }
    }
}

@Composable
private fun PhotoPage(
    photoId: Long,
    photoSource: PhotoSource,
) {
    val flow = remember(photoId) { photoSource.observePhotoById(photoId) }
    val photo: Photo? by flow.collectAsState(initial = null)
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("$VIEWER_PAGE_TAG_PREFIX$photoId"),
        contentAlignment = Alignment.Center,
    ) {
        val model = photo?.contentUri
        if (model != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(true)
                    .memoryCacheKey("viewer-$photoId")
                    .diskCacheKey("viewer-$photoId")
                    .build(),
                contentDescription = photo?.displayName,
                contentScale = ContentScale.Fit,
                placeholder = ColorPainter(Color.Black),
                error = ColorPainter(Color.Black),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
            )
        } else {
            // Placeholder background while the row resolves; Coil per-image
            // cache reuses the bitmap on swipe-back so this only ever
            // flashes once per cold mount.
            Text(
                text = "",
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

internal const val VIEWER_PAGER_TAG = "viewer_pager"
internal const val VIEWER_PAGE_TAG_PREFIX = "viewer_page_"
