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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.ui.multiselect.SelectionState
import com.eight87.shutterboy.ui.multiselect.isSelected
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val inSelectionMode = selectionState is SelectionState.Active
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
    var scrubbingThumb by remember { mutableStateOf(false) }
    // Only suppress decodes during THUMB SCRUB (where the grid intentionally
    // doesn't scroll and there's no point firing requests for in-between
    // positions). Normal fling scrolling lets requests fire as cells enter
    // view — each one is ~3 ms via MediaStore loadThumbnail, so tiles
    // fade in progressively instead of waiting for a big synchronized
    // batch when the scroll stops. That's what was causing the
    // "everything pops in at once at the end" feeling.
    val suppressDecode by remember {
        derivedStateOf { scrubbingThumb }
    }

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
    val targetPx = LocalThumbnailQuality.current.targetPx
    val prefetchScope = rememberCoroutineScope()
    val prefetcher = remember(targetPx) {
        ThumbnailPrefetcher(
            loader = SingletonImageLoader.get(context),
            maxPending = 500,
            workerCount = 16,
            scope = prefetchScope,
        )
    }
    LaunchedEffect(timeline, targetPx, prefetcher) {
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.isScrollInProgress
        }
            .distinctUntilChanged()
            .collect { (first, scrolling) ->
                if (scrolling) {
                    // Drop everything pending — when the user starts
                    // scrolling/scrubbing, the existing prefetch queue
                    // is by definition stale.
                    prefetcher.clear()
                    return@collect
                }
                val visibleCount = gridState.layoutInfo.visibleItemsInfo.size
                if (visibleCount <= 0) return@collect
                // 5 viewports behind + 5 ahead — wide enough that
                // continued scroll in either direction lands on
                // already-decoded thumbnails.
                val from = (first - visibleCount * 5).coerceAtLeast(0)
                val to = (first + visibleCount * 6).coerceAtMost(timeline.size)
                // Order matters with a LIFO worker pool: submitted-last
                // gets serviced first. So submit the FAR edges first
                // and the near-viewport items LAST — that puts the
                // most-likely-to-be-visible items on TOP of the stack.
                val center = first + visibleCount / 2
                val orderedIndices = (from until to).sortedByDescending {
                    Math.abs(it - center)
                }
                for (i in orderedIndices) {
                    val cell = timeline.getOrNull(i) as? TimelineDisplayItem.PhotoCell
                        ?: continue
                    val id = cell.photo.id.value
                    val req = ImageRequest.Builder(context)
                        .data(cell.photo.contentUri)
                        .size(Size(targetPx, targetPx))
                        .memoryCacheKey("thumb-$id-$targetPx")
                        .diskCacheKey("thumb-$id-$targetPx")
                        .build()
                    prefetcher.submit(req)
                }
            }
    }

    CompositionLocalProvider(LocalSuppressDecode provides suppressDecode) {
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
                                inSelectionMode = inSelectionMode,
                                onTap = { onPhotoTap(item.photo.id, backingIds) },
                                onLongPress = { onPhotoLongPress(item.photo.id) },
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
            onScrubbingChange = { scrubbingThumb = it },
        )
    }
    }
}

