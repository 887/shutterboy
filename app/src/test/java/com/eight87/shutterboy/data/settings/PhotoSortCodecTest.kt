package com.eight87.shutterboy.data.settings

import com.eight87.shutterboy.domain.sort.Direction
import com.eight87.shutterboy.domain.sort.PhotoSort
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase E.5 — round-trip [PhotoSort] through [PhotoSortCodec]. Pure-JUnit;
 * no Compose / Android dependency.
 */
class PhotoSortCodecTest {

    @Test
    fun `every sort × direction round-trips`() {
        val sorts = listOf(
            PhotoSort.ByDateTaken(Direction.ASC),
            PhotoSort.ByDateTaken(Direction.DESC),
            PhotoSort.ByDateAdded(Direction.ASC),
            PhotoSort.ByDateAdded(Direction.DESC),
            PhotoSort.ByName(Direction.ASC),
            PhotoSort.ByName(Direction.DESC),
            PhotoSort.BySize(Direction.ASC),
            PhotoSort.BySize(Direction.DESC),
        )
        for (sort in sorts) {
            val token = PhotoSortCodec.encode(sort)
            val decoded = PhotoSortCodec.decode(token)
            assertEquals("round-trip failed for $sort (token=$token)", sort, decoded)
        }
    }

    @Test
    fun `tokens follow surface_role naming`() {
        assertEquals("date_taken_desc", PhotoSortCodec.encode(PhotoSort.ByDateTaken(Direction.DESC)))
        assertEquals("date_added_asc", PhotoSortCodec.encode(PhotoSort.ByDateAdded(Direction.ASC)))
        assertEquals("name_asc", PhotoSortCodec.encode(PhotoSort.ByName(Direction.ASC)))
        assertEquals("size_desc", PhotoSortCodec.encode(PhotoSort.BySize(Direction.DESC)))
    }

    @Test
    fun `unknown token decodes to Default`() {
        assertEquals(PhotoSort.Default, PhotoSortCodec.decode("garbage"))
        assertEquals(PhotoSort.Default, PhotoSortCodec.decode("date_taken_xyz"))
        assertEquals(PhotoSort.Default, PhotoSortCodec.decode(""))
    }

    @Test
    fun `null token decodes to Default`() {
        assertEquals(PhotoSort.Default, PhotoSortCodec.decode(null))
    }
}
