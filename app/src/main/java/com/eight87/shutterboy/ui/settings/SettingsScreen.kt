package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.ScanProgressStrip
import com.eight87.shutterboy.ui.nav.SettingsAbout
import com.eight87.shutterboy.ui.nav.SettingsLibrary
import com.eight87.shutterboy.ui.nav.SettingsLookAndFeel
import com.eight87.shutterboy.ui.nav.SettingsPhotos
import com.eight87.shutterboy.ui.nav.SettingsSearch
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import com.eight87.shutterboy.ui.settings.catalog.SettingsRowDivider
import com.eight87.shutterboy.ui.settings.catalog.SettingsSearchBar

/**
 * Settings root — tonearmboy-shape grouped cards under a pinned search
 * field. Each card carries a section title (Appearance / Library /
 * Photos / About) rendered in the M3 primary accent. The search icon
 * was dropped from the top bar in favour of the pinned bar — taps
 * still push the same [SettingsSearch] overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_settings)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.backStack.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cd_settings_back),
                            )
                        }
                    },
                )
                ScanProgressStrip(scanner = scope.libraryScanner)
            }
        },
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = SettingsDimens.CardSpacing),
            verticalArrangement = Arrangement.spacedBy(SettingsDimens.CardSpacing),
        ) {
            SettingsSearchBar(
                onOpen = { scope.backStack.push(SettingsSearch) },
                modifier = Modifier.padding(
                    start = SettingsDimens.PagePadding,
                    end = SettingsDimens.PagePadding,
                    top = 12.dp,
                ),
            )

            SettingsCard(
                title = stringResource(R.string.settings_section_appearance),
                modifier = Modifier.padding(horizontal = SettingsDimens.PagePadding),
            ) {
                SubPageRow(
                    id = "settings_lookfeel_root",
                    icon = Icons.Outlined.Palette,
                    label = stringResource(R.string.settings_root_lookfeel_row_label),
                    subtitle = stringResource(R.string.settings_root_lookfeel_row_subtitle),
                    onClick = { scope.backStack.push(SettingsLookAndFeel) },
                )
            }

            SettingsCard(
                title = stringResource(R.string.settings_section_library),
                modifier = Modifier.padding(horizontal = SettingsDimens.PagePadding),
            ) {
                SubPageRow(
                    id = "settings_library_root",
                    icon = Icons.Outlined.Storage,
                    label = stringResource(R.string.settings_root_library_row_label),
                    subtitle = stringResource(R.string.settings_root_library_row_subtitle),
                    onClick = { scope.backStack.push(SettingsLibrary) },
                )
                SettingsRowDivider()
                SubPageRow(
                    id = "settings_photos_root",
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.settings_root_photos_row_label),
                    subtitle = stringResource(R.string.settings_root_photos_row_subtitle),
                    onClick = { scope.backStack.push(SettingsPhotos) },
                )
            }

            SettingsCard(
                title = stringResource(R.string.settings_section_about),
                modifier = Modifier.padding(horizontal = SettingsDimens.PagePadding),
            ) {
                SubPageRow(
                    id = "settings_about_root",
                    icon = Icons.Filled.Info,
                    label = stringResource(R.string.settings_root_about_row_label),
                    subtitle = stringResource(R.string.settings_root_about_row_subtitle),
                    onClick = { scope.backStack.push(SettingsAbout) },
                )
            }
        }
    }
}

@Composable
private fun SubPageRow(
    id: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    SettingsRow(
        id = id,
        icon = icon,
        label = label,
        subtitle = subtitle,
        onClick = onClick,
    )
}
