package com.eight87.shutterboy.ui.photos.grid

import android.net.Uri
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import com.eight87.shutterboy.domain.sort.PhotoSort
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class SectionStartsTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun photo(
        id: Long,
        displayName: String = "p$id.jpg",
        dateTakenMs: Long = 0L,
        sizeBytes: Long = 0L,
    ): Photo = Photo(
        id = PhotoId(id),
        contentUri = Uri.parse("content://x/$id"),
        displayName = displayName,
        dateTakenMs = dateTakenMs,
        dateAddedMs = dateTakenMs,
        width = 100,
        height = 100,
        sizeBytes = sizeBytes,
        mimeType = "image/jpeg",
        folderId = FolderId(1L),
    )

    private fun msAt(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `empty timeline yields empty starts`() {
        val starts = sectionStartsFrom(emptyList(), PhotoSort.ByDateTaken(), Locale.ENGLISH, zone)
        assertEquals(emptyList<Pair<Int, String>>(), starts)
    }

    @Test
    fun `ByDateTaken emits one entry per MonthYearBand at its index`() {
        val timeline = listOf(
            TimelineDisplayItem.MonthYearBand(YearMonth.of(2026, 5)),
            TimelineDisplayItem.PhotoCell(photo(1, dateTakenMs = msAt(2026, 5, 11))),
            TimelineDisplayItem.PhotoCell(photo(2, dateTakenMs = msAt(2026, 5, 9))),
            TimelineDisplayItem.MonthYearBand(YearMonth.of(2026, 4)),
            TimelineDisplayItem.PhotoCell(photo(3, dateTakenMs = msAt(2026, 4, 1))),
        )
        val starts = sectionStartsFrom(timeline, PhotoSort.ByDateTaken(), Locale.ENGLISH, zone)
        assertEquals(listOf(0 to "MAY 2026", 3 to "APRIL 2026"), starts)
    }

    @Test
    fun `ByDateTaken at Days density picks up YearBand entries`() {
        val timeline = listOf(
            TimelineDisplayItem.YearBand(2026),
            TimelineDisplayItem.DayCell(LocalDate.of(2026, 5, 11), photo(1), photoCount = 3),
            TimelineDisplayItem.YearBand(2025),
            TimelineDisplayItem.DayCell(LocalDate.of(2025, 12, 1), photo(2), photoCount = 1),
        )
        val starts = sectionStartsFrom(timeline, PhotoSort.ByDateTaken(), Locale.ENGLISH, zone)
        assertEquals(listOf(0 to "2026", 2 to "2025"), starts)
    }

    @Test
    fun `ByName emits at first-letter transitions over PhotoCell stream`() {
        val timeline = listOf(
            TimelineDisplayItem.PhotoCell(photo(1, displayName = "alpha.jpg")),
            TimelineDisplayItem.PhotoCell(photo(2, displayName = "antelope.jpg")),
            TimelineDisplayItem.PhotoCell(photo(3, displayName = "beach.jpg")),
            TimelineDisplayItem.PhotoCell(photo(4, displayName = "carousel.jpg")),
        )
        val starts = sectionStartsFrom(timeline, PhotoSort.ByName(), Locale.ENGLISH, zone)
        assertEquals(listOf(0 to "A", 2 to "B", 3 to "C"), starts)
    }

    @Test
    fun `BySize emits at bucket transitions`() {
        val timeline = listOf(
            TimelineDisplayItem.PhotoCell(photo(1, sizeBytes = 500L * 1024)),       // < 1 MB
            TimelineDisplayItem.PhotoCell(photo(2, sizeBytes = 800L * 1024)),       // < 1 MB
            TimelineDisplayItem.PhotoCell(photo(3, sizeBytes = 3L * 1024 * 1024)),  // 1-5 MB
            TimelineDisplayItem.PhotoCell(photo(4, sizeBytes = 10L * 1024 * 1024)), // 5-20 MB
        )
        val starts = sectionStartsFrom(timeline, PhotoSort.BySize(), Locale.ENGLISH, zone)
        assertEquals(listOf(0 to "< 1 MB", 2 to "1-5 MB", 3 to "5-20 MB"), starts)
    }

    @Test
    fun `sort-axis swap on identical photo timeline produces different starts`() {
        val timeline = listOf(
            TimelineDisplayItem.MonthYearBand(YearMonth.of(2026, 5)),
            TimelineDisplayItem.PhotoCell(photo(1, displayName = "alpha.jpg", dateTakenMs = msAt(2026, 5, 11))),
            TimelineDisplayItem.PhotoCell(photo(2, displayName = "beta.jpg", dateTakenMs = msAt(2026, 5, 9))),
        )
        val dateStarts = sectionStartsFrom(timeline, PhotoSort.ByDateTaken(), Locale.ENGLISH, zone)
        val nameStarts = sectionStartsFrom(timeline, PhotoSort.ByName(), Locale.ENGLISH, zone)
        assertEquals(listOf(0 to "MAY 2026"), dateStarts)
        // Name starts skip the band (no Photo on it) and emit at PhotoCell transitions.
        assertEquals(listOf(1 to "A", 2 to "B"), nameStarts)
    }
}
