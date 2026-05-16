package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch

/**
 * Appearance section card — surfaces the four [BaseTheme] options with
 * inline radio-button affordances. The fourth option (Custom seed
 * colour) opens [ColorPickerDialog]; the currently picked seed is
 * surfaced as a small swatch trailing the row when Custom is active.
 *
 * Reads the current value from [ThemePreferences.observeBaseTheme] and
 * writes back through [ThemePreferences.setBaseTheme] inside a coroutine
 * scope tied to the composable's lifecycle.
 */
@Composable
fun AppearanceSection(
    themePreferences: ThemePreferences,
    modifier: Modifier = Modifier,
) {
    val baseTheme by themePreferences.observeBaseTheme()
        .collectAsStateWithLifecycle(initialValue = BaseTheme.Default)
    val scope = rememberCoroutineScope()
    var colorPickerOpen by remember { mutableStateOf(false) }

    // Match the stored value against the four picker buckets. A stored
    // `Custom(rgb)` (whatever the seed) maps to the Custom row.
    val storedIsCustom = baseTheme is BaseTheme.Custom
    val storedSeed = (baseTheme as? BaseTheme.Custom)?.seedRgb ?: DEFAULT_SEED_RGB

    SettingsCard(
        modifier = modifier,
        title = stringResource(R.string.settings_section_appearance),
    ) {
        ThemeRadioRow(
            label = stringResource(R.string.settings_appearance_default_android),
            selected = baseTheme is BaseTheme.DefaultAndroid,
            onClick = { scope.launch { themePreferences.setBaseTheme(BaseTheme.DefaultAndroid) } },
            testTagName = "appearance_default_android",
        )
        ThemeRadioRow(
            label = stringResource(R.string.settings_appearance_default_colors),
            selected = baseTheme is BaseTheme.DefaultColors,
            onClick = { scope.launch { themePreferences.setBaseTheme(BaseTheme.DefaultColors) } },
            testTagName = "appearance_default_colors",
        )
        ThemeRadioRow(
            label = stringResource(R.string.settings_appearance_pure_black),
            selected = baseTheme is BaseTheme.PureBlack,
            onClick = { scope.launch { themePreferences.setBaseTheme(BaseTheme.PureBlack) } },
            testTagName = "appearance_pure_black",
        )
        // Custom row — tapping opens the colour picker. Selecting it
        // commits the picked RGB; the trailing swatch surfaces the
        // current seed (or the placeholder default) at a glance.
        ThemeRadioRow(
            label = stringResource(R.string.settings_appearance_custom),
            selected = storedIsCustom,
            onClick = { colorPickerOpen = true },
            testTagName = "appearance_custom",
            trailing = if (storedIsCustom) {
                {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000000L or storedSeed))
                            .semantics { testTag = "appearance_custom_swatch" },
                    )
                }
            } else null,
        )
    }

    if (colorPickerOpen) {
        ColorPickerDialog(
            initialRgb = storedSeed,
            onConfirm = { rgb ->
                scope.launch { themePreferences.setBaseTheme(BaseTheme.Custom(rgb)) }
                colorPickerOpen = false
            },
            onDismiss = { colorPickerOpen = false },
        )
    }
}

/**
 * One BaseTheme picker row. Falls through to [SettingsRow] for the
 * shared icon + label + spacing shape, with a [RadioButton] trailing
 * affordance reflecting the selection state. The whole row is
 * clickable; tap commits the pick (or opens the colour picker for the
 * Custom row).
 */
@Composable
private fun ThemeRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTagName: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(
                horizontal = SettingsDimens.RowHorizontalPadding,
                vertical = SettingsDimens.RowVerticalPadding,
            )
            .semantics { testTag = testTagName },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icons.Outlined.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(SettingsDimens.IconSize),
        )
        Spacer(Modifier.size(SettingsDimens.IconLabelGap))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
            Spacer(Modifier.size(12.dp))
        }
        RadioButton(selected = selected, onClick = null)
    }
}

// Material 3 default purple seed — the placeholder when the user
// hasn't picked anything yet.
private const val DEFAULT_SEED_RGB: Long = 0x6750A4L
