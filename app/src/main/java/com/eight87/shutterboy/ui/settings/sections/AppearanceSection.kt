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
import com.eight87.shutterboy.data.settings.ThemePreferences
import com.eight87.shutterboy.ui.settings.ColorPickerDialog
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import kotlinx.coroutines.launch

/**
 * Appearance section — single "Theme" row that opens a picker dialog
 * with the four [BaseTheme] options. The current pick is shown as the
 * row subtitle (e.g. "Material You (system)"). Tapping the Custom
 * option in the picker opens [ColorPickerDialog] for a seed colour.
 *
 * Modeled on tonearmboy's `SettingsLookAndFeelScreen` picker pattern —
 * the row is the menu point; the dialog is where the actual choice
 * happens.
 */
@Composable
fun AppearanceSection(
    themePreferences: ThemePreferences,
    modifier: Modifier = Modifier,
) {
    val baseTheme by themePreferences.observeBaseTheme()
        .collectAsStateWithLifecycle(initialValue = BaseTheme.Default)
    val scope = rememberCoroutineScope()
    var pickerOpen by remember { mutableStateOf(false) }
    var colorPickerOpen by remember { mutableStateOf(false) }

    val currentSeed = (baseTheme as? BaseTheme.Custom)?.seedRgb ?: DEFAULT_SEED_RGB

    SettingsCard(
        modifier = modifier,
        title = stringResource(R.string.settings_section_appearance),
    ) {
        SettingsRow(
            id = "settings_appearance_theme",
            icon = Icons.Outlined.Palette,
            label = stringResource(R.string.settings_appearance_theme),
            subtitle = themeLabel(baseTheme),
            onClick = { pickerOpen = true },
            trailing = if (baseTheme is BaseTheme.Custom) {
                {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000000L or currentSeed))
                            .semantics { testTag = "appearance_custom_swatch" },
                    )
                }
            } else null,
        )
    }

    if (pickerOpen) {
        ThemePickerDialog(
            current = baseTheme,
            onPick = { picked ->
                if (picked is BaseTheme.Custom) {
                    pickerOpen = false
                    colorPickerOpen = true
                } else {
                    scope.launch { themePreferences.setBaseTheme(picked) }
                    pickerOpen = false
                }
            },
            onDismiss = { pickerOpen = false },
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
private fun themeLabel(theme: BaseTheme): String = when (theme) {
    is BaseTheme.DefaultAndroid -> stringResource(R.string.settings_appearance_default_android)
    is BaseTheme.DefaultColors -> stringResource(R.string.settings_appearance_default_colors)
    is BaseTheme.PureBlack -> stringResource(R.string.settings_appearance_pure_black)
    is BaseTheme.Custom -> stringResource(R.string.settings_appearance_custom)
}

@Composable
private fun ThemePickerDialog(
    current: BaseTheme,
    onPick: (BaseTheme) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_appearance_theme)) },
        text = {
            Column {
                ThemeOptionRow(
                    label = stringResource(R.string.settings_appearance_default_android),
                    selected = current is BaseTheme.DefaultAndroid,
                    onClick = { onPick(BaseTheme.DefaultAndroid) },
                )
                ThemeOptionRow(
                    label = stringResource(R.string.settings_appearance_default_colors),
                    selected = current is BaseTheme.DefaultColors,
                    onClick = { onPick(BaseTheme.DefaultColors) },
                )
                ThemeOptionRow(
                    label = stringResource(R.string.settings_appearance_pure_black),
                    selected = current is BaseTheme.PureBlack,
                    onClick = { onPick(BaseTheme.PureBlack) },
                )
                ThemeOptionRow(
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
private fun ThemeOptionRow(
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
