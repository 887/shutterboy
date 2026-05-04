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
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class PhotoSectionTest {

    private val utc = ZoneId.of("UTC")

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

    @Test
    fun `groupByMonth on empty list returns empty`() {
        assertTrue(groupByMonth(emptyList(), utc).isEmpty())
    }

    @Test
    fun `groupByMonth produces one section per month-year preserving input order`() {
        val photos = listOf(
            photo(1, LocalDateTime.of(2026, 5, 4, 12, 0)),  // May 2026
            photo(2, LocalDateTime.of(2026, 5, 1, 9, 0)),   // May 2026
            photo(3, LocalDateTime.of(2026, 4, 30, 18, 0)), // April 2026
            photo(4, LocalDateTime.of(2026, 1, 15, 10, 0)), // January 2026
        )
        val sections = groupByMonth(photos, utc)
        assertEquals(3, sections.size)
        assertEquals(YearMonth.of(2026, 5), sections[0].yearMonth)
        assertEquals(listOf(1L, 2L), sections[0].photos.map { it.id.value })
        assertEquals(YearMonth.of(2026, 4), sections[1].yearMonth)
        assertEquals(listOf(3L), sections[1].photos.map { it.id.value })
        assertEquals(YearMonth.of(2026, 1), sections[2].yearMonth)
        assertEquals(listOf(4L), sections[2].photos.map { it.id.value })
    }

    @Test
    fun `groupByMonth single section when all photos share a month`() {
        val photos = listOf(
            photo(1, LocalDateTime.of(2026, 5, 1, 0, 0)),
            photo(2, LocalDateTime.of(2026, 5, 31, 23, 59)),
        )
        val sections = groupByMonth(photos, utc)
        assertEquals(1, sections.size)
        assertEquals(YearMonth.of(2026, 5), sections[0].yearMonth)
        assertEquals(2, sections[0].photos.size)
    }

    @Test
    fun `groupByMonth re-bucketing when oscillating across month boundaries`() {
        // ASC chronological order — but contrived to verify that a non-monotonic input
        // does NOT pre-merge sections; the grouper preserves input order section-by-section.
        val photos = listOf(
            photo(1, LocalDateTime.of(2026, 5, 1, 0, 0)),  // May
            photo(2, LocalDateTime.of(2026, 4, 30, 0, 0)), // April
            photo(3, LocalDateTime.of(2026, 5, 2, 0, 0)),  // May again — separate section
        )
        val sections = groupByMonth(photos, utc)
        assertEquals(3, sections.size)
        assertEquals(YearMonth.of(2026, 5), sections[0].yearMonth)
        assertEquals(YearMonth.of(2026, 4), sections[1].yearMonth)
        assertEquals(YearMonth.of(2026, 5), sections[2].yearMonth)
    }

    @Test
    fun `formatMonthBand uppercases en-US labels`() {
        assertEquals("MAY 2026", formatMonthBand(YearMonth.of(2026, 5), Locale.US))
        assertEquals("JANUARY 2026", formatMonthBand(YearMonth.of(2026, 1), Locale.US))
        assertEquals("DECEMBER 2025", formatMonthBand(YearMonth.of(2025, 12), Locale.US))
    }

    @Test
    fun `formatMonthBand respects locale`() {
        // German "MAI" instead of English "MAY" — proves the locale isn't hard-coded.
        assertEquals("MAI 2026", formatMonthBand(YearMonth.of(2026, 5), Locale.GERMAN))
    }
}
