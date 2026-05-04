package com.eight87.shutterboy.domain.sort

import android.net.Uri
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo
import com.eight87.shutterboy.domain.PhotoId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class PhotoSortTest {

    private fun photo(
        id: Long,
        name: String,
        dateTakenMs: Long,
        dateAddedMs: Long = dateTakenMs,
        sizeBytes: Long = 100_000,
    ): Photo = Photo(
        id = PhotoId(id),
        contentUri = Uri.parse("content://test/$id"),
        displayName = name,
        dateTakenMs = dateTakenMs,
        dateAddedMs = dateAddedMs,
        width = 100,
        height = 100,
        sizeBytes = sizeBytes,
        mimeType = "image/jpeg",
        folderId = FolderId(1),
    )

    private val a = photo(id = 1, name = "alpha.jpg", dateTakenMs = 3000, sizeBytes = 300)
    private val b = photo(id = 2, name = "beta.jpg", dateTakenMs = 1000, sizeBytes = 100)
    private val c = photo(id = 3, name = "Charlie.JPG", dateTakenMs = 2000, sizeBytes = 200)
    private val unsorted = listOf(c, a, b)

    @Test
    fun `ByDateTaken DESC default puts newest first`() {
        val sorted = unsorted.sortedWith(PhotoSort.ByDateTaken().comparator)
        assertEquals(listOf(a, c, b), sorted)
    }

    @Test
    fun `ByDateTaken ASC inverts to oldest first`() {
        val sorted = unsorted.sortedWith(PhotoSort.ByDateTaken(Direction.ASC).comparator)
        assertEquals(listOf(b, c, a), sorted)
    }

    @Test
    fun `ByName ASC default sorts alphabetically case-insensitive`() {
        val sorted = unsorted.sortedWith(PhotoSort.ByName().comparator)
        assertEquals(listOf(a, b, c), sorted)
    }

    @Test
    fun `BySize DESC default puts biggest first`() {
        val sorted = unsorted.sortedWith(PhotoSort.BySize().comparator)
        assertEquals(listOf(a, c, b), sorted)
    }

    @Test
    fun `BySize ASC inverts to smallest first`() {
        val sorted = unsorted.sortedWith(PhotoSort.BySize(Direction.ASC).comparator)
        assertEquals(listOf(b, c, a), sorted)
    }

    @Test
    fun `Default sort is ByDateTaken DESC`() {
        val default = PhotoSort.Default
        assert(default is PhotoSort.ByDateTaken)
        assertEquals(Direction.DESC, default.direction)
    }
}
