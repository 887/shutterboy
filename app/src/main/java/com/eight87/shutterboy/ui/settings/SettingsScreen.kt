package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.SettingsAbout
import com.eight87.shutterboy.ui.settings.catalog.SettingsCard
import com.eight87.shutterboy.ui.settings.catalog.SettingsDimens
import com.eight87.shutterboy.ui.settings.catalog.SettingsRow

/**
 * Phase C.x — Settings tab body. M3 Expressive grouped-cards shape mirroring
 * tonearmboy's settings root: grouped rounded cards "sitting in the middle"
 * of the page. Phase I builds out the full catalog (Look and Feel / Library
 * / Photos / Albums / Viewer / About sections); for now we surface a single
 * About card with one row that pushes [SettingsAbout] onto the stack.
 *
 * The `SettingsCard` + `SettingsRow` primitives in
 * [com.eight87.shutterboy.ui.settings.catalog] are the reusable surfaces every
 * Phase I sub-page will consume.
 */
@Composable
fun SettingsScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = SettingsDimens.PagePadding,
                end = SettingsDimens.PagePadding,
                top = SettingsDimens.CardSpacing,
                bottom = SettingsDimens.CardSpacing,
            ),
    ) {
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
