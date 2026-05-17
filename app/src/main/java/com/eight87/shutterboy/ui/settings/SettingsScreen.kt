package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RootTopBar
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.Settings
import com.eight87.shutterboy.ui.nav.SettingsAbout
import com.eight87.shutterboy.ui.nav.SettingsPhotos
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow
import com.eight87.shutterboy.ui.settings.sections.AppearanceSection
import com.eight87.shutterboy.ui.settings.sections.LibrarySection

/**
 * Settings tab body. M3 Expressive grouped-cards under a shared
 * [RootTopBar] so destination switching is uniform across the three
 * root screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            RootTopBar(
                current = Settings,
                onSelect = { dest -> scope.backStack.selectTab(dest) },
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
            LibrarySection(
                libraryScanner = scope.libraryScanner,
                snackbar = scope.snackbar,
            )
            SettingsCard {
                SettingsRow(
                    id = "settings_photos_root",
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.settings_root_photos_row_label),
                    subtitle = stringResource(R.string.settings_root_photos_row_subtitle),
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { scope.backStack.push(SettingsPhotos) },
                )
            }
            SettingsCard {
                SettingsRow(
                    id = "settings_about_root",
                    icon = Icons.Filled.Info,
                    label = stringResource(R.string.settings_root_about_row_label),
                    subtitle = stringResource(R.string.settings_root_about_row_subtitle),
                    trailing = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    onClick = { scope.backStack.push(SettingsAbout) },
                )
            }
        }
    }
}
