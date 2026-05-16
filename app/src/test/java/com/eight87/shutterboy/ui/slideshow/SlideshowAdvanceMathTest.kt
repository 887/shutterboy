package com.eight87.shutterboy.ui.slideshow

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase J.1 — pure-JUnit coverage of [SlideshowAdvanceMath.nextPage].
 *
 *   - At position N of M items, advance produces (N + 1) mod M.
 *   - Wraparound from last → first.
 *   - Empty list edge case stays at 0.
 */
class SlideshowAdvanceMathTest {

    @Test
    fun advance_from_middle_yields_next_index() {
        assertEquals(3, SlideshowAdvanceMath.nextPage(currentPage = 2, pageCount = 5))
    }

    @Test
    fun advance_from_first_yields_second() {
        assertEquals(1, SlideshowAdvanceMath.nextPage(currentPage = 0, pageCount = 3))
    }

    @Test
    fun advance_wraps_from_last_to_first() {
        assertEquals(0, SlideshowAdvanceMath.nextPage(currentPage = 4, pageCount = 5))
    }

    @Test
    fun advance_with_single_page_stays_at_zero() {
        assertEquals(0, SlideshowAdvanceMath.nextPage(currentPage = 0, pageCount = 1))
    }

    @Test
    fun advance_with_empty_list_stays_at_zero() {
        assertEquals(0, SlideshowAdvanceMath.nextPage(currentPage = 0, pageCount = 0))
    }

    @Test
    fun advance_with_empty_list_and_nonzero_position_stays_at_zero() {
        assertEquals(0, SlideshowAdvanceMath.nextPage(currentPage = 7, pageCount = 0))
    }
}
