package com.eight87.shutterboy.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Phase F.3 — pure-JUnit coverage of the human-readable EXIF formatters
 * (`1/125s`, `f/2.8`, `35mm`, `12 MP`). No Robolectric, no Compose.
 */
class ExifPanelFormattersTest {

    @Test
    fun `shutter speed sub-second renders as 1 over N`() {
        assertEquals("1/125s", ExifPanelFormatters.formatShutterSpeed(1f / 125f))
        assertEquals("1/250s", ExifPanelFormatters.formatShutterSpeed(0.004f))
        assertEquals("1/1000s", ExifPanelFormatters.formatShutterSpeed(0.001f))
    }

    @Test
    fun `shutter speed at or above one second renders with seconds suffix`() {
        assertEquals("1s", ExifPanelFormatters.formatShutterSpeed(1f))
        assertEquals("2s", ExifPanelFormatters.formatShutterSpeed(2f))
        assertEquals("1.5s", ExifPanelFormatters.formatShutterSpeed(1.5f))
    }

    @Test
    fun `shutter speed nulls and zeros yield null`() {
        assertNull(ExifPanelFormatters.formatShutterSpeed(null))
        assertNull(ExifPanelFormatters.formatShutterSpeed(0f))
        assertNull(ExifPanelFormatters.formatShutterSpeed(-1f))
        assertNull(ExifPanelFormatters.formatShutterSpeed(Float.NaN))
    }

    @Test
    fun `aperture renders as f-slash-N`() {
        assertEquals("f/2.8", ExifPanelFormatters.formatAperture(2.8f))
        assertEquals("f/4", ExifPanelFormatters.formatAperture(4f))
        assertEquals("f/1.4", ExifPanelFormatters.formatAperture(1.4f))
        assertEquals("f/8", ExifPanelFormatters.formatAperture(8.0f))
    }

    @Test
    fun `aperture nulls and zeros yield null`() {
        assertNull(ExifPanelFormatters.formatAperture(null))
        assertNull(ExifPanelFormatters.formatAperture(0f))
        assertNull(ExifPanelFormatters.formatAperture(-2.8f))
    }

    @Test
    fun `focal length renders as integer mm`() {
        assertEquals("35mm", ExifPanelFormatters.formatFocalLength(35f))
        assertEquals("50mm", ExifPanelFormatters.formatFocalLength(50.2f))
        assertEquals("85mm", ExifPanelFormatters.formatFocalLength(84.6f))
    }

    @Test
    fun `focal length nulls and zeros yield null`() {
        assertNull(ExifPanelFormatters.formatFocalLength(null))
        assertNull(ExifPanelFormatters.formatFocalLength(0f))
    }

    @Test
    fun `iso renders as ISO N`() {
        assertEquals("ISO 100", ExifPanelFormatters.formatIso(100))
        assertEquals("ISO 6400", ExifPanelFormatters.formatIso(6400))
        assertNull(ExifPanelFormatters.formatIso(null))
        assertNull(ExifPanelFormatters.formatIso(0))
        assertNull(ExifPanelFormatters.formatIso(-200))
    }

    @Test
    fun `megapixels rounds dimension product to whole MP`() {
        assertEquals("12 MP", ExifPanelFormatters.formatMegapixels(4000, 3000))
        assertEquals("12 MP", ExifPanelFormatters.formatMegapixels(4032, 3024))
        assertEquals("24 MP", ExifPanelFormatters.formatMegapixels(6000, 4000))
        assertEquals("1 MP", ExifPanelFormatters.formatMegapixels(1024, 1024))
    }

    @Test
    fun `megapixels rejects non-positive dims`() {
        assertNull(ExifPanelFormatters.formatMegapixels(0, 100))
        assertNull(ExifPanelFormatters.formatMegapixels(100, 0))
        assertNull(ExifPanelFormatters.formatMegapixels(-100, 100))
    }

    @Test
    fun `dimensions renders width times height`() {
        assertEquals("4032 × 3024", ExifPanelFormatters.formatDimensions(4032, 3024))
        assertNull(ExifPanelFormatters.formatDimensions(0, 100))
    }

    @Test
    fun `file size scales bytes to KB MB GB`() {
        assertEquals("512 B", ExifPanelFormatters.formatFileSize(512))
        assertEquals("2 KB", ExifPanelFormatters.formatFileSize(2048))
        // 5 MB exactly
        assertEquals("5.0 MB", ExifPanelFormatters.formatFileSize(5L * 1024 * 1024))
        // 1.5 GB
        assertEquals("1.5 GB", ExifPanelFormatters.formatFileSize((1.5 * 1024 * 1024 * 1024).toLong()))
        assertNull(ExifPanelFormatters.formatFileSize(0))
    }
}
