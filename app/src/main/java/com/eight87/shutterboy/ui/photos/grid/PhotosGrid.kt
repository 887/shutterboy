package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.domain.sort.PhotoSort

/**
 * Phase C.2 + C.3 — Photos timeline body. `LazyVerticalGrid` reading from
 * the narrow [PhotoSource] facet (R.A locked). Density-zoom (C.3) drives
 * column count + per-cell shape via [PhotosZoomLevel]; pinch-to-cycle is
 * wired by the caller (`PhotosScreen`) so this composable stays declarative.
 *
 * Inline month-year bands at [PhotosZoomLevel.Items] (Aves pattern,
 * scroll-with-content, no overlay-pin — locked in C.2). Year bands at
 * `.Days` and `.Months`. No bands at `.Years` (each year is a hero tile).
 */
@Composable
internal fun PhotosGrid(
    photoSource: PhotoSource,
    sort: PhotoSort,
    level: PhotosZoomLevel,
    onPhotoTap: (PhotoId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val photos by photoSource.observePhotos(sort)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    if (photos.isEmpty()) {
        EmptyPhotosState(modifier = modifier)
        return
    }

    val locale = LocalConfiguration.current.locales.get(0) ?: java.util.Locale.getDefault()
    val timeline = buildTimeline(photos, level)

    LazyVerticalGrid(
        columns = GridCells.Fixed(level.columns),
        modifier = modifier
            .fillMaxSize()
            .animateContentSize(),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        timeline.forEachIndexed { index, item ->
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
                            onTap = { onPhotoTap(item.photo.id) },
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
                            onClick = { onPhotoTap(item.cover.id) },
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
                            onClick = { onPhotoTap(item.cover.id) },
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
                            onClick = { onPhotoTap(item.cover.id) },
                        )
                    }
            }
            // [index] is not used directly — the per-item `key` already drives
            // diff stability; suppress unused-warning by referencing it.
            @Suppress("UNUSED_EXPRESSION") index
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: Photo,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = photo.contentUri,
        contentDescription = photo.displayName,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onTap),
    )
}
