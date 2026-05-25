package com.eight87.shutterboy.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import com.eight87.shutterboy.ui.nav.LocalAnimatedContentScopeOrNull
import com.eight87.shutterboy.ui.nav.LocalSharedTransitionScope
import com.eight87.shutterboy.ui.nav.photoSharedElementKey
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flip
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.automirrored.outlined.RotateLeft
import androidx.compose.material.icons.automirrored.outlined.RotateRight
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.CachePolicy
import coil3.request.crossfade
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.repo.FavoriteCommands
import com.eight87.shutterboy.data.repo.PhotoDeleter
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.domain.DeleteRequest
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
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
    val resolvedBackingIds = remember(destination.backingKey, destination.photoIdValue) {
        // Stash miss (cold restart, process death) falls back to a
        // one-element pager containing just the tapped photo so the
        // viewer still opens correctly without the swipe-neighbours.
        scope.graph.takeBackingIds(destination.backingKey)
            ?: listOf(destination.photoIdValue)
    }
    PhotoViewerContent(
        backingIds = resolvedBackingIds,
        initialPhotoId = destination.photoIdValue,
        photoSource = scope.photoSource,
        photoDeleter = scope.photoDeleter,
        favoriteCommands = scope.favoriteCommands,
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
    photoDeleter: PhotoDeleter? = null,
    favoriteCommands: FavoriteCommands? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // F.4 — backing-ids becomes a mutableStateList so a settled delete
    // removes the row in place; the pager's pageCount lambda reads
    // .size reactively, so the pager re-counts on mutation.
    val pagerIds = remember { mutableStateListOf<Long>().apply { addAll(backingIds) } }
    val initialPage = pagerIds.indexOf(initialPhotoId).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { pagerIds.size }

    var infoVisible by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    // F.2 — chrome toggle. Single tap on a page flips this; auto-hide
    // after CHROME_AUTO_HIDE_MS of no chrome-toggle interaction.
    var chromeVisible by remember { mutableStateOf(true) }

    // G.1 + G.2 — gesture vocabulary: vertical drag below the pager's
    // claim threshold accumulates into one of two release actions
    // (dismiss / open-info). Horizontal drags route to the pager; pinch
    // (when wired) routes to transformable; both bypass the accumulator
    // because they don't produce vertical NestedScroll deltas.
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { ViewerGestureMath.DISMISS_THRESHOLD_DP.dp.toPx() }
    val infoOpenThresholdPx = with(density) { ViewerGestureMath.INFO_OPEN_THRESHOLD_DP.dp.toPx() }

    val dismissAccumulator = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    // G.3.1 + G.3.2 — pinch + double-tap zoom + pan state. Shared across
    // pages so the LaunchedEffect on pagerState.currentPage can reset
    // scale + pan when the user swipes to a different photo.
    val scaleState = remember { androidx.compose.runtime.mutableFloatStateOf(ViewerZoomMath.MIN_SCALE) }
    val panState = remember { mutableStateOf(Offset.Zero) }
    var viewportWidth by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    var viewportHeight by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // Per-photo orientation: rotation in degrees + horizontal mirror.
    // Reset on page change (alongside scale + pan).
    val rotationState = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val mirroredState = remember { androidx.compose.runtime.mutableStateOf(false) }
    // Animated value used for the double-tap toggle so the transition
    // tweens rather than snaps. Pinch updates write to scaleState
    // directly (animation off the live finger position would feel laggy).
    val animatedScale by animateFloatAsState(
        targetValue = scaleState.floatValue,
        label = "viewer-zoom",
    )
    LaunchedEffect(pagerState.currentPage) {
        scaleState.floatValue = ViewerZoomMath.MIN_SCALE
        panState.value = Offset.Zero
        rotationState.floatValue = 0f
        mirroredState.value = false
    }
    val dismissConnection = remember(onBack, dismissThresholdPx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Only collect dominantly-vertical, post-scroll deltas — when
                // the pager has consumed a horizontal swipe, available.y is
                // ~0, which leaves our accumulator alone.
                if (available.y > 0f) {
                    dismissAccumulator.floatValue += available.y
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Drain accumulated downward over-scroll first when the
                // user reverses (drags back up).
                if (available.y < 0f && dismissAccumulator.floatValue > 0f) {
                    val drained = (-available.y).coerceAtMost(dismissAccumulator.floatValue)
                    dismissAccumulator.floatValue -= drained
                    return Offset(0f, -drained)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val travelled = dismissAccumulator.floatValue
                dismissAccumulator.floatValue = 0f
                if (ViewerGestureMath.exceedsDismissThreshold(travelled, dismissThresholdPx)) {
                    onBack()
                }
                return Velocity.Zero
            }
        }
    }

    // F.4 — backing-list mutator. Called both after a settled API 30+
    // system-consent intent (RESULT_OK) and after the API 26-28 direct
    // delete returns. Computes the next pager position via the pure
    // helper so the math has unit-test coverage.
    val applyDeletion: (Long) -> Unit = { deletedId ->
        val result = ViewerBackingListMath.removeId(
            list = pagerIds.toList(),
            deletedId = deletedId,
            currentPosition = pagerState.currentPage,
        )
        pagerIds.clear()
        pagerIds.addAll(result.newList)
        if (result.newPosition < 0) {
            onBack()
        } else {
            coroutineScope.launch { pagerState.scrollToPage(result.newPosition) }
        }
    }

    // F.4 — pending-delete-id holds the id whose system-consent intent we
    // launched on API 30+, so the result callback can apply the deletion
    // when the user confirms the system dialog.
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val pending = pendingDeleteId
        pendingDeleteId = null
        if (result.resultCode == Activity.RESULT_OK && pending != null) {
            // Aves-style instant removal — drop the row from Room
            // immediately. The MediaStore-observer rescan still fires
            // in the background but the user sees the deletion land in
            // one frame.
            val deleter = photoDeleter
            if (deleter != null) {
                coroutineScope.launch {
                    deleter.eagerlyRemoveFromCache(listOf(PhotoId(pending)))
                }
            }
            applyDeletion(pending)
        }
    }

    // Current page's photo — reactive read, drives both the TopAppBar title
    // and the info-sheet body.
    val currentId: Long? = pagerIds.getOrNull(pagerState.currentPage)
    val currentPhotoFlow = remember(currentId) {
        if (currentId == null) flowOf(null) else photoSource.observePhotoById(currentId)
    }
    val currentPhoto: Photo? by currentPhotoFlow.collectAsStateWithLifecycle(initialValue = null)
    val isCurrentVideo = currentPhoto?.mimeType?.startsWith("video/") == true

    // Auto-hide chrome after CHROME_AUTO_HIDE_MS — but ONLY for photo
    // pages. On a video page, taps go to Media3's controller (we
    // intentionally skip our own tap detector there), so the user has
    // no way to bring the chrome back. Keep share/favorite/etc visible
    // for the whole video playback.
    LaunchedEffect(chromeVisible, isCurrentVideo) {
        if (chromeVisible && !isCurrentVideo) {
            delay(CHROME_AUTO_HIDE_MS)
            chromeVisible = false
        }
    }

    // G.3 — observe favorite-state for the current photo id. Falls back to
    // a no-op flow (false) when favorite commands aren't wired (test mounts).
    val isFavoriteFlow = remember(currentId, favoriteCommands) {
        if (currentId == null || favoriteCommands == null) flowOf(false)
        else favoriteCommands.observeIsFavorite(PhotoId(currentId))
    }
    val isFavorite: Boolean by isFavoriteFlow.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
            ) {
                TopAppBar(
                    title = {
                        // Single-line title, horizontally scrollable when
                        // the filename is too long for the row.
                        val titleScroll = rememberScrollState()
                        Text(
                            text = currentPhoto?.displayName
                                ?: stringResource(R.string.viewer_loading_title),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            softWrap = false,
                            color = Color.White,
                            modifier = Modifier
                                .horizontalScroll(titleScroll),
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
                        // F.5 — Share via Intent.ACTION_SEND + chooser.
                        IconButton(
                            onClick = {
                                currentPhoto?.let { photo ->
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = photo.mimeType
                                        putExtra(Intent.EXTRA_STREAM, photo.contentUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            share,
                                            context.getString(R.string.viewer_share_chooser_title),
                                        ),
                                    )
                                }
                            },
                            enabled = currentPhoto != null,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = stringResource(R.string.cd_viewer_share),
                            )
                        }
                        // F.6 — Edit handoff via Intent.ACTION_EDIT chooser.
                        IconButton(
                            onClick = {
                                currentPhoto?.let { photo ->
                                    val edit = Intent(Intent.ACTION_EDIT).apply {
                                        setDataAndType(photo.contentUri, photo.mimeType)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            edit,
                                            context.getString(R.string.viewer_edit_chooser_title),
                                        ),
                                    )
                                }
                            },
                            enabled = currentPhoto != null,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.cd_viewer_edit),
                            )
                        }
                        // G.3 — Favorite toggle. Sits between Edit and Delete.
                        IconButton(
                            onClick = {
                                val id = currentId
                                val fav = favoriteCommands
                                if (id != null && fav != null) {
                                    coroutineScope.launch { fav.toggleFavorite(PhotoId(id)) }
                                }
                            },
                            enabled = currentPhoto != null && favoriteCommands != null,
                        ) {
                            if (isFavorite) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription =
                                        stringResource(R.string.cd_viewer_favorite),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.FavoriteBorder,
                                    contentDescription =
                                        stringResource(R.string.cd_viewer_unfavorite),
                                )
                            }
                        }
                        // F.4 — Delete via MediaStore consent (API 30+) /
                        // direct delete (API 26-28). Confirm dialog first.
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = currentPhoto != null && photoDeleter != null,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.cd_viewer_delete),
                            )
                        }
                        // Rotate left / right / reset + horizontal flip.
                        // Mutates the per-page transform state owned by
                        // the viewer; resets when swiping to a new photo.
                        IconButton(
                            onClick = {
                                rotationState.floatValue =
                                    (rotationState.floatValue - 90f) % 360f
                            },
                            enabled = currentPhoto != null,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.RotateLeft,
                                contentDescription = stringResource(R.string.cd_viewer_rotate_left),
                            )
                        }
                        IconButton(
                            onClick = {
                                rotationState.floatValue = 0f
                                mirroredState.value = false
                            },
                            enabled = currentPhoto != null,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restore,
                                contentDescription = stringResource(R.string.cd_viewer_rotate_reset),
                            )
                        }
                        IconButton(
                            onClick = {
                                rotationState.floatValue =
                                    (rotationState.floatValue + 90f) % 360f
                            },
                            enabled = currentPhoto != null,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.RotateRight,
                                contentDescription = stringResource(R.string.cd_viewer_rotate_right),
                            )
                        }
                        IconButton(
                            onClick = { mirroredState.value = !mirroredState.value },
                            enabled = currentPhoto != null,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Flip,
                                contentDescription = stringResource(R.string.cd_viewer_flip),
                            )
                        }
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
                        // Translucent scrim over the black viewer
                        // background so the photo shows through
                        // slightly. White content for contrast.
                        containerColor = Color.Black.copy(alpha = 0.4f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White,
                    ),
                )
            }
        },
        bottomBar = {
            // Filmstrip + page counter. Hidden when the chrome is hidden
            // OR when the current photo is zoomed in (per user request:
            // "unless I zoom in on the photo, that should vanish").
            val zoomed = ViewerZoomMath.isZoomed(scaleState.floatValue)
            AnimatedVisibility(
                visible = chromeVisible && !zoomed,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                FilmstripBar(
                    pagerIds = pagerIds,
                    currentPage = pagerState.currentPage,
                    onJumpTo = { idx ->
                        coroutineScope.launch { pagerState.scrollToPage(idx) }
                    },
                )
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
                .onSizeChanged { size ->
                    viewportWidth = size.width.toFloat()
                    viewportHeight = size.height.toFloat()
                }
                .nestedScroll(dismissConnection)
                .testTag(VIEWER_PAGER_TAG),
        ) {
            if (pagerIds.isEmpty()) {
                Text(
                    text = stringResource(R.string.viewer_empty),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    pageSize = PageSize.Fill,
                    // G.3 — pager swipe disabled while zoomed in so a
                    // single-finger pan past the photo edge doesn't
                    // advance the page mid-zoom.
                    userScrollEnabled = !ViewerZoomMath.isZoomed(scaleState.floatValue),
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val pageId = pagerIds[page]
                    val isCurrentPage = page == pagerState.currentPage
                    PhotoPage(
                        photoId = pageId,
                        photoSource = photoSource,
                        isCurrentPage = isCurrentPage,
                        dismissThresholdPx = dismissThresholdPx,
                        infoOpenThresholdPx = infoOpenThresholdPx,
                        onSwipeUpForInfo = { infoVisible = true },
                        onSwipeDownDismiss = onBack,
                        onTap = { chromeVisible = !chromeVisible },
                        // G.3.1 / G.3.2 — only the visible page gets the
                        // live zoom + pan state. Off-screen pages render
                        // at rest so paging in/out doesn't carry zoom.
                        scale = if (isCurrentPage) animatedScale else ViewerZoomMath.MIN_SCALE,
                        pan = if (isCurrentPage) panState.value else Offset.Zero,
                        rotationDegrees = if (isCurrentPage) rotationState.floatValue else 0f,
                        mirroredX = if (isCurrentPage) mirroredState.value else false,
                        onPinch = { zoomChange, panChange ->
                            val newScale = ViewerZoomMath.clampScale(
                                scaleState.floatValue * zoomChange,
                            )
                            scaleState.floatValue = newScale
                            panState.value = if (ViewerZoomMath.isZoomed(newScale)) {
                                val raw = panState.value + panChange
                                val (cx, cy) = ViewerZoomMath.clampPan(
                                    raw.x, raw.y, newScale,
                                    viewportWidth, viewportHeight,
                                )
                                Offset(cx, cy)
                            } else {
                                Offset.Zero
                            }
                        },
                        onDoubleTap = {
                            scaleState.floatValue =
                                ViewerZoomMath.toggledScale(scaleState.floatValue)
                            if (!ViewerZoomMath.isZoomed(scaleState.floatValue)) {
                                panState.value = Offset.Zero
                            }
                        },
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

    // F.4 — single-photo confirm dialog. Bulk delete (H.3) uses a
    // typed-confirm dialog with a TextField; single-photo delete relies
    // on the system MediaStore consent dialog for the second tap and so
    // only needs a simple Cancel / Delete confirm here.
    if (showDeleteConfirm) {
        val targetId = currentId
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    stringResource(
                        if (isCurrentVideo) R.string.viewer_delete_dialog_title_video
                        else R.string.viewer_delete_dialog_title
                    )
                )
            },
            text = { Text(stringResource(R.string.viewer_delete_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        val id = targetId
                        val deleter = photoDeleter
                        if (id != null && deleter != null) {
                            coroutineScope.launch {
                                when (val req = deleter.deletePhotos(listOf(PhotoId(id)))) {
                                    is DeleteRequest.Consent -> {
                                        pendingDeleteId = id
                                        val isr = IntentSenderRequest
                                            .Builder(req.intentSender.intentSender)
                                            .build()
                                        deleteConsentLauncher.launch(isr)
                                    }
                                    is DeleteRequest.Immediate -> {
                                        if (req.deletedCount > 0) {
                                            deleter.eagerlyRemoveFromCache(listOf(PhotoId(id)))
                                            applyDeletion(id)
                                        }
                                    }
                                    is DeleteRequest.Failure -> Unit
                                }
                            }
                        }
                    },
                ) { Text(stringResource(R.string.viewer_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.viewer_delete_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PhotoPage(
    photoId: Long,
    photoSource: PhotoSource,
    isCurrentPage: Boolean,
    dismissThresholdPx: Float,
    infoOpenThresholdPx: Float,
    onSwipeUpForInfo: () -> Unit,
    onSwipeDownDismiss: () -> Unit,
    onTap: () -> Unit,
    scale: Float,
    pan: Offset,
    onPinch: (zoomChange: Float, panChange: Offset) -> Unit,
    onDoubleTap: () -> Unit,
    rotationDegrees: Float = 0f,
    mirroredX: Boolean = false,
) {
    val flow = remember(photoId) { photoSource.observePhotoById(photoId) }
    val photo: Photo? by flow.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val isVideo = photo?.mimeType?.startsWith("video/") == true
    // Same px the grid uses for its thumb cache key — so the viewer
    // can use that bitmap as its placeholder via placeholderMemoryCacheKey.
    // Match the same key the photos grid produces for the default
    // Items level (4 columns), so the viewer hits the in-memory
    // thumbnail when the user opened from there.
    val thumbPlaceholderPx =
        com.eight87.shutterboy.ui.photos.grid.rememberGridTargetPx(columns = 4)

    // G.2 — per-page vertical drag detector. Accumulates the vertical
    // delta; on release we classify it into swipe-up-info / swipe-down-
    // dismiss / no-op. detectVerticalDragGestures only claims events
    // with actual movement, so single-tap (chrome) and double-tap
    // (zoom) paths are untouched (G.2.3).
    val dragAccumulator = remember(photoId) { androidx.compose.runtime.mutableFloatStateOf(0f) }

    // G.3.1 + G.3.2 — pinch + pan via transformable. The lambda forwards
    // zoom + pan changes to the parent so a single scale/pan state can
    // be reset on page change. transformable claims pointer events as
    // soon as 2+ fingers are down, so single-finger drag still routes
    // to the vertical-drag detector below.
    val transformableState = rememberTransformableState { panChange, zoomChange, _, _ ->
        onPinch(zoomChange, panChange)
    }
    val zoomed = ViewerZoomMath.isZoomed(scale)

    // R.F.22 — wrap `zoomed` / `isVideo` in a State<T> so the vertical-drag
    // detector reads the current values via `.value` inside its suspend
    // block, instead of capturing them at install time. Without this the
    // outer `pointerInput(zoomed, isVideo)` reinstalled the detector every
    // time `zoomed` crossed the rest threshold (every pinch frame). The
    // detector now installs once per photoId and reads gates lazily.
    val zoomedState = androidx.compose.runtime.rememberUpdatedState(zoomed)
    val isVideoState = androidx.compose.runtime.rememberUpdatedState(isVideo)

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Video pages skip the zoom + tap gesture stack — pinch/zoom
            // math doesn't apply to a PlayerView, and the tap detector
            // would swallow Media3's seek-bar / play-pause taps. The
            // vertical-drag dismiss/info gestures still apply (swipe-down
            // to close, swipe-up to open info), but we relax the `zoomed`
            // gate since video has no zoom state.
            .then(
                if (isVideo) Modifier else Modifier
                    // G.3.1 — outermost: transformable claims pinch (2+ fingers).
                    // Single-finger drags fall through to the gesture detectors
                    // below via the canPan = { zoomed } gate — without it,
                    // transformable claims single-finger pans too and swallows
                    // both the HorizontalPager swipe and our vertical-drag
                    // dismiss/info gestures. Once zoomed, single-finger pan is
                    // wanted (the user is moving around inside the zoomed image).
                    .transformable(
                        state = transformableState,
                        canPan = { zoomed },
                    )
                    // F.2 + G.3.2 — single tap toggles chrome, double tap toggles
                    // zoom. Separate pointerInput so the tap detector doesn't
                    // fight the vertical drag detector below.
                    .pointerInput(photoId) {
                        detectTapGestures(
                            onTap = { onTap() },
                            onDoubleTap = { onDoubleTap() },
                        )
                    },
            )
            .pointerInput(photoId, dismissThresholdPx, infoOpenThresholdPx) {
                // R.F.22 — keys stable per photo + thresholds only. `zoomed`
                // and `isVideo` are read via rememberUpdatedState refs inside
                // the drag handlers, so the detector installs once and stays
                // installed across the entire zoom session.
                detectVerticalDragGestures(
                    onDragStart = {
                        // Gate at gesture-start: if currently zoomed (and not
                        // video), drop the drag — pinch-pan owns the gesture.
                        if (zoomedState.value && !isVideoState.value) return@detectVerticalDragGestures
                        dragAccumulator.floatValue = 0f
                    },
                    onDragCancel = { dragAccumulator.floatValue = 0f },
                    onDragEnd = {
                        if (zoomedState.value && !isVideoState.value) {
                            dragAccumulator.floatValue = 0f
                            return@detectVerticalDragGestures
                        }
                        val travel = dragAccumulator.floatValue
                        dragAccumulator.floatValue = 0f
                        when (ViewerGestureMath.classifyRelease(
                            dragPx = travel,
                            dismissThresholdPx = dismissThresholdPx,
                            infoOpenThresholdPx = infoOpenThresholdPx,
                        )) {
                            ViewerGestureMath.ReleaseAction.OpenInfo -> onSwipeUpForInfo()
                            ViewerGestureMath.ReleaseAction.Dismiss -> onSwipeDownDismiss()
                            ViewerGestureMath.ReleaseAction.None -> Unit
                        }
                    },
                ) { _, dragAmount ->
                    dragAccumulator.floatValue += dragAmount
                }
            }
            .testTag("$VIEWER_PAGE_TAG_PREFIX$photoId"),
        contentAlignment = Alignment.Center,
    ) {
        val model = photo?.contentUri
        if (model != null && isVideo) {
            VideoPlayerSurface(
                contentUri = model,
                isActive = isCurrentPage,
                modifier = Modifier.fillMaxSize(),
            )
        } else if (model != null) {
            // Lean pass: dropped the grid → viewer shared-element open
            // animation. Coil's crossfade still gives a brief fade-in,
            // and the pager + chrome are now usable immediately on
            // open (Nav3's AnimatedContent no longer gates pointer
            // routing on a multi-hundred-ms bounds transform).
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(model)
                    .crossfade(true)
                    // Viewer images are huge (~10 MB decoded). Letting
                    // them fill Coil's shared memory cache LRU-evicts
                    // grid thumbnails — so when the user backs out, the
                    // tiles have to re-decode and flash from grey. Skip
                    // memory cache here; disk cache keeps pager swipes
                    // fast.
                    .memoryCachePolicy(CachePolicy.DISABLED)
                    .memoryCacheKey("viewer-$photoId")
                    .diskCacheKey("viewer-$photoId")
                    // While the full-res image is decoding, use the
                    // grid thumbnail as placeholder — provides instant
                    // visual continuity for the open/close transition
                    // without paying for a separate placeholder load.
                    .placeholderMemoryCacheKey("thumb-$photoId-$thumbPlaceholderPx")
                    .build(),
                contentDescription = photo?.displayName,
                contentScale = ContentScale.Fit,
                placeholder = ColorPainter(Color.Black),
                error = ColorPainter(Color.Black),
                modifier = Modifier
                    .fillMaxSize()
                    // G.3.1 — graphicsLayer (not Modifier.scale) so the
                    // State reads happen at draw time, skipping composition
                    // on every pinch frame. Clamp / pan-bounds-math is a
                    // follow-up; for now we let the image overshoot.
                    .graphicsLayer {
                        scaleX = if (mirroredX) -scale else scale
                        scaleY = scale
                        translationX = pan.x
                        translationY = pan.y
                        rotationZ = rotationDegrees
                    }
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
// F.2 — chrome auto-hide after 3 s of no chrome-toggle interaction.
private const val CHROME_AUTO_HIDE_MS: Long = 3000L

/**
 * Viewer bottom chrome — filmstrip of [FILMSTRIP_RADIUS] neighbours on
 * each side of the current pager page + a "N / total" counter beneath.
 * Tapping a neighbour jumps the pager there. Renders nothing for
 * indices out of range so the current tile stays centred (the user
 * always knows where they are even at the start / end of the list).
 */
private const val FILMSTRIP_RADIUS = 3

@Composable
private fun FilmstripBar(
    pagerIds: List<Long>,
    currentPage: Int,
    onJumpTo: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        ) {
            for (offset in -FILMSTRIP_RADIUS..FILMSTRIP_RADIUS) {
                val idx = currentPage + offset
                val isCurrent = offset == 0
                FilmstripTile(
                    photoId = pagerIds.getOrNull(idx),
                    isCurrent = isCurrent,
                    onClick = {
                        if (idx in pagerIds.indices && !isCurrent) onJumpTo(idx)
                    },
                )
            }
        }
        Spacer(Modifier.size(4.dp))
        Text(
            text = "${currentPage + 1} / ${pagerIds.size}",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun FilmstripTile(
    photoId: Long?,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val tileSize = if (isCurrent) 56.dp else 44.dp
    Box(
        modifier = Modifier
            .width(tileSize)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(enabled = photoId != null && !isCurrent, onClick = onClick),
    ) {
        if (photoId != null) {
            // Use the same thumb cache key the grid + prefetcher
            // populate so this is a synchronous memory-cache hit when
            // the neighbours are already warm.
            val context = androidx.compose.ui.platform.LocalContext.current
            val targetPx = with(LocalDensity.current) { tileSize.roundToPx() }
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(
                        android.content.ContentUris.withAppendedId(
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            photoId,
                        ),
                    )
                    .size(coil3.size.Size(targetPx, targetPx))
                    .precision(coil3.size.Precision.INEXACT)
                    .memoryCacheKey("thumb-$photoId-$targetPx")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (isCurrent) {
            // Outlined ring around the current tile so the user knows
            // at a glance which photo they're on.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = 2.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(4.dp),
                    ),
            )
        }
    }
}
