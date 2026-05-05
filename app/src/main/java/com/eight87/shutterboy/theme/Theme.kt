package com.eight87.shutterboy.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 theme for shutterboy. Three layers:
 *
 * 1. **Dynamic color (Material You) on API 31+** — when [dynamicColor] is true
 *    the colour scheme is sourced from the system wallpaper-derived palette.
 *    Default on; Phase I.2 will surface a `ThemeSettings.dynamicColor` toggle.
 * 2. **Brand palette fallback** on API < 31 — burnt-orange primary, warm-copper
 *    secondary, slate-blue tertiary; surfaces collapse onto the warm charcoal
 *    that also drives the launcher / splash background so the cold-boot
 *    handoff stays continuous.
 * 3. **System dark / light** — picked up via [isSystemInDarkTheme]. Phase I.2
 *    will surface a `ThemeSettings.theme: System | Light | Dark` override.
 *
 * Phase I.2 will replace the dynamicColor parameter with a
 * `ThemeSettings`-driven enum (System / Light / Dark + DynamicColor toggle +
 * Black mode). The shape of this composable stays the same.
 */
@Composable
fun ShutterboyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(darkTheme = darkTheme, dynamicColor = dynamicColor)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

/**
 * Pure colour-scheme picker — split out so a Robolectric test (Phase I.2 test
 * suite) can verify the three-way pick logic without instantiating the full
 * theme.
 */
@Composable
internal fun resolveColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return if (darkTheme) ShutterboyDarkColorScheme else ShutterboyLightColorScheme
}

private val ShutterboyDarkColorScheme: ColorScheme = darkColorScheme(
    primary = ShutterOrange80,
    secondary = ShutterCopper80,
    tertiary = ShutterSlate80,
    background = ShutterCharcoal,
    surface = ShutterCharcoal,
)

private val ShutterboyLightColorScheme: ColorScheme = lightColorScheme(
    primary = ShutterOrange40,
    secondary = ShutterCopper40,
    tertiary = ShutterSlate40,
    background = ShutterCharcoalLight,
    surface = ShutterCharcoalLight,
)
