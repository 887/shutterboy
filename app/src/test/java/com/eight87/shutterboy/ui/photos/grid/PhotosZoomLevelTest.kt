package com.eight87.shutterboy.ui.photos.grid

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure tests for the C.3 density-zoom state machine. No Compose, no
 * Robolectric — both [PhotosZoomLevel] and [ZoomAccumulator] are intentionally
 * pure data so these run as JVM JUnit tests.
 */
class PhotosZoomLevelTest {

    // ---- PhotosZoomLevel ----

    @Test
    fun `zoomIn cycles toward Items`() {
        assertEquals(PhotosZoomLevel.Months, PhotosZoomLevel.Years.zoomIn())
        assertEquals(PhotosZoomLevel.Days, PhotosZoomLevel.Months.zoomIn())
        assertEquals(PhotosZoomLevel.Items, PhotosZoomLevel.Days.zoomIn())
    }

    @Test
    fun `zoomIn clamps at Items`() {
        assertEquals(PhotosZoomLevel.Items, PhotosZoomLevel.Items.zoomIn())
    }

    @Test
    fun `zoomOut cycles toward Years`() {
        assertEquals(PhotosZoomLevel.Days, PhotosZoomLevel.Items.zoomOut())
        assertEquals(PhotosZoomLevel.Months, PhotosZoomLevel.Days.zoomOut())
        assertEquals(PhotosZoomLevel.Years, PhotosZoomLevel.Months.zoomOut())
    }

    @Test
    fun `zoomOut clamps at Years`() {
        assertEquals(PhotosZoomLevel.Years, PhotosZoomLevel.Years.zoomOut())
    }

    @Test
    fun `column count matches design doc`() {
        assertEquals(4, PhotosZoomLevel.Items.columns)
        assertEquals(3, PhotosZoomLevel.Days.columns)
        assertEquals(2, PhotosZoomLevel.Months.columns)
        assertEquals(1, PhotosZoomLevel.Years.columns)
    }

    // ---- ZoomAccumulator ----

    @Test
    fun `small zoom changes accumulate without transitioning`() {
        var acc = ZoomAccumulator()
        acc = acc.apply(1.1f) // pending=1.1, below 1.5 in
        assertEquals(PhotosZoomLevel.Items, acc.level)
        assertEquals(1.1f, acc.pendingZoom, 1e-6f)
        acc = acc.apply(1.2f) // pending=1.32, still below 1.5
        assertEquals(PhotosZoomLevel.Items, acc.level)
        assertEquals(1.32f, acc.pendingZoom, 1e-6f)
    }

    @Test
    fun `crossing the in-threshold zooms in and resets pending`() {
        var acc = ZoomAccumulator(level = PhotosZoomLevel.Years, pendingZoom = 1.4f)
        acc = acc.apply(1.2f) // 1.4 * 1.2 = 1.68 >= 1.5 -> zoom in
        assertEquals(PhotosZoomLevel.Months, acc.level)
        assertEquals(1.0f, acc.pendingZoom, 1e-6f)
    }

    @Test
    fun `crossing the out-threshold zooms out and resets pending`() {
        var acc = ZoomAccumulator(level = PhotosZoomLevel.Items, pendingZoom = 0.8f)
        acc = acc.apply(0.8f) // 0.8 * 0.8 = 0.64 <= 0.66 -> zoom out
        assertEquals(PhotosZoomLevel.Days, acc.level)
        assertEquals(1.0f, acc.pendingZoom, 1e-6f)
    }

    @Test
    fun `at Items, further zoom-in attempts stay at Items`() {
        var acc = ZoomAccumulator(level = PhotosZoomLevel.Items)
        acc = acc.apply(2.0f) // 2.0 >= 1.5 — would zoom in, clamps at Items
        assertEquals(PhotosZoomLevel.Items, acc.level)
        assertEquals(1.0f, acc.pendingZoom, 1e-6f)
    }

    @Test
    fun `at Years, further zoom-out attempts stay at Years`() {
        var acc = ZoomAccumulator(level = PhotosZoomLevel.Years)
        acc = acc.apply(0.5f) // 0.5 <= 0.66 — would zoom out, clamps at Years
        assertEquals(PhotosZoomLevel.Years, acc.level)
        assertEquals(1.0f, acc.pendingZoom, 1e-6f)
    }

    @Test
    fun `unity zoom never transitions`() {
        val before = ZoomAccumulator(level = PhotosZoomLevel.Months, pendingZoom = 1.2f)
        val after = before.apply(1.0f) // 1.2 * 1.0 = 1.2, no threshold crossed
        assertEquals(before.level, after.level)
        assertEquals(1.2f, after.pendingZoom, 1e-6f)
    }
}
