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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.eight87.shutterboy.data.settings.BaseTheme

/**
 * Material 3 Expressive theme for shutterboy. The base [ColorScheme] is
 * determined by [baseTheme]:
 *
 *  - [BaseTheme.DefaultAndroid] — Material You / dynamic colour on API 31+,
 *    falls back to the shutterboy brand palette on older devices.
 *  - [BaseTheme.DefaultColors] — the static shutterboy brand palette regardless
 *    of API.
 *  - [BaseTheme.PureBlack] — same primary colours as DefaultAndroid but with
 *    `surface` / `background` collapsed to pure black for AMOLED displays.
 *  - [BaseTheme.Custom] — a `ColorScheme` derived from a user-picked seed
 *    colour via the in-app HSV picker.
 *
 * Light mode uses [expressiveLightColorScheme] which produces the wider
 * surface-tier ladder (`surfaceContainerLow…High`) M3E needs; dark mode stays
 * on [darkColorScheme] (no `expressiveDarkColorScheme` ships yet).
 *
 * Wrapped in [MaterialExpressiveTheme] so M3E motion / typography / shape
 * defaults (rounded XL group shapes, faster spring motion) apply.
 */
@Composable
fun ShutterboyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    baseTheme: BaseTheme = BaseTheme.Default,
    content: @Composable () -> Unit,
) {
    val colorScheme = resolveBaseScheme(darkTheme = darkTheme, baseTheme = baseTheme)
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = ShutterboyShapes,
        typography = Typography,
        content = content,
    )
}

/**
 * Pure colour-scheme picker — split out so a Robolectric test can verify the
 * four-way pick logic without instantiating the full theme.
 */
@Composable
internal fun resolveBaseScheme(darkTheme: Boolean, baseTheme: BaseTheme): ColorScheme {
    val dynamicAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when (baseTheme) {
        BaseTheme.DefaultAndroid -> {
            if (dynamicAvailable) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) ShutterboyDarkColorScheme else ShutterboyLightColorScheme
            }
        }
        BaseTheme.DefaultColors ->
            if (darkTheme) ShutterboyDarkColorScheme else ShutterboyLightColorScheme
        BaseTheme.PureBlack -> {
            val foundation = if (dynamicAvailable) {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) ShutterboyDarkColorScheme else ShutterboyLightColorScheme
            }
            foundation.copy(background = Color.Black, surface = Color.Black)
        }
        is BaseTheme.Custom -> deriveCustomScheme(baseTheme.seedRgb, darkTheme)
    }
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
