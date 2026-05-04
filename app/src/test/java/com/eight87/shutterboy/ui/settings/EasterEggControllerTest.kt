package com.eight87.shutterboy.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure tap-counter state machine pinned in tests so the easter-egg controller
 * can be exhaustively verified without spinning up Compose. Mirrors
 * tonearmboy's `EasterEggControllerTest` shape.
 */
class EasterEggControllerTest {

    private val window = 2_000L

    @Test
    fun `first tap starts a new run`() {
        val s0 = EasterEggTapState()
        val s1 = s0.afterTap(now = 1_000L, windowMs = window)
        assertEquals(1, s1.tapCount)
        assertEquals(1_000L, s1.firstTapAtMs)
    }

    @Test
    fun `second tap inside window advances count, keeps firstTapAtMs`() {
        val s1 = EasterEggTapState(tapCount = 1, firstTapAtMs = 1_000L)
        val s2 = s1.afterTap(now = 1_500L, windowMs = window)
        assertEquals(2, s2.tapCount)
        assertEquals(1_000L, s2.firstTapAtMs)
    }

    @Test
    fun `third tap inside window reaches threshold`() {
        val s2 = EasterEggTapState(tapCount = 2, firstTapAtMs = 1_000L)
        val s3 = s2.afterTap(now = 2_500L, windowMs = window)
        assertEquals(3, s3.tapCount)
        assertEquals(EASTER_EGG_TAP_THRESHOLD, s3.tapCount)
    }

    @Test
    fun `tap exactly at the window edge still counts (less-than-or-equal boundary)`() {
        // afterTap uses strict greater-than for reset, so now == first + window stays in window.
        val s1 = EasterEggTapState(tapCount = 1, firstTapAtMs = 1_000L)
        val s2 = s1.afterTap(now = 1_000L + window, windowMs = window)
        assertEquals(2, s2.tapCount)
    }

    @Test
    fun `tap one ms past the window restarts the run`() {
        val s2 = EasterEggTapState(tapCount = 2, firstTapAtMs = 1_000L)
        val s3 = s2.afterTap(now = 1_000L + window + 1, windowMs = window)
        assertEquals(1, s3.tapCount)
        assertEquals(1_000L + window + 1, s3.firstTapAtMs)
    }

    @Test
    fun `firstTapAtMs null treats every tap as a fresh run`() {
        val s0 = EasterEggTapState(tapCount = 99, firstTapAtMs = null)
        val s1 = s0.afterTap(now = 5_000L, windowMs = window)
        assertEquals(1, s1.tapCount)
        assertEquals(5_000L, s1.firstTapAtMs)
    }

    @Test
    fun `mixed pattern - tap reset tap tap tap reaches threshold`() {
        var state = EasterEggTapState()
        state = state.afterTap(now = 0L, windowMs = window) // tap 1, count=1
        state = state.afterTap(now = window + 1, windowMs = window) // outside window -> count=1, new run
        state = state.afterTap(now = window + 100, windowMs = window) // count=2
        state = state.afterTap(now = window + 200, windowMs = window) // count=3
        assertEquals(3, state.tapCount)
    }

    @Test
    fun `default constants match the design`() {
        assertEquals(3, EASTER_EGG_TAP_THRESHOLD)
        assertEquals(2_000L, EASTER_EGG_WINDOW_MS)
    }
}
