package com.eight87.shutterboy.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JUnit checks on [deriveCustomScheme]. Verifies that:
 *  - primary / secondary / tertiary anchors are distinct (i.e. the hue
 *    shift actually moves the secondary and tertiary off the primary);
 *  - the same seed produces different `primary` values in light vs dark
 *    mode (lightness lift differs);
 *  - `onPrimary` is picked to contrast the primary (Black on a light
 *    primary, White on a dark one) via the luminance threshold.
 */
class DeriveCustomSchemeTest {

    @Test
    fun `primary secondary tertiary anchors are distinct in dark mode`() {
        val s = deriveCustomScheme(seedRgb = 0xB94A1AL, darkTheme = true)
        assertNotEquals(s.primary, s.secondary)
        assertNotEquals(s.primary, s.tertiary)
        assertNotEquals(s.secondary, s.tertiary)
    }

    @Test
    fun `primary secondary tertiary anchors are distinct in light mode`() {
        val s = deriveCustomScheme(seedRgb = 0xB94A1AL, darkTheme = false)
        assertNotEquals(s.primary, s.secondary)
        assertNotEquals(s.primary, s.tertiary)
        assertNotEquals(s.secondary, s.tertiary)
    }

    @Test
    fun `same seed yields different primary across light and dark`() {
        val seed = 0x6750A4L
        val light = deriveCustomScheme(seed, darkTheme = false)
        val dark = deriveCustomScheme(seed, darkTheme = true)
        assertNotEquals(light.primary, dark.primary)
    }

    @Test
    fun `onPrimary is black for a light-luminance primary (yellow seed)`() {
        // Pure yellow seed → high-luminance primary anchor in dark mode
        // (l = 0.7), so the onPrimary picker should land on Black.
        val s = deriveCustomScheme(seedRgb = 0xFFFF00L, darkTheme = true)
        assertTrue(
            "expected Black onPrimary for high-luminance primary ${s.primary}; got ${s.onPrimary}",
            s.onPrimary == Color.Black,
        )
    }

    @Test
    fun `onPrimary is white for a dark-luminance primary (navy seed in light mode)`() {
        // Deep navy seed in light mode lands the primary anchor on a
        // dark, low-luminance colour (l = 0.4), so onPrimary picks White.
        val s = deriveCustomScheme(seedRgb = 0x001050L, darkTheme = false)
        assertTrue(
            "expected White onPrimary for low-luminance primary ${s.primary}; got ${s.onPrimary}",
            s.onPrimary == Color.White,
        )
    }
}
