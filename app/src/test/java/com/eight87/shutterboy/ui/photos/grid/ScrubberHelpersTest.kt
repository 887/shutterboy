package com.eight87.shutterboy.ui.photos.grid

import android.net.Uri
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/**
 * Phase C.7 — verifies the pure scrubber + sticky-header derivation
 * helpers. Robolectric only because [Photo] uses [android.net.Uri], not
 * for any Compose surface.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class ScrubberHelpersTest {

    private val utc: ZoneId = ZoneId.of("UTC")

    private fun photo(id: Long, ldt: LocalDateTime): Photo {
        val ms = ldt.toInstant(ZoneOffset.UTC).toEpochMilli()
        return Photo(
            id = PhotoId(id),
            contentUri = Uri.parse("content://test/$id"),
            displayName = "p$id.jpg",
            dateTakenMs = ms,
            dateAddedMs = ms,
            width = 100,
            height = 100,
            sizeBytes = 1000,
            mimeType = "image/jpeg",
            folderId = FolderId(1),
        )
    }

    private val sample: List<Photo> = listOf(
        photo(1, LocalDateTime.of(2026, 5, 4, 12, 0)),
        photo(2, LocalDateTime.of(2026, 5, 4, 9, 0)),
        photo(3, LocalDateTime.of(2026, 5, 1, 18, 0)),
        photo(4, LocalDateTime.of(2026, 4, 30, 18, 0)),
        photo(5, LocalDateTime.of(2025, 12, 15, 10, 0)),
        photo(6, LocalDateTime.of(2024, 1, 1, 0, 0)),
    )

    // ---- extractYearMarkers ------------------------------------------------

    @Test
    fun `extractYearMarkers on empty timeline returns empty`() {
        assertTrue(extractYearMarkers(emptyList()).isEmpty())
    }

    @Test
    fun `extractYearMarkers at Items level finds first index of each year`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Items, utc)
        val markers = extractYearMarkers(timeline)
        // Bands at indices: 2026-05 (0), 2026-04 (4), 2025-12 (6), 2024-01 (8).
        // Markers: 2026 → 0, 2025 → 6, 2024 → 8.
        assertEquals(3, markers.size)
        assertEquals(YearMarker(year = 2026, timelineIndex = 0), markers[0])
        assertEquals(YearMarker(year = 2025, timelineIndex = 6), markers[1])
        assertEquals(YearMarker(year = 2024, timelineIndex = 8), markers[2])
    }

    @Test
    fun `extractYearMarkers at Years level emits one marker per YearCell`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Years, utc)
        val markers = extractYearMarkers(timeline)
        // Three YearCell entries → three markers, indices 0, 1, 2.
        assertEquals(3, markers.size)
        assertEquals(YearMarker(2026, 0), markers[0])
        assertEquals(YearMarker(2025, 1), markers[1])
        assertEquals(YearMarker(2024, 2), markers[2])
    }

    @Test
    fun `extractYearMarkers single-year input collapses to one marker`() {
        val singleYear = sample.take(4)  // all 2026
        val timeline = buildTimeline(singleYear, PhotosZoomLevel.Items, utc)
        val markers = extractYearMarkers(timeline)
        assertEquals(1, markers.size)
        assertEquals(2026, markers[0].year)
        assertEquals(0, markers[0].timelineIndex)
    }

    // ---- yearAtFraction ----------------------------------------------------

    @Test
    fun `yearAtFraction on empty markers returns null`() {
        assertNull(yearAtFraction(emptyList(), 0.5f))
    }

    @Test
    fun `yearAtFraction maps 0 to first marker and 1 to last`() {
        val markers = listOf(YearMarker(2026, 0), YearMarker(2025, 6), YearMarker(2024, 8))
        assertEquals(YearMarker(2026, 0), yearAtFraction(markers, 0f))
        assertEquals(YearMarker(2024, 8), yearAtFraction(markers, 1f))
    }

    @Test
    fun `yearAtFraction clamps out-of-range fractions to nearest end`() {
        val markers = listOf(YearMarker(2026, 0), YearMarker(2025, 6))
        assertEquals(YearMarker(2026, 0), yearAtFraction(markers, -0.5f))
        assertEquals(YearMarker(2025, 6), yearAtFraction(markers, 1.7f))
    }

    @Test
    fun `yearAtFraction picks middle marker at 0_5`() {
        val markers = listOf(YearMarker(2026, 0), YearMarker(2025, 6), YearMarker(2024, 8))
        assertEquals(YearMarker(2025, 6), yearAtFraction(markers, 0.5f))
    }

    // ---- stickyHeaderLabel -------------------------------------------------

    @Test
    fun `stickyHeaderLabel on empty timeline returns null`() {
        assertNull(stickyHeaderLabel(emptyList(), 0, PhotosZoomLevel.Items, Locale.US))
    }

    @Test
    fun `stickyHeaderLabel at Items returns month-year of preceding band`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Items, utc)
        // index 2 is the second 2026-05 photo cell; band at 0 covers it.
        assertEquals("MAY 2026", stickyHeaderLabel(timeline, 2, PhotosZoomLevel.Items, Locale.US))
    }

    @Test
    fun `stickyHeaderLabel at Items walks backward across photo cells`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Items, utc)
        // index 5 is the 2026-04 photo cell; band at 4 is MonthYearBand(2026-04).
        assertEquals("APRIL 2026", stickyHeaderLabel(timeline, 5, PhotosZoomLevel.Items, Locale.US))
    }

    @Test
    fun `stickyHeaderLabel at Days returns year of preceding YearBand`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Days, utc)
        // Days timeline starts with YearBand(2026) at index 0; index 1 is the
        // first DayCell.
        assertEquals("2026", stickyHeaderLabel(timeline, 1, PhotosZoomLevel.Days, Locale.US))
    }

    @Test
    fun `stickyHeaderLabel at Years uses YearCell directly`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Years, utc)
        assertEquals("2025", stickyHeaderLabel(timeline, 1, PhotosZoomLevel.Years, Locale.US))
    }

    @Test
    fun `stickyHeaderLabel clamps out-of-range index to last valid item`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Years, utc)
        // Last YearCell is 2024 — out-of-range index should clamp there.
        assertEquals("2024", stickyHeaderLabel(timeline, 999, PhotosZoomLevel.Years, Locale.US))
    }

    // ---- Sanity checks against the timeline builder ------------------------

    @Test
    fun `Items timeline first marker points at the very first band`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Items, utc)
        val markers = extractYearMarkers(timeline)
        val first = markers.first()
        assertEquals(TimelineDisplayItem.MonthYearBand(YearMonth.of(2026, 5)), timeline[first.timelineIndex])
    }

    @Test
    fun `Days timeline last marker points at the last YearBand`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Days, utc)
        val markers = extractYearMarkers(timeline)
        val last = markers.last()
        assertEquals(TimelineDisplayItem.YearBand(2024), timeline[last.timelineIndex])
        // And the corresponding DayCell sits one slot later.
        val dayCell = timeline[last.timelineIndex + 1] as TimelineDisplayItem.DayCell
        assertEquals(LocalDate.of(2024, 1, 1), dayCell.date)
    }
}
