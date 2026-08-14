package com.eight87.shutterboy.ui.multiselect

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
 * The MIME a bulk-share chooser filters on. Too broad and the sheet fills
 * with apps that can't take the files; too narrow and the right target is
 * missing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CommonMimeTypeTest {

    private fun photo(id: Long, mime: String) = Photo(
        id = PhotoId(id),
        contentUri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "IMG_$id",
        dateTakenMs = 0L,
        dateAddedMs = 0L,
        width = 1,
        height = 1,
        sizeBytes = 1L,
        mimeType = mime,
        folderId = FolderId(1L),
    )

    @Test
    fun `identical types collapse to the exact type`() {
        val types = listOf(photo(1, "image/jpeg"), photo(2, "image/jpeg"))
        assertEquals("image/jpeg", commonMimeType(types))
    }

    @Test
    fun `same top-level type widens to the wildcard subtype`() {
        val types = listOf(photo(1, "image/jpeg"), photo(2, "image/png"))
        assertEquals("image/*", commonMimeType(types))
    }

    @Test
    fun `mixed image and video falls back to any type`() {
        val types = listOf(photo(1, "image/jpeg"), photo(2, "video/mp4"))
        assertEquals("*/*", commonMimeType(types))
    }

    @Test
    fun `blank types are ignored rather than widening the result`() {
        val types = listOf(photo(1, "image/jpeg"), photo(2, ""))
        assertEquals("image/jpeg", commonMimeType(types))
    }
}
