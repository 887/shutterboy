package com.eight87.shutterboy.theme

import androidx.compose.ui.graphics.Color

/**
 * m3-expressive Phase D — pair of `(container, onContainer)` colours
 * for the per-row coloured circle row-icon avatars. Mirrors Material
 * 3's `primaryContainer` / `onPrimaryContainer` token shape so the
 * runtime use site reads identically to a built-in colour role.
 *
 * Five hand-picked hues map 1:1 to shutterboy's Settings categories
 * (Look and Feel / Library / Photos / Albums / About) — see the table
 * in `docs/plans/m3-expressive.md`. [accentFor] picks an entry by
 * stable hash of a row id so direct callers and future I.x catalog
 * rows get the right accent without per-call wiring.
 *
 * The accents are NOT driven off `dynamicDarkColorScheme()` — letting
 * the user's wallpaper steamroll the per-category intent defeats the
 * colour-coding entirely. Hand-picked stays hand-picked even when
 * Dynamic Color is on for the rest of the M3 palette.
 */
data class CategoryAccent(
    val container: Color,
    val onContainer: Color,
)

/**
 * Dark / AMOLED-leaning palette. Containers around chroma 60 %,
 * lightness 22–28 % so the circle reads as a quietly-saturated tile
 * against the page; on-container tones land at lightness 80 % for
 * high-contrast filled glyphs.
 *
 * Order matches the table in m3-expressive.md:
 *   0 Look and Feel — sky blue
 *   1 Library       — purple
 *   2 Photos        — pink
 *   3 Albums        — orange
 *   4 About         — green
 */
internal val DarkCategoryPalette: List<CategoryAccent> = listOf(
    CategoryAccent(container = Color(0xFF003D5C), onContainer = Color(0xFF8FCEFF)), // sky blue
    CategoryAccent(container = Color(0xFF3F2C73), onContainer = Color(0xFFD0BCFF)), // purple
    CategoryAccent(container = Color(0xFF5C2940), onContainer = Color(0xFFFFB1C8)), // pink
    CategoryAccent(container = Color(0xFF5C3300), onContainer = Color(0xFFFFB877)), // orange
    CategoryAccent(container = Color(0xFF1F4D2E), onContainer = Color(0xFF9CDDB4)), // green
)

/**
 * Light palette — same hues, brighter containers so the avatar reads
 * against the lighter page surface. On-container tones drop to
 * lightness 25 % for legible filled glyphs.
 */
internal val LightCategoryPalette: List<CategoryAccent> = listOf(
    CategoryAccent(container = Color(0xFFCFE8FF), onContainer = Color(0xFF003047)),
    CategoryAccent(container = Color(0xFFE8DEF8), onContainer = Color(0xFF21005D)),
    CategoryAccent(container = Color(0xFFFFD8E5), onContainer = Color(0xFF3F0026)),
    CategoryAccent(container = Color(0xFFFFDDB6), onContainer = Color(0xFF2B1700)),
    CategoryAccent(container = Color(0xFFC8E8D2), onContainer = Color(0xFF002912)),
)
