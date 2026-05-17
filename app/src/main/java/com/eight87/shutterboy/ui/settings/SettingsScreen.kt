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
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.eight87.shutterboy.ui.nav.SettingsLibrary
import com.eight87.shutterboy.ui.nav.SettingsLookAndFeel
import com.eight87.shutterboy.ui.nav.SettingsPhotos
import com.eight87.shutterboy.ui.nav.SettingsSearch
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow

/**
 * Settings tab body. M3 Expressive grouped-cards under a shared
 * [RootTopBar] so destination switching is uniform across the three
 * root screens. Appearance stays inline (theme picker is a one-tap
 * affordance); Library / Photos / About push into sub-pages.
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
                RootTopBar(
                    current = Settings,
                    onSelect = { dest -> scope.backStack.selectTab(dest) },
                    actions = {
                        IconButton(onClick = { scope.backStack.push(SettingsSearch) }) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = stringResource(
                                    R.string.settings_search_open_cd,
                                ),
                            )
                        }
                    },
                )
                com.eight87.shutterboy.ui.nav.ScanProgressStrip(
                    scanner = scope.libraryScanner,
                )
            }
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
            SettingsCard {
                SubPageRow(
                    id = "settings_lookfeel_root",
                    icon = Icons.Outlined.Palette,
                    label = stringResource(R.string.settings_root_lookfeel_row_label),
                    subtitle = stringResource(R.string.settings_root_lookfeel_row_subtitle),
                    onClick = { scope.backStack.push(SettingsLookAndFeel) },
                )
                SubPageRow(
                    id = "settings_library_root",
                    icon = Icons.Outlined.Storage,
                    label = stringResource(R.string.settings_root_library_row_label),
                    subtitle = stringResource(R.string.settings_root_library_row_subtitle),
                    onClick = { scope.backStack.push(SettingsLibrary) },
                )
                SubPageRow(
                    id = "settings_photos_root",
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.settings_root_photos_row_label),
                    subtitle = stringResource(R.string.settings_root_photos_row_subtitle),
                    onClick = { scope.backStack.push(SettingsPhotos) },
                )
            }
            SettingsCard {
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
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}
