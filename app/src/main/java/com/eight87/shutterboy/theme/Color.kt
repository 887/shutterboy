package com.eight87.shutterboy.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Shutterboy brand palette — burnt-orange primary + warm-charcoal surfaces,
 * matched to the launcher icon's "Capture the Stripes" copper-on-black design.
 * These tokens are the API < 31 fallback; on API 31+ Material You /
 * dynamic-color takes over (see [ShutterboyTheme]).
 */

// Primary — burnt orange, the tiger-fur tone.
val ShutterOrange40 = Color(0xFFB94A1A)
val ShutterOrange80 = Color(0xFFFFB68F)

// Secondary — warm copper / brass, the nametag tone.
val ShutterCopper40 = Color(0xFF7E5A38)
val ShutterCopper80 = Color(0xFFE6C2A1)

// Tertiary — muted slate-blue for cool accent (folders, smart-album chips).
val ShutterSlate40 = Color(0xFF44627B)
val ShutterSlate80 = Color(0xFFAAC9E5)

// =============================================================================
// BaseTheme.Custom — pure HSL helpers ported from tonearmboy. Internal so the
// theme package consumes them; no Compose runtime required, JVM-testable.
// =============================================================================

/**
 * Derive a Material 3 [ColorScheme] from a 24-bit RGB seed.
 *
 * Strategy: build primary / secondary / tertiary tonal anchors by
 * shifting the seed's hue (secondary = +30°, tertiary = +60°) and
 * lightness, then plug them into the canonical
 * `lightColorScheme` / `darkColorScheme` factories. This sidesteps
 * Material 3's `dynamicColorScheme(seed, isDark)` (added in 1.4) so
 * the build works regardless of the active Material 3 version.
 *
 * Pure helper for unit-testability — no Compose runtime required.
 */
internal fun deriveCustomScheme(seedRgb: Long, darkTheme: Boolean): ColorScheme {
    val primary = colorFromRgbLong(seedRgb)
    val (h, s, _) = rgbToHslTriple(primary)
    val secondary = hslColor(((h + 30f) % 360f), (s * 0.7f).coerceIn(0f, 1f), if (darkTheme) 0.7f else 0.45f)
    val tertiary = hslColor(((h + 60f) % 360f), (s * 0.6f).coerceIn(0f, 1f), if (darkTheme) 0.7f else 0.5f)
    val primaryDark = hslColor(h, s, if (darkTheme) 0.7f else 0.4f)
    val onPrimary = if (luminance(primaryDark) > 0.5f) Color.Black else Color.White
    return if (darkTheme) {
        darkColorScheme(
            primary = primaryDark,
            secondary = secondary,
            tertiary = tertiary,
            onPrimary = onPrimary,
        )
    } else {
        lightColorScheme(
            primary = primaryDark,
            secondary = secondary,
            tertiary = tertiary,
            onPrimary = onPrimary,
        )
    }
}

internal fun colorFromRgbLong(rgb: Long): Color {
    val r = ((rgb shr 16) and 0xFFL).toInt()
    val g = ((rgb shr 8) and 0xFFL).toInt()
    val b = (rgb and 0xFFL).toInt()
    return Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = 1f)
}

/** Returns (hue 0..360, saturation 0..1, lightness 0..1). */
internal fun rgbToHslTriple(c: Color): Triple<Float, Float, Float> {
    val r = c.red; val g = c.green; val b = c.blue
    val max = maxOf(r, g, b); val min = minOf(r, g, b)
    val l = (max + min) / 2f
    val delta = max - min
    if (delta == 0f) return Triple(0f, 0f, l)
    val s = if (l > 0.5f) delta / (2f - max - min) else delta / (max + min)
    val h = when (max) {
        r -> 60f * (((g - b) / delta) % 6f)
        g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Triple(h, s, l)
}

internal fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val s = saturation.coerceIn(0f, 1f)
    val l = lightness.coerceIn(0f, 1f)
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val hp = h / 60f
    val x = c * (1f - kotlin.math.abs((hp % 2f) - 1f))
    val (r1, g1, b1) = when {
        hp < 1f -> Triple(c, x, 0f)
        hp < 2f -> Triple(x, c, 0f)
        hp < 3f -> Triple(0f, c, x)
        hp < 4f -> Triple(0f, x, c)
        hp < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = l - c / 2f
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
        alpha = 1f,
    )
}

internal fun luminance(c: Color): Float {
    // Relative luminance per WCAG-ish; cheap enough for an on-color decision.
    return 0.2126f * c.red + 0.7152f * c.green + 0.0722f * c.blue
}
