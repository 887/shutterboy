package com.eight87.shutterboy.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExifEnricherTest {

    @Test
    fun `parseShutterSpeed handles fractional seconds form`() {
        // 1/250 → 0.004
        assertEquals(0.004f, parseShutterSpeed("1/250")!!, 1e-6f)
    }

    @Test
    fun `parseShutterSpeed handles decimal seconds form`() {
        assertEquals(0.0033f, parseShutterSpeed("0.0033")!!, 1e-6f)
    }

    @Test
    fun `parseShutterSpeed handles integer seconds`() {
        assertEquals(2.0f, parseShutterSpeed("2")!!, 1e-6f)
    }

    @Test
    fun `parseShutterSpeed returns null for null input`() {
        assertNull(parseShutterSpeed(null))
    }

    @Test
    fun `parseShutterSpeed returns null for blank input`() {
        assertNull(parseShutterSpeed("   "))
    }

    @Test
    fun `parseShutterSpeed returns null for unparseable input`() {
        assertNull(parseShutterSpeed("oops"))
    }

    @Test
    fun `parseShutterSpeed returns null for divide-by-zero`() {
        assertNull(parseShutterSpeed("1/0"))
    }

    @Test
    fun `parseShutterSpeed handles long shutter`() {
        assertEquals(30.0f, parseShutterSpeed("30/1")!!, 1e-6f)
    }
}
