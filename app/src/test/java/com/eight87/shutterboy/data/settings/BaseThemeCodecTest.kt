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
    fun `legacy DefaultColors stored value falls back to Default`() {
        // "DefaultColors" was the old shutterboy-only brand palette;
        // it no longer exists and is mapped to the shared default.
        assertEquals(BaseTheme.Default, BaseTheme.fromStored("DefaultColors"))
    }

    @Test
    fun `PureBlack round-trips`() {
        val v: BaseTheme = BaseTheme.PureBlack
        assertEquals(v, BaseTheme.fromStored(v.toStored()))
    }

    @Test
    fun `legacy Custom stored values extract their seed`() {
        // The old combined-base-and-tint format. The seed moves to
        // the new tint-colour pref on migration; base falls back to
        // Default. This test asserts the parser still extracts the
        // seed cleanly.
        val seeds = listOf(0x000000L, 0xFFFFFFL, 0x6750A4L, 0xB94A1AL, 0xABCDEFL)
        for (seed in seeds) {
            val raw = "Custom:0x${seed.toString(16).padStart(6, '0').uppercase()}"
            assertEquals("seed parse failed for $seed", seed, BaseTheme.extractLegacyCustomSeed(raw))
            assertEquals(BaseTheme.Default, BaseTheme.fromStored(raw))
        }
    }

    @Test
    fun `legacy Custom fromStored accepts bare hex without 0x prefix`() {
        assertEquals(0x6750A4L, BaseTheme.extractLegacyCustomSeed("Custom:6750A4"))
    }

    @Test
    fun `legacy Custom fromStored accepts lower-case prefix`() {
        assertEquals(0xABCDEFL, BaseTheme.extractLegacyCustomSeed("Custom:0xabcdef"))
    }

    @Test
    fun `legacy extractLegacyCustomSeed masks high bits to 24 bits`() {
        assertEquals(0x123456L, BaseTheme.extractLegacyCustomSeed("Custom:0xFF123456"))
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
