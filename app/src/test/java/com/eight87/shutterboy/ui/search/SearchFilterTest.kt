package com.eight87.shutterboy.ui.search

import android.net.Uri
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase G.3 — exercises the AND-combination of the active boolean filter
 * set, the favorite-id intersection, and the optional folder restriction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchFilterTest {

    private val now = 1_700_000_000_000L

    private fun makePhoto(
        id: Long,
        folderId: Long,
        dateTakenMs: Long = now,
        lat: Double? = null,
        lon: Double? = null,
    ): Photo = Photo(
        id = PhotoId(id),
        contentUri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "photo-$id.jpg",
        dateTakenMs = dateTakenMs,
        dateAddedMs = dateTakenMs,
        width = 4000,
        height = 3000,
        sizeBytes = 4_000_000L,
        mimeType = "image/jpeg",
        folderId = FolderId(folderId),
        latitude = lat,
        longitude = lon,
    )

    private val photos = listOf(
        makePhoto(1L, folderId = 10L, lat = 1.0, lon = 1.0),
        makePhoto(2L, folderId = 10L),
        makePhoto(3L, folderId = 20L, dateTakenMs = now - 60L * 24L * 60L * 60L * 1000L),
        makePhoto(4L, folderId = 20L, lat = 2.0, lon = 2.0),
    )

    @Test
    fun empty_active_no_folder_returns_input_unchanged() {
        val out = applyFilters(photos, emptySet(), now)
        assertEquals(photos, out)
    }

    @Test
    fun favorites_chip_intersects_with_favorite_id_set() {
        val out = applyFilters(
            photos = photos,
            active = setOf(SearchFilter.Favorites),
            nowMs = now,
            favoriteIds = setOf(1L, 4L),
        )
        assertEquals(listOf(1L, 4L), out.map { it.id.value })
    }

    @Test
    fun folder_filter_alone_restricts_to_that_folder() {
        val out = applyFilters(
            photos = photos,
            active = emptySet(),
            nowMs = now,
            selectedFolderId = FolderId(20L),
        )
        assertEquals(listOf(3L, 4L), out.map { it.id.value })
    }

    @Test
    fun favorites_AND_folder_AND_gps_combine_with_AND_semantics() {
        val out = applyFilters(
            photos = photos,
            active = setOf(SearchFilter.Favorites, SearchFilter.Gps),
            nowMs = now,
            favoriteIds = setOf(1L, 4L),
            selectedFolderId = FolderId(20L),
        )
        // Photo 4 is the only one in folder 20 with GPS AND a favorite.
        assertEquals(listOf(4L), out.map { it.id.value })
    }

    @Test
    fun recent_filter_excludes_old_photos() {
        val out = applyFilters(
            photos = photos,
            active = setOf(SearchFilter.Recent),
            nowMs = now,
        )
        // Photo 3 is 60 days old, beyond the 30-day window.
        assertEquals(listOf(1L, 2L, 4L), out.map { it.id.value })
    }
}
