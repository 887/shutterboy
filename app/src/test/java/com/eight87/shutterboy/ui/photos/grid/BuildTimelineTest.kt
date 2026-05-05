package com.eight87.shutterboy.ui.photos.grid

import android.net.Uri
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Verifies the per-density timeline builder folds a flat photo list into the
 * right shape per [PhotosZoomLevel]. Robolectric only because [Photo] uses
 * [android.net.Uri], not for any Compose surface.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class BuildTimelineTest {

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
        // 2026 May
        photo(1, LocalDateTime.of(2026, 5, 4, 12, 0)),
        photo(2, LocalDateTime.of(2026, 5, 4, 9, 0)),
        photo(3, LocalDateTime.of(2026, 5, 1, 18, 0)),
        // 2026 April
        photo(4, LocalDateTime.of(2026, 4, 30, 18, 0)),
        // 2025 December
        photo(5, LocalDateTime.of(2025, 12, 15, 10, 0)),
    )

    @Test
    fun `Items level interleaves month-year bands and photo cells`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Items, utc)
        // Expect: Band(2026-05) + 3 photos + Band(2026-04) + 1 photo + Band(2025-12) + 1 photo
        // = 3 bands + 5 cells = 8 elements
        assertEquals(8, timeline.size)
        assertEquals(TimelineDisplayItem.MonthYearBand(YearMonth.of(2026, 5)), timeline[0])
        assertTrue(timeline[1] is TimelineDisplayItem.PhotoCell)
        assertTrue(timeline[2] is TimelineDisplayItem.PhotoCell)
        assertTrue(timeline[3] is TimelineDisplayItem.PhotoCell)
        assertEquals(TimelineDisplayItem.MonthYearBand(YearMonth.of(2026, 4)), timeline[4])
        assertTrue(timeline[5] is TimelineDisplayItem.PhotoCell)
        assertEquals(TimelineDisplayItem.MonthYearBand(YearMonth.of(2025, 12)), timeline[6])
        assertTrue(timeline[7] is TimelineDisplayItem.PhotoCell)
    }

    @Test
    fun `Days level aggregates per-day with year bands and photo counts`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Days, utc)
        // 2026 → May 4 (2 photos), May 1 (1), April 30 (1) ; 2025 → Dec 15 (1)
        // Expect: YearBand(2026), Day(May 4, count=2), Day(May 1, count=1), Day(Apr 30, count=1),
        //         YearBand(2025), Day(Dec 15, count=1)
        assertEquals(6, timeline.size)
        assertEquals(TimelineDisplayItem.YearBand(2026), timeline[0])
        val firstDay = timeline[1] as TimelineDisplayItem.DayCell
        assertEquals(2, firstDay.photoCount)
        val secondDay = timeline[2] as TimelineDisplayItem.DayCell
        assertEquals(1, secondDay.photoCount)
        assertEquals(TimelineDisplayItem.YearBand(2025), timeline[4])
        val onlyDecDay = timeline[5] as TimelineDisplayItem.DayCell
        assertEquals(1, onlyDecDay.photoCount)
    }

    @Test
    fun `Months level aggregates per-month with year bands`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Months, utc)
        // 2026 → May (3 photos), April (1) ; 2025 → December (1)
        assertEquals(5, timeline.size)
        assertEquals(TimelineDisplayItem.YearBand(2026), timeline[0])
        val may = timeline[1] as TimelineDisplayItem.MonthCell
        assertEquals(YearMonth.of(2026, 5), may.yearMonth)
        assertEquals(3, may.photoCount)
        val april = timeline[2] as TimelineDisplayItem.MonthCell
        assertEquals(YearMonth.of(2026, 4), april.yearMonth)
        assertEquals(1, april.photoCount)
        assertEquals(TimelineDisplayItem.YearBand(2025), timeline[3])
        val dec25 = timeline[4] as TimelineDisplayItem.MonthCell
        assertEquals(YearMonth.of(2025, 12), dec25.yearMonth)
    }

    @Test
    fun `Years level produces one cell per year, no bands`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Years, utc)
        assertEquals(2, timeline.size)
        val y2026 = timeline[0] as TimelineDisplayItem.YearCell
        assertEquals(2026, y2026.year)
        assertEquals(4, y2026.photoCount)
        val y2025 = timeline[1] as TimelineDisplayItem.YearCell
        assertEquals(2025, y2025.year)
        assertEquals(1, y2025.photoCount)
    }

    @Test
    fun `empty input returns empty timeline at every level`() {
        for (level in PhotosZoomLevel.entries) {
            assertTrue(buildTimeline(emptyList(), level, utc).isEmpty())
        }
    }

    @Test
    fun `cover is the first photo per group (newest-first input)`() {
        val timeline = buildTimeline(sample, PhotosZoomLevel.Years, utc)
        val y2026 = timeline[0] as TimelineDisplayItem.YearCell
        // First 2026 photo in input order is id=1 (May 4 12:00).
        assertEquals(1L, y2026.cover.id.value)
    }
}
