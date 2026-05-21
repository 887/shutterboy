package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eight87.shutterboy.R
import com.eight87.shutterboy.data.settings.ThumbnailQuality
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import com.eight87.shutterboy.ui.settings.catalog.SettingsRowDivider
import com.eight87.shutterboy.ui.settings.sections.AppearanceSection
import kotlinx.coroutines.launch

/**
 * Settings → Look and Feel sub-page (main.md I.2). Consolidates the
 * Theme picker (moved off the Settings root) with two new knobs:
 * default grid density (which [PhotosZoomLevel] the Photos timeline
 * boots into) and thumbnail quality (Coil request size at every tile).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLookAndFeelScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    // Display knobs (grid density + thumbnail quality) moved to
    // Settings → Photos, since they're about how photos render, not
    // about look-and-feel. This sub-page now hosts only Appearance.

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.settings_look_and_feel_title)) },
                navigationIcon = {
                    IconButton(onClick = { scope.backStack.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_settings_back),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = SettingsDimens.PagePadding,
                    end = SettingsDimens.PagePadding,
                    top = SettingsDimens.CardSpacing,
                    bottom = SettingsDimens.CardSpacing,
                ),
            verticalArrangement = Arrangement.spacedBy(SettingsDimens.CardSpacing),
        ) {
            AppearanceSection(themePreferences = scope.themePreferences)
        }
    }
}

@Composable
private fun <T> RadioPickerDialog(
    title: String,
    options: List<T>,
    current: T,
    labelOf: @Composable (T) -> String,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { opt ->
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(opt) }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = opt == current,
                            onClick = { onPick(opt) },
                        )
                        Text(
                            text = labelOf(opt),
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_lookfeel_close))
            }
        },
    )
}

private fun densityLabelRes(level: PhotosZoomLevel): Int = when (level) {
    PhotosZoomLevel.Items -> R.string.settings_lookfeel_density_items
    PhotosZoomLevel.Days -> R.string.settings_lookfeel_density_days
    PhotosZoomLevel.Months -> R.string.settings_lookfeel_density_months
    PhotosZoomLevel.Years -> R.string.settings_lookfeel_density_years
}

private fun qualityLabelRes(quality: ThumbnailQuality): Int = when (quality) {
    ThumbnailQuality.Low -> R.string.settings_lookfeel_quality_low
    ThumbnailQuality.Medium -> R.string.settings_lookfeel_quality_medium
    ThumbnailQuality.High -> R.string.settings_lookfeel_quality_high
}
