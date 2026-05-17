package com.eight87.shutterboy.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM coverage for [ViewerZoomMath]. The Compose-bound
 * `Modifier.transformable` + `detectTapGestures(onDoubleTap=…)` paths in
 * [PhotoViewerScreen] are verified visually by the user — adb `input`
 * does not synthesize reliable multi-touch for pinch, and double-tap
 * timing under `input tap` is fragile (plan G.3.1 / G.3.2 testing
 * note). The testable seam is the math.
 */
class ViewerZoomMathTest {

    @Test
    fun `clampScale clamps below MIN_SCALE up to MIN_SCALE`() {
        assertEquals(1f, ViewerZoomMath.clampScale(0.5f), 0f)
        assertEquals(1f, ViewerZoomMath.clampScale(0f), 0f)
        assertEquals(1f, ViewerZoomMath.clampScale(-2f), 0f)
    }

    @Test
    fun `clampScale passes through scales inside the range`() {
        assertEquals(1f, ViewerZoomMath.clampScale(1f), 0f)
        assertEquals(1.5f, ViewerZoomMath.clampScale(1.5f), 0f)
        assertEquals(2f, ViewerZoomMath.clampScale(2f), 0f)
        assertEquals(3f, ViewerZoomMath.clampScale(3f), 0f)
    }

    @Test
    fun `clampScale clamps above MAX_SCALE down to MAX_SCALE`() {
        assertEquals(3f, ViewerZoomMath.clampScale(3.5f), 0f)
        assertEquals(3f, ViewerZoomMath.clampScale(10f), 0f)
        assertEquals(3f, ViewerZoomMath.clampScale(Float.MAX_VALUE), 0f)
    }

    @Test
    fun `double-tap toggle goes from rest to zoomed`() {
        assertEquals(2f, ViewerZoomMath.toggledScale(1f), 0f)
    }

    @Test
    fun `double-tap toggle collapses any zoomed-in scale back to rest`() {
        assertEquals(1f, ViewerZoomMath.toggledScale(2f), 0f)
        // While zoomed past rest (e.g. mid-pinch), a double-tap also resets.
        assertEquals(1f, ViewerZoomMath.toggledScale(1.5f), 0f)
        assertEquals(1f, ViewerZoomMath.toggledScale(3f), 0f)
    }

    @Test
    fun `double-tap from below rest still climbs to zoomed target`() {
        // clampScale should already prevent this in practice, but the
        // toggle's contract is "if not zoomed in, zoom in".
        assertEquals(2f, ViewerZoomMath.toggledScale(0.5f), 0f)
    }

    @Test
    fun `isZoomed gates pan accumulation`() {
        assertFalse(ViewerZoomMath.isZoomed(1f))
        assertFalse(ViewerZoomMath.isZoomed(0.5f))
        assertTrue(ViewerZoomMath.isZoomed(1.01f))
        assertTrue(ViewerZoomMath.isZoomed(2f))
        assertTrue(ViewerZoomMath.isZoomed(3f))
    }
}
