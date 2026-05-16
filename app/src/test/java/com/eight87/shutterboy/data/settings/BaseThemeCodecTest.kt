package com.eight87.shutterboy.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-JUnit round-trip for [BaseTheme] storage codec. Verifies that
 * `toStored` + `fromStored` is a fixed point for every variant, that
 * malformed input falls back to [BaseTheme.Default], and that the
 * `Custom:` hex parser tolerates both the canonical `0x`-prefixed form
 * and the bare-hex form.
 */
class BaseThemeCodecTest {

    @Test
    fun `DefaultAndroid round-trips`() {
        val v: BaseTheme = BaseTheme.DefaultAndroid
        assertEquals(v, BaseTheme.fromStored(v.toStored()))
    }

    @Test
    fun `DefaultColors round-trips`() {
        val v: BaseTheme = BaseTheme.DefaultColors
        assertEquals(v, BaseTheme.fromStored(v.toStored()))
    }

    @Test
    fun `PureBlack round-trips`() {
        val v: BaseTheme = BaseTheme.PureBlack
        assertEquals(v, BaseTheme.fromStored(v.toStored()))
    }

    @Test
    fun `Custom round-trips with assorted seeds`() {
        val seeds = listOf(0x000000L, 0xFFFFFFL, 0x6750A4L, 0xB94A1AL, 0xABCDEFL)
        for (seed in seeds) {
            val v = BaseTheme.Custom(seed)
            assertEquals("round-trip failed for $seed", v, BaseTheme.fromStored(v.toStored()))
        }
    }

    @Test
    fun `Custom toStored emits canonical 0x-prefixed uppercase hex`() {
        assertEquals("Custom:0x6750A4", BaseTheme.Custom(0x6750A4L).toStored())
        assertEquals("Custom:0x000000", BaseTheme.Custom(0x0L).toStored())
        assertEquals("Custom:0xFFFFFF", BaseTheme.Custom(0xFFFFFFL).toStored())
    }

    @Test
    fun `Custom fromStored accepts bare hex without 0x prefix`() {
        assertEquals(BaseTheme.Custom(0x6750A4L), BaseTheme.fromStored("Custom:6750A4"))
    }

    @Test
    fun `Custom fromStored accepts lower-case prefix`() {
        assertEquals(BaseTheme.Custom(0xABCDEFL), BaseTheme.fromStored("Custom:0xabcdef"))
    }

    @Test
    fun `Custom fromStored masks high bits to 24 bits`() {
        // Alpha byte (or any high bits) is dropped — only 24 bits of RGB survive.
        assertEquals(BaseTheme.Custom(0x123456L), BaseTheme.fromStored("Custom:0xFF123456"))
    }

    @Test
    fun `null falls back to Default`() {
        assertEquals(BaseTheme.Default, BaseTheme.fromStored(null))
    }

    @Test
    fun `unknown variant falls back to Default`() {
        assertEquals(BaseTheme.Default, BaseTheme.fromStored("WhateverThemeName"))
    }

    @Test
    fun `malformed Custom hex falls back to Default`() {
        assertEquals(BaseTheme.Default, BaseTheme.fromStored("Custom:not-hex"))
        assertEquals(BaseTheme.Default, BaseTheme.fromStored("Custom:"))
    }
}
