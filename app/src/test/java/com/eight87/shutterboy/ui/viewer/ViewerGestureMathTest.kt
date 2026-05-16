package com.eight87.shutterboy.ui.viewer

import com.eight87.shutterboy.ui.viewer.ViewerGestureMath.ReleaseAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM coverage for [ViewerGestureMath]. The Compose-bound gesture
 * handler in [PhotoViewerScreen] is verified by AVD smoke (plan G.1.5 /
 * G.2.5); the testable seam is the math.
 */
class ViewerGestureMathTest {

    private val dismissPx = 384f // 128 dp at 3.0 density
    private val infoPx = 192f    // 64 dp at 3.0 density

    @Test
    fun `dismiss alpha is 1 at zero drag`() {
        assertEquals(1f, ViewerGestureMath.dismissScrimAlpha(0f, dismissPx), 0f)
    }

    @Test
    fun `dismiss alpha is 0 exactly at threshold`() {
        assertEquals(0f, ViewerGestureMath.dismissScrimAlpha(dismissPx, dismissPx), 0f)
    }

    @Test
    fun `dismiss alpha clamps to 0 past the threshold`() {
        assertEquals(0f, ViewerGestureMath.dismissScrimAlpha(dismissPx * 2f, dismissPx), 0f)
        assertEquals(0f, ViewerGestureMath.dismissScrimAlpha(10_000f, dismissPx), 0f)
    }

    @Test
    fun `dismiss alpha is monotone half-fade at half-threshold`() {
        assertEquals(0.5f, ViewerGestureMath.dismissScrimAlpha(dismissPx / 2f, dismissPx), 1e-4f)
    }

    @Test
    fun `upward drag does not change dismiss alpha`() {
        // Negative drag (upward) is the info-open gesture's territory;
        // the dismiss scrim must stay fully opaque.
        assertEquals(1f, ViewerGestureMath.dismissScrimAlpha(-50f, dismissPx), 0f)
        assertEquals(1f, ViewerGestureMath.dismissScrimAlpha(-dismissPx, dismissPx), 0f)
    }

    @Test
    fun `zero or negative threshold yields full alpha`() {
        // Defensive: avoids div-by-zero if a caller forgets to convert dp→px.
        assertEquals(1f, ViewerGestureMath.dismissScrimAlpha(100f, 0f), 0f)
        assertEquals(1f, ViewerGestureMath.dismissScrimAlpha(100f, -10f), 0f)
    }

    @Test
    fun `dismiss threshold predicate fires at and past threshold`() {
        assertFalse(ViewerGestureMath.exceedsDismissThreshold(dismissPx - 1f, dismissPx))
        assertTrue(ViewerGestureMath.exceedsDismissThreshold(dismissPx, dismissPx))
        assertTrue(ViewerGestureMath.exceedsDismissThreshold(dismissPx + 100f, dismissPx))
    }

    @Test
    fun `info-open threshold predicate fires on upward travel only`() {
        assertFalse(ViewerGestureMath.exceedsInfoOpenThreshold(infoPx, infoPx))
        assertFalse(ViewerGestureMath.exceedsInfoOpenThreshold(0f, infoPx))
        assertTrue(ViewerGestureMath.exceedsInfoOpenThreshold(-infoPx, infoPx))
        assertTrue(ViewerGestureMath.exceedsInfoOpenThreshold(-infoPx * 2f, infoPx))
    }

    @Test
    fun `release classifies an idle accumulator as None`() {
        assertEquals(
            ReleaseAction.None,
            ViewerGestureMath.classifyRelease(0f, dismissPx, infoPx),
        )
    }

    @Test
    fun `release classifies past-dismiss-threshold as Dismiss`() {
        assertEquals(
            ReleaseAction.Dismiss,
            ViewerGestureMath.classifyRelease(dismissPx + 1f, dismissPx, infoPx),
        )
    }

    @Test
    fun `release classifies past-info-threshold as OpenInfo`() {
        assertEquals(
            ReleaseAction.OpenInfo,
            ViewerGestureMath.classifyRelease(-(infoPx + 1f), dismissPx, infoPx),
        )
    }

    @Test
    fun `release between thresholds returns None`() {
        assertEquals(
            ReleaseAction.None,
            ViewerGestureMath.classifyRelease(40f, dismissPx, infoPx),
        )
        assertEquals(
            ReleaseAction.None,
            ViewerGestureMath.classifyRelease(-20f, dismissPx, infoPx),
        )
    }
}
