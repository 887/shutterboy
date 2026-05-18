package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.settings.BaseTheme
import com.eight87.shutterboy.data.settings.ThemeMode
import com.eight87.shutterboy.data.settings.ThemePreferences
import com.eight87.shutterboy.ui.settings.ColorPickerDialog
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import com.eight87.shutterboy.ui.settings.catalog.SettingsRowDivider
import kotlinx.coroutines.launch

/**
 * Appearance section — two rows matching tonearmboy's Look-and-Feel
 * shape:
 *
 *  - **Theme**: light/dark/auto override.
 *  - **Base theme**: Material You / brand palette / pure black /
 *    custom seed colour. Trailing swatch always shown — the resolved
 *    primary for the four canned options, the picked seed for Custom.
 *
 * Each row opens an [AlertDialog] picker. Custom in the Base-theme
 * picker hands off to [ColorPickerDialog] for the seed value.
 */
@Composable
fun AppearanceSection(
    themePreferences: ThemePreferences,
    modifier: Modifier = Modifier,
) {
    val baseTheme by themePreferences.observeBaseTheme()
        .collectAsStateWithLifecycle(initialValue = BaseTheme.Default)
    val themeMode by themePreferences.observeThemeMode()
        .collectAsStateWithLifecycle(initialValue = ThemeMode.Default)
    val scope = rememberCoroutineScope()
    var basePickerOpen by remember { mutableStateOf(false) }
    var colorPickerOpen by remember { mutableStateOf(false) }
    var modePickerOpen by remember { mutableStateOf(false) }

    val currentSeed = (baseTheme as? BaseTheme.Custom)?.seedRgb ?: DEFAULT_SEED_RGB
    val swatchColor = swatchFor(baseTheme)

    SettingsCard(
        modifier = modifier,
        title = stringResource(R.string.settings_section_appearance),
    ) {
        SettingsRow(
            id = "settings_appearance_theme_mode",
            icon = Icons.Outlined.DarkMode,
            label = stringResource(R.string.settings_appearance_theme),
            subtitle = themeModeLabel(themeMode),
            onClick = { modePickerOpen = true },
        )
        SettingsRowDivider()
        SettingsRow(
            id = "settings_appearance_base_theme",
            icon = Icons.Outlined.Palette,
            label = stringResource(R.string.settings_appearance_base_theme),
            subtitle = themeLabel(baseTheme),
            onClick = { basePickerOpen = true },
            trailing = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .semantics { testTag = "appearance_base_swatch" },
                )
            },
        )
    }

    if (modePickerOpen) {
        ThemeModePickerDialog(
            current = themeMode,
            onPick = { picked ->
                scope.launch { themePreferences.setThemeMode(picked) }
                modePickerOpen = false
            },
            onDismiss = { modePickerOpen = false },
        )
    }

    if (basePickerOpen) {
        BaseThemePickerDialog(
            current = baseTheme,
            onPick = { picked ->
                if (picked is BaseTheme.Custom) {
                    basePickerOpen = false
                    colorPickerOpen = true
                } else {
                    scope.launch { themePreferences.setBaseTheme(picked) }
                    basePickerOpen = false
                }
            },
            onDismiss = { basePickerOpen = false },
        )
    }

    if (colorPickerOpen) {
        ColorPickerDialog(
            initialRgb = currentSeed,
            onConfirm = { rgb ->
                scope.launch { themePreferences.setBaseTheme(BaseTheme.Custom(rgb)) }
                colorPickerOpen = false
            },
            onDismiss = { colorPickerOpen = false },
        )
    }
}

@Composable
private fun swatchFor(theme: BaseTheme): Color = when (theme) {
    is BaseTheme.Custom -> Color(0xFF000000L or theme.seedRgb)
    is BaseTheme.PureBlack -> Color.Black
    else -> MaterialTheme.colorScheme.primary
}

@Composable
private fun themeLabel(theme: BaseTheme): String = when (theme) {
    is BaseTheme.DefaultAndroid -> stringResource(R.string.settings_appearance_default_android)
    is BaseTheme.DefaultColors -> stringResource(R.string.settings_appearance_default_colors)
    is BaseTheme.PureBlack -> stringResource(R.string.settings_appearance_pure_black)
    is BaseTheme.Custom -> stringResource(R.string.settings_appearance_custom)
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> stringResource(R.string.settings_appearance_mode_system)
    ThemeMode.Light -> stringResource(R.string.settings_appearance_mode_light)
    ThemeMode.Dark -> stringResource(R.string.settings_appearance_mode_dark)
}

@Composable
private fun ThemeModePickerDialog(
    current: ThemeMode,
    onPick: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_appearance_theme)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { mode ->
                    PickerRow(
                        label = themeModeLabel(mode),
                        selected = mode == current,
                        onClick = { onPick(mode) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun BaseThemePickerDialog(
    current: BaseTheme,
    onPick: (BaseTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_appearance_base_theme)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                PickerRow(
                    label = stringResource(R.string.settings_appearance_default_android),
                    selected = current is BaseTheme.DefaultAndroid,
                    onClick = { onPick(BaseTheme.DefaultAndroid) },
                )
                PickerRow(
                    label = stringResource(R.string.settings_appearance_default_colors),
                    selected = current is BaseTheme.DefaultColors,
                    onClick = { onPick(BaseTheme.DefaultColors) },
                )
                PickerRow(
                    label = stringResource(R.string.settings_appearance_pure_black),
                    selected = current is BaseTheme.PureBlack,
                    onClick = { onPick(BaseTheme.PureBlack) },
                )
                PickerRow(
                    label = stringResource(R.string.settings_appearance_custom),
                    selected = current is BaseTheme.Custom,
                    onClick = { onPick(BaseTheme.Custom(DEFAULT_SEED_RGB)) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun PickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(vertical = SettingsDimens.RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.size(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val DEFAULT_SEED_RGB: Long = 0x6750A4L
