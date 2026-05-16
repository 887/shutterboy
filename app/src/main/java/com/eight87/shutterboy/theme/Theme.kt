@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.eight87.shutterboy.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Material 3 Expressive theme for shutterboy. Three layers:
 *
 * 1. **Dynamic color (Material You) on API 31+** — when [dynamicColor] is true
 *    the colour scheme is sourced from the system wallpaper-derived palette.
 *    Default on; Phase I.2 will surface a `ThemeSettings.dynamicColor` toggle.
 * 2. **Brand seed fallback** on API < 31 — burnt-orange primary, warm-copper
 *    secondary, slate-blue tertiary. Light mode uses
 *    [expressiveLightColorScheme] which produces the wider surface-tier ladder
 *    (`surfaceContainerLow…High`) M3E needs; dark mode stays on
 *    [darkColorScheme] (no `expressiveDarkColorScheme` ships in 1.5.0-alpha18).
 * 3. **System dark / light** — picked up via [isSystemInDarkTheme]. Phase I.2
 *    will surface a `ThemeSettings.theme: System | Light | Dark` override.
 *
 * Wrapped in [MaterialExpressiveTheme] so M3E motion / typography / shape
 * defaults (rounded XL group shapes, faster spring motion) apply.
 */
@Composable
fun ShutterboyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveColorScheme(darkTheme = darkTheme, dynamicColor = dynamicColor)
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

/**
 * Pure colour-scheme picker — split out so a Robolectric test can verify the
 * three-way pick logic without instantiating the full theme.
 */
@Composable
internal fun resolveColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return if (darkTheme) ShutterboyDarkColorScheme else ShutterboyLightColorScheme
}

// m3-expressive B.2 — don't collapse `background` AND `surface` onto the
// same colour. Let M3E derive `background` from `surface` so the
// surfaceContainer* ladder reads as lifted against the page.
private val ShutterboyDarkColorScheme: ColorScheme = darkColorScheme(
    primary = ShutterOrange80,
    secondary = ShutterCopper80,
    tertiary = ShutterSlate80,
)

// expressiveLightColorScheme() is no-arg in 1.5.0-alpha18 — overlay the
// brand seeds via .copy(). The wider surface-tier ladder is preserved.
private val ShutterboyLightColorScheme: ColorScheme = expressiveLightColorScheme().copy(
    primary = ShutterOrange40,
    secondary = ShutterCopper40,
    tertiary = ShutterSlate40,
)
