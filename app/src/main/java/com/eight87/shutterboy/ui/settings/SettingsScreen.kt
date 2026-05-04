package com.eight87.shutterboy.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.RouteScope

/**
 * Phase C.1 — placeholder body for the Settings tab. Phase I replaces with
 * the real M3 Expressive grouped-cards-with-pill-search root.
 */
@Composable
fun SettingsScreen(
    @Suppress("UNUSED_PARAMETER") scope: RouteScope,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.settings_placeholder_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
