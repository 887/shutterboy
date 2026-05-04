package com.eight87.shutterboy.ui.photos.grid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eight87.shutterboy.data.repo.PhotoSource
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.domain.sort.PhotoSort

/**
 * Phase C.2 — Photos timeline body. `LazyVerticalGrid` of thumbnails with
 * inline month-year bands (Aves pattern, scroll-with-content, no overlay-pin
 * — locked in C.2). Density-zoom (C.3) and year-scrubber (C.4) layer on
 * top in their own commits; this commit ships the Items-density grid only.
 *
 * Composable takes the narrow [PhotoSource] facet, never the wholesale
 * `RoomGalleryRepository` (R.A locked).
 */
@Composable
internal fun PhotosGrid(
    photoSource: PhotoSource,
    sort: PhotoSort,
    onPhotoTap: (PhotoId) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
) {
    val photos by photoSource.observePhotos(sort)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    if (photos.isEmpty()) {
        EmptyPhotosState(modifier = modifier)
        return
    }

    val sections = groupByMonth(photos)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        sections.forEach { section ->
            // Inline scroll-with-content month-year band (Aves pattern).
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "band-${section.yearMonth}",
                contentType = "month_year_band",
            ) {
                MonthYearBand(yearMonth = section.yearMonth)
            }
            items(
                items = section.photos,
                key = { it.id.value },
                contentType = { "photo_thumbnail" },
            ) { photo ->
                PhotoThumbnail(photo = photo, onTap = { onPhotoTap(photo.id) })
            }
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
