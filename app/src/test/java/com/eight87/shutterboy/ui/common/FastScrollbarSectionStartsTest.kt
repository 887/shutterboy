package com.eight87.shutterboy.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class FastScrollbarSectionStartsTest {

    @Test
    fun `empty section list yields empty starts`() {
        val starts = sectionStartsFromHeaderRuns(emptyList(), emptyMap())
        assertEquals(emptyList<Pair<Int, String>>(), starts)
    }

    @Test
    fun `single section places header at index zero`() {
        val starts = sectionStartsFromHeaderRuns(
            sectionLabels = listOf("MAY 2026"),
            itemCountsBySection = mapOf("MAY 2026" to 7),
        )
        assertEquals(listOf(0 to "MAY 2026"), starts)
    }

    @Test
    fun `multi-section header indices follow one-band-plus-N-items layout`() {
        // MAY 2026:  [0]=band, [1..3]=3 photos
        // APR 2026:  [4]=band, [5..6]=2 photos
        // MAR 2026:  [7]=band, [8..12]=5 photos
        val starts = sectionStartsFromHeaderRuns(
            sectionLabels = listOf("MAY 2026", "APR 2026", "MAR 2026"),
            itemCountsBySection = mapOf(
                "MAY 2026" to 3,
                "APR 2026" to 2,
                "MAR 2026" to 5,
            ),
        )
        assertEquals(
            listOf(
                0 to "MAY 2026",
                4 to "APR 2026",
                7 to "MAR 2026",
            ),
            starts,
        )
    }

    @Test
    fun `missing count in map is treated as zero items`() {
        val starts = sectionStartsFromHeaderRuns(
            sectionLabels = listOf("A", "B", "C"),
            itemCountsBySection = mapOf("B" to 4),
        )
        // A: band-only -> next cursor = 1
        // B: band + 4 items -> next cursor = 6
        // C: band-only
        assertEquals(
            listOf(0 to "A", 1 to "B", 6 to "C"),
            starts,
        )
    }

    @Test
    fun `section labels preserve display order`() {
        val starts = sectionStartsFromHeaderRuns(
            sectionLabels = listOf("Z", "M", "A"),
            itemCountsBySection = mapOf("Z" to 1, "M" to 1, "A" to 1),
        )
        assertEquals(listOf("Z", "M", "A"), starts.map { it.second })
    }
}
