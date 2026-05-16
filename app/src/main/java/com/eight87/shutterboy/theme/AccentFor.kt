package com.eight87.shutterboy.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Pick a [CategoryAccent] for the row identified by [id]. Hash-based
 * so the mapping is stable across runs without a hand-built id→accent
 * map.
 *
 * Pure (non-Compose) entry point — takes an explicit `isDark` flag so
 * it's drivable from unit tests and from contexts that don't have a
 * composition. The `(id.hashCode().rem(N) + N).rem(N)` shape avoids
 * Kotlin's sign-preserving `%` on negative hashes.
 */
internal fun accentFor(id: String, isDark: Boolean): CategoryAccent {
    val palette = if (isDark) DarkCategoryPalette else LightCategoryPalette
    val raw = id.hashCode() % palette.size
    val idx = (raw + palette.size) % palette.size
    return palette[idx]
}

/**
 * Compose-side convenience: reads the current dark-theme flag and
 * delegates to [accentFor]. Used by `SettingsRow`'s auto-accent
 * fallback so call sites that pass `id` get a category accent for
 * free.
 */
@Composable
@ReadOnlyComposable
internal fun accentFor(id: String): CategoryAccent =
    accentFor(id, isSystemInDarkTheme())
