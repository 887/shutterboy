package com.eight87.shutterboy.ui.slideshow

/**
 * Phase J.1 — pure helper for slideshow advance math. Kept as a top-level
 * object so `SlideshowAdvanceMathTest` can exercise it without spinning
 * up Robolectric.
 *
 * Contract: at position N of M items, [nextPage] yields `(N + 1) mod M`.
 * Empty backing list is the degenerate edge case — stays at 0 (caller
 * never schedules an advance against an empty pager anyway, but the
 * helper handles it defensively).
 */
internal object SlideshowAdvanceMath {
    fun nextPage(currentPage: Int, pageCount: Int): Int {
        if (pageCount <= 0) return 0
        return (currentPage + 1).mod(pageCount)
    }
}
