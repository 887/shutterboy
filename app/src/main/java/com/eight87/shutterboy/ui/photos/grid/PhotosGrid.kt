package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.ui.multiselect.SelectionState
import com.eight87.shutterboy.ui.multiselect.isSelected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.withContext

/**
 * Phase C.2 + C.3 + C.4 + C.5 + D.3.5 — Photos timeline body.
 * `LazyVerticalGrid` reading from a strategy-supplied [PhotoStream] (R.D
 * engine-and-strategy locked — Photos / FolderDetail / SmartAlbumDetail
 * all delegate). Density-zoom (C.3) drives column count + per-cell shape
 * via [PhotosZoomLevel]; pinch-to-cycle is wired by the caller so this
 * composable stays declarative.
 *
 * Inline month-year bands at [PhotosZoomLevel.Items] (Aves pattern,
 * scroll-with-content, no overlay-pin — locked in C.2). Year bands at
 * `.Days` and `.Months`. No bands at `.Years` (each year is a hero tile).
 *
 * Overlays composed in the same `Box`:
 *   - [YearScrubber] — right-edge floating pills (year markers + a
 *     current-scroll bubble), reveal-on-scroll, touch-through outside
 *     the pills themselves (C.4 + C.5 merged).
 *
 * Tap on a thumbnail / cover-tile fires [onPhotoTap] with the tapped id +
 * the backing id list (every Photo currently in the grid, in display
 * order) so the receiving viewer route can drive a `HorizontalPager`
 * (Phase F lands the actual viewer; C.6 wires the placeholder route).
 */
@Composable
internal fun PhotosGrid(
    stream: PhotoStream,
    level: PhotosZoomLevel,
    onPhotoTap: (PhotoId, List<Long>) -> Unit,
    modifier: Modifier = Modifier,
    emptyState: (@Composable (Modifier) -> Unit)? = null,
    selectionState: SelectionState = SelectionState.Idle,
    onPhotoLongPress: (PhotoId) -> Unit = {},
) {
    val photos by stream.observe()
        .collectAsStateWithLifecycle(initialValue = null)

    val loaded = photos
    if (loaded == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }
    if (loaded.isEmpty()) {
        if (emptyState != null) emptyState(modifier) else EmptyPhotosState(modifier = modifier)
        return
    }

    val locale = LocalConfiguration.current.locales.get(0) ?: java.util.Locale.getDefault()
    // Timeline assembly off the main thread. Critically, we KEEP the
    // previous timeline visible while a new one builds — resetting to
    // empty on every photo emission would unmount the grid mid-scroll
    // and freeze the UI while the user is interacting.
    var timeline by remember { mutableStateOf<List<TimelineDisplayItem>>(emptyList()) }
    var backingIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    LaunchedEffect(loaded, level) {
        val newTimeline = withContext(Dispatchers.Default) { buildTimeline(loaded, level) }
        val newIds = withContext(Dispatchers.Default) { loaded.map { it.id.value } }
        timeline = newTimeline
        backingIds = newIds
    }
    val markers = remember(timeline) { extractYearMarkers(timeline) }
    val gridState = rememberLazyGridState()

    // Stable cell callbacks. The lambdas below are remembered ONCE for
    // the lifetime of PhotosGrid; they read the current onPhotoTap /
    // onPhotoLongPress / backingIds through rememberUpdatedState refs
    // so they never need to be recreated on grid recomposition. Every
    // cell gets the same lambda identity → no forced per-cell
    // recomposition when the grid recomposes for unrelated reasons
    // (scroll, selection toggle, etc).
    val onPhotoTapRef = rememberUpdatedState(onPhotoTap)
    val onPhotoLongPressRef = rememberUpdatedState(onPhotoLongPress)
    val backingIdsRef = rememberUpdatedState(backingIds)
    val stableOnTap: (com.eight87.shutterboy.domain.PhotoId) -> Unit =
        remember { { id -> onPhotoTapRef.value(id, backingIdsRef.value) } }
    val stableOnLongPress: (com.eight87.shutterboy.domain.PhotoId) -> Unit =
        remember { { id -> onPhotoLongPressRef.value(id) } }
    if (timeline.isEmpty()) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    // Aves-style prefetch: bounded LIFO queue (max ~3 viewports) with
    // 4 IO workers stealing the freshest submissions first. The queue
    // is cleared every time scrolling starts so a long fling doesn't
    // bury fresh visible positions under stale earlier ones. Per-id
    // dedupe via memory-cache hit check inside the prefetcher.
    val context = LocalContext.current
    val targetPx = rememberGridTargetPx(level.columns)
    val lowPx = rememberGridTargetPx(level.columns, multiplier = 0.5f)
    val prefetchScope = rememberCoroutineScope()
    val prefetcher = remember(targetPx) {
        ThumbnailPrefetcher(
            context = context,
            loader = SingletonImageLoader.get(context),
            targetPx = targetPx,
            maxPending = 1500,
            workerCount = 8,
            scope = prefetchScope,
        )
    }
    @OptIn(FlowPreview::class)
    LaunchedEffect(timeline, targetPx, prefetcher) {
        // Adaptive, direction-aware prefetch scheduler. The compose
        // snapshot read happens on whatever thread `collect` is
        // running on (the LaunchedEffect's main-coroutine context),
        // but all the math + set-building + queue submits hop to
        // Dispatchers.Default below so the main thread is only on the
        // hook for the cheap state read, not the scheduling itself.
        //
        // Tracking:
        //   - `lastIndex` / `lastTimeNs`: previous viewport sample.
        //   - `emaVelocity`: items-per-second, signed. Positive = user
        //     is scrolling toward larger indices (down/right). EMA
        //     smoothing damps the per-sample noise without lagging
        //     direction reversals more than ~2 frames.
        //
        // Window shape:
        //   - Asymmetric around the leading edge — the edge in the
        //     direction we're moving — not around the center of the
        //     viewport. When scrolling down, "ahead" means greater
        //     indices; we extend the window further past the bottom
        //     of the screen than above it, because that's where the
        //     eye actually goes.
        //   - Adaptive: the ahead-half of the window scales with
        //     |velocity|. Slow scroll = tight window (don't pollute
        //     the cache). Fast fling = wide window (look further
        //     ahead so tiles are ready when they enter view).
        //
        // Cancellation:
        //   - On every sample we build the keep-set (every id in the
        //     current far window) and call `prefetcher.retain`. Tasks
        //     pending or in-flight for ids outside the set are
        //     dropped + their CancellationSignal fired. The bounce-
        //     back case (reverse direction mid-fling) is the main
        //     beneficiary — we stop loading way-behind tiles instead
        //     of letting them complete into the cache.
        var lastIndex = -1
        var lastTimeNs = 0L
        var emaVelocity = 0f

        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.layoutInfo.visibleItemsInfo.size
        }
            .distinctUntilChanged()
            // Coalesce mid-fling pulses. A hard fling fires ~30
            // viewport changes per second; without sampling we'd
            // schedule (build sets, retain, submit) 30 times in 1s
            // and the prefetcher would spend most of its budget
            // cancelling + re-submitting tiles it had already
            // started decoding. 100 ms gives the workers a chance
            // to actually finish what we asked for, while still
            // updating frequently enough during drift that the
            // window follows the user.
            // Aggressive sampling (40 ms) so direction reverses
            // reprioritize within ~2 frames instead of ~6. Per-sample
            // work is cheap (cancel + submit through cancellable
            // workers) so we can afford the higher rate.
            .sample(40L)
            .collect { (first, visibleCount) ->
                if (visibleCount <= 0) return@collect
                val now = System.nanoTime()
                val instant = if (lastIndex >= 0 && lastTimeNs > 0L) {
                    val dt = (now - lastTimeNs) / 1_000_000_000f
                    if (dt > 0.0001f) (first - lastIndex) / dt else 0f
                } else 0f
                // Light smoothing only — bias toward the instant
                // reading so direction reverses are detected on the
                // next sample, not several samples in.
                emaVelocity = emaVelocity * 0.3f + instant * 0.7f
                lastIndex = first
                lastTimeNs = now

                val velocity = emaVelocity
                val timelineSnapshot = timeline
                withContext(Dispatchers.Default) {
                    val dir = when {
                        velocity > 3f -> 1
                        velocity < -3f -> -1
                        else -> 0
                    }
                    val absV = kotlin.math.abs(velocity)
                    // Single-tier target prefetch. Window is small,
                    // asymmetric around the leading edge, and scales
                    // modestly with velocity so a fling decelerates
                    // onto warmed tiles without us hammering the OS
                    // thumbnail pipe with deep look-ahead that the
                    // visible path is going to need first.
                    val speedScale = (absV / 40f).coerceIn(0f, 2f)
                    val ahead = (visibleCount * (3f + 3f * speedScale)).toInt()
                    val behind = visibleCount * 2

                    val (lo, hi) = when (dir) {
                        1 -> (first - behind) to (first + ahead)
                        -1 -> (first - ahead) to (first + behind)
                        else -> (first - visibleCount * 3) to (first + visibleCount * 3)
                    }
                    val from = lo.coerceAtLeast(0)
                    val to = hi.coerceAtMost(timelineSnapshot.size)

                    // Leading edge = where new tiles enter view.
                    val leadingEdge = when (dir) {
                        1 -> first + visibleCount
                        -1 -> first
                        else -> first + visibleCount / 2
                    }

                    // Symmetric keep-zone, larger than the prefetch
                    // window so short direction reversals don't kill
                    // recently-warmed tiles. Only items more than ~20
                    // viewports away get cancelled.
                    val keepHalf = visibleCount * 20
                    val keepFrom = (first - keepHalf).coerceAtLeast(0)
                    val keepTo = (first + keepHalf).coerceAtMost(timelineSnapshot.size)
                    val keepIds = HashSet<Long>(keepTo - keepFrom)
                    for (i in keepFrom until keepTo) {
                        val cell = timelineSnapshot.getOrNull(i)
                            as? TimelineDisplayItem.PhotoCell ?: continue
                        keepIds += cell.photo.id.value
                    }
                    prefetcher.retain(keepIds)

                    // Submit farthest from leading edge first so items
                    // just past the leading edge end up on TOP of the
                    // LIFO and are decoded next.
                    val indices = (from until to).sortedByDescending {
                        kotlin.math.abs(it - leadingEdge)
                    }
                    for (i in indices) {
                        val cell = timelineSnapshot.getOrNull(i)
                            as? TimelineDisplayItem.PhotoCell ?: continue
                        prefetcher.submit(cell.photo.id.value, cell.photo.contentUri)
                    }
                }
            }
    }

    CompositionLocalProvider(
        LocalPrefetcher provides prefetcher,
        LocalGridTargetPx provides targetPx,
        LocalGridLowPx provides lowPx,
    ) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(level.columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            timeline.forEach { item ->
                when (item) {
                    is TimelineDisplayItem.MonthYearBand ->
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "band-month-${item.yearMonth}",
                            contentType = "band_month_year",
                        ) { MonthYearBand(yearMonth = item.yearMonth) }

                    is TimelineDisplayItem.YearBand ->
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "band-year-${item.year}",
                            contentType = "band_year",
                        ) { YearBand(year = item.year) }

                    is TimelineDisplayItem.PhotoCell ->
                        item(
                            key = "photo-${item.photo.id.value}",
                            contentType = "photo_thumbnail",
                        ) {
                            PhotoThumbnail(
                                photo = item.photo,
                                selected = selectionState.isSelected(item.photo.id),
                                onTap = stableOnTap,
                                onLongPress = stableOnLongPress,
                            )
                        }

                    is TimelineDisplayItem.DayCell ->
                        item(
                            key = "day-${item.date}",
                            contentType = "tile_day",
                        ) {
                            CoverTile(
                                cover = item.cover,
                                label = formatDayTile(item.date, locale),
                                photoCount = item.photoCount,
                                onClick = { onPhotoTap(item.cover.id, backingIds) },
                            )
                        }

                    is TimelineDisplayItem.MonthCell ->
                        item(
                            key = "month-${item.yearMonth}",
                            contentType = "tile_month",
                        ) {
                            CoverTile(
                                cover = item.cover,
                                label = formatMonthTile(item.yearMonth, locale),
                                photoCount = item.photoCount,
                                onClick = { onPhotoTap(item.cover.id, backingIds) },
                            )
                        }

                    is TimelineDisplayItem.YearCell ->
                        item(
                            span = { GridItemSpan(maxLineSpan) },
                            key = "year-${item.year}",
                            contentType = "tile_year",
                        ) {
                            CoverTile(
                                cover = item.cover,
                                label = formatYearBand(item.year),
                                photoCount = item.photoCount,
                                aspectRatio = 16f / 9f,
                                onClick = { onPhotoTap(item.cover.id, backingIds) },
                            )
                        }
                }
            }
        }

        YearScrubber(
            markers = markers,
            timeline = timeline,
            level = level,
            gridState = gridState,
            modifier = Modifier.align(Alignment.CenterEnd),
            onScrubbingChange = { /* no-op; suppress-decode dropped in the lean pass */ },
        )
    }
    }
}

