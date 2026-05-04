package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.SettingsAbout

/**
 * Phase C.x — Settings tab placeholder body until Phase I builds the M3
 * Expressive grouped-cards root with pill search. For now this surfaces a
 * single navigable row that pushes [SettingsAbout] onto the stack — enough
 * to host the easter-egg + license + GitHub link without pretending the
 * whole settings tree exists yet.
 */
@Composable
fun SettingsScreen(
    scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        SettingsRootRow(
            label = stringResource(R.string.settings_root_about_row_label),
            subtitle = stringResource(R.string.settings_root_about_row_subtitle),
            onClick = { scope.backStack.push(SettingsAbout) },
        )
    }
}

@Composable
private fun SettingsRootRow(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
