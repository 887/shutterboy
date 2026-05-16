@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.eight87.shutterboy.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * m3-expressive B.4 — cheap regression guard. The M3E surface-tier
 * ladder only reads as "lifted" if `surface` and `surfaceContainer`
 * (etc.) resolve to distinguishable RGB values. If a future patch
 * collapses them back onto one colour, this test catches it before the
 * AVD smoke would.
 *
 * Threshold is a coarse Euclidean-RGB distance — easier to reason
 * about than a true CIEDE2000 and good enough to spot a "both colours
 * equal" regression.
 */
class SurfaceTierContractTest {

    @Test
    fun `dark scheme - surface and surfaceContainer differ noticeably`() {
        val dark = darkColorScheme(
            primary = ShutterOrange80,
            secondary = ShutterCopper80,
            tertiary = ShutterSlate80,
        )
        assertDistinguishable(dark, "dark")
    }

    @Test
    fun `light scheme - surface and surfaceContainer differ noticeably`() {
        val light = expressiveLightColorScheme().copy(
            primary = ShutterOrange40,
            secondary = ShutterCopper40,
            tertiary = ShutterSlate40,
        )
        assertDistinguishable(light, "light")
    }

    @Test
    fun `dark scheme - surfaceContainer ladder is monotonic`() {
        val dark = darkColorScheme(
            primary = ShutterOrange80,
            secondary = ShutterCopper80,
            tertiary = ShutterSlate80,
        )
        val tiers = listOf(
            dark.surfaceContainerLowest,
            dark.surfaceContainerLow,
            dark.surfaceContainer,
            dark.surfaceContainerHigh,
            dark.surfaceContainerHighest,
        )
        for (i in 0 until tiers.size - 1) {
            val a = tiers[i]
            val b = tiers[i + 1]
            assertTrue(
                "dark tier $i should not equal tier ${i + 1}: $a vs $b",
                a.toArgb() != b.toArgb(),
            )
        }
    }

    private fun assertDistinguishable(scheme: ColorScheme, label: String) {
        val s = scheme.surface
        val c = scheme.surfaceContainer
        val dr = s.red - c.red
        val dg = s.green - c.green
        val db = s.blue - c.blue
        val distance = kotlin.math.sqrt(dr * dr + dg * dg + db * db)
        // 0.02 in 0..1 RGB space ~= delta-E ≥ 5 in human-perceptible terms.
        assertTrue(
            "$label scheme: surface ($s) and surfaceContainer ($c) collapsed onto the same tier (distance=$distance)",
            distance >= 0.02f,
        )
    }

    private fun androidx.compose.ui.graphics.Color.toArgb(): Int =
        (alpha * 255f).toInt().shl(24) or
            (red * 255f).toInt().shl(16) or
            (green * 255f).toInt().shl(8) or
            (blue * 255f).toInt()
}
