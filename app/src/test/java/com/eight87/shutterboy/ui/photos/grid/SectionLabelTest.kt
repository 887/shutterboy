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
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class SectionLabelTest {

    private val zone: ZoneId = ZoneOffset.UTC

    private fun photo(
        id: Long = 1L,
        displayName: String = "IMG.jpg",
        dateTakenMs: Long = 0L,
        dateAddedMs: Long = 0L,
        sizeBytes: Long = 0L,
    ): Photo = Photo(
        id = PhotoId(id),
        contentUri = Uri.parse("content://x/$id"),
        displayName = displayName,
        dateTakenMs = dateTakenMs,
        dateAddedMs = dateAddedMs,
        width = 100,
        height = 100,
        sizeBytes = sizeBytes,
        mimeType = "image/jpeg",
        folderId = FolderId(1L),
    )

    private fun msAt(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    @Test
    fun `ByDateTaken returns MMM yyyy band label`() {
        val p = photo(dateTakenMs = msAt(2026, 5, 11))
        val label = sectionLabelFor(PhotoSort.ByDateTaken(), p, Locale.ENGLISH, zone)
        assertEquals("MAY 2026", label)
    }

    @Test
    fun `ByDateAdded uses dateAddedMs not dateTakenMs`() {
        val p = photo(dateTakenMs = msAt(2024, 1, 1), dateAddedMs = msAt(2026, 5, 11))
        val label = sectionLabelFor(PhotoSort.ByDateAdded(), p, Locale.ENGLISH, zone)
        assertEquals("MAY 2026", label)
    }

    @Test
    fun `ByName returns first letter uppercased`() {
        val label = sectionLabelFor(PhotoSort.ByName(), photo(displayName = "afternoon.jpg"), Locale.ENGLISH)
        assertEquals("A", label)
    }

    @Test
    fun `ByName falls back to hash for non-letter leading char`() {
        val label = sectionLabelFor(PhotoSort.ByName(), photo(displayName = "_IMG_0001.jpg"), Locale.ENGLISH)
        assertEquals("#", label)
    }

    @Test
    fun `ByName is locale-aware for uppercase`() {
        // Turkish dotted-i: lowercase i should uppercase to dotted-İ in Turkish locale.
        val label = sectionLabelFor(PhotoSort.ByName(), photo(displayName = "istanbul.jpg"), Locale.forLanguageTag("tr"))
        assertEquals("İ", label)
    }

    @Test
    fun `BySize buckets at lt-1MB boundary`() {
        assertEquals("< 1 MB", sectionLabelFor(PhotoSort.BySize(), photo(sizeBytes = 500L * 1024)))
    }

    @Test
    fun `BySize buckets at 1-5MB`() {
        assertEquals("1-5 MB", sectionLabelFor(PhotoSort.BySize(), photo(sizeBytes = 3L * 1024 * 1024)))
    }

    @Test
    fun `BySize buckets at 5-20MB`() {
        assertEquals("5-20 MB", sectionLabelFor(PhotoSort.BySize(), photo(sizeBytes = 10L * 1024 * 1024)))
    }

    @Test
    fun `BySize buckets at gt-20MB`() {
        assertEquals("> 20 MB", sectionLabelFor(PhotoSort.BySize(), photo(sizeBytes = 50L * 1024 * 1024)))
    }
}
