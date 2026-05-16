package com.eight87.shutterboy.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eight87.shutterboy.R
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
    PhotoViewerContent(
        backingIds = destination.backingIds,
        initialPhotoId = destination.photoIdValue,
        photoSource = scope.photoSource,
        photoDeleter = scope.photoDeleter,
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
    LaunchedEffect(chromeVisible) {
        if (chromeVisible) {
            delay(CHROME_AUTO_HIDE_MS)
            chromeVisible = false
        }
    }

    // G.1 + G.2 — gesture vocabulary: vertical drag below the pager's
    // claim threshold accumulates into one of two release actions
    // (dismiss / open-info). Horizontal drags route to the pager; pinch
    // (when wired) routes to transformable; both bypass the accumulator
    // because they don't produce vertical NestedScroll deltas.
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { ViewerGestureMath.DISMISS_THRESHOLD_DP.dp.toPx() }
    val infoOpenThresholdPx = with(density) { ViewerGestureMath.INFO_OPEN_THRESHOLD_DP.dp.toPx() }

    val dismissAccumulator = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
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
            applyDeletion(pending)
        }
    }

    // Current page's photo — reactive read, drives both the TopAppBar title
    // and the info-sheet body.
    val currentId: Long? = pagerIds.getOrNull(pagerState.currentPage)
    val currentPhotoFlow = remember(currentId) {
        if (currentId == null) flowOf(null) else photoSource.observePhotoById(currentId)
    }
    val currentPhoto: Photo? by currentPhotoFlow.collectAsState(initial = null)

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically { -it },
                exit = slideOutVertically { -it },
            ) {
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
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
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
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val pageId = pagerIds[page]
                    PhotoPage(
                        photoId = pageId,
                        photoSource = photoSource,
                        dismissThresholdPx = dismissThresholdPx,
                        infoOpenThresholdPx = infoOpenThresholdPx,
                        onSwipeUpForInfo = { infoVisible = true },
                        onSwipeDownDismiss = onBack,
                        onTap = { chromeVisible = !chromeVisible },
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
            title = { Text(stringResource(R.string.viewer_delete_dialog_title)) },
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
                                        if (req.deletedCount > 0) applyDeletion(id)
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

@Composable
private fun PhotoPage(
    photoId: Long,
    photoSource: PhotoSource,
    dismissThresholdPx: Float,
    infoOpenThresholdPx: Float,
    onSwipeUpForInfo: () -> Unit,
    onSwipeDownDismiss: () -> Unit,
    onTap: () -> Unit,
) {
    val flow = remember(photoId) { photoSource.observePhotoById(photoId) }
    val photo: Photo? by flow.collectAsState(initial = null)
    val context = LocalContext.current

    // G.2 — per-page vertical drag detector. Accumulates the vertical
    // delta; on release we classify it into swipe-up-info / swipe-down-
    // dismiss / no-op. detectVerticalDragGestures only claims events
    // with actual movement, so single-tap (chrome) and double-tap
    // (zoom) paths are untouched (G.2.3).
    val dragAccumulator = remember(photoId) { androidx.compose.runtime.mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // F.2 — single tap toggles chrome. Separate pointerInput so the
            // tap detector doesn't fight the vertical drag detector below.
            .pointerInput(photoId) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(photoId, dismissThresholdPx, infoOpenThresholdPx) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulator.floatValue = 0f },
                    onDragCancel = { dragAccumulator.floatValue = 0f },
                    onDragEnd = {
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
// F.2 — chrome auto-hide after 3 s of no chrome-toggle interaction.
private const val CHROME_AUTO_HIDE_MS: Long = 3000L
