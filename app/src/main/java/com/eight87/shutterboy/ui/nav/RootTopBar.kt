package com.eight87.shutterboy.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.R

/**
 * Shared top-bar for the three root destinations (Photos, Collections,
 * Settings). The destination switcher lives here — three icon buttons
 * to the right of the title, with the selected one rendered as a
 * tinted [FilledTonalIconButton] pill (same visual language as M3's
 * NavigationBar selected indicator). Per-screen action icons (search,
 * sort, overflow) follow the destination row via the [actions] slot.
 *
 * Replaces the previous bottom [androidx.compose.material3.NavigationBar]
 * — the destination switcher is now top-aligned alongside the search
 * affordance, modelled after the sibling app `strictlykeptboy`'s
 * `ShellTopBar`.
 */
@Composable
fun RootTopBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Title takes the leftover space; truncates first so the
            // destination switcher stays anchored to the right edge even
            // when per-screen actions are wide (Photos = Search+Sort vs
            // Collections = MoreVert vs Settings = SettingsSearch).
            Text(
                text = stringResource(rootLabelRes(current)),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Per-screen actions sit BEFORE the destination switcher so
            // the three nav icons (Photos / Collections / Settings) stay
            // in fixed rightmost positions across every root screen.
            // Settings is always the far-right icon — feels like tabs,
            // not buttons that jump screen-to-screen.
            actions()
            rootDestinations.forEach { dest ->
                DestinationButton(
                    dest = dest,
                    selected = dest == current,
                    onClick = { onSelect(dest) },
                )
            }
        }
    }
}

@Composable
private fun DestinationButton(
    dest: Destination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val (icon, cd) = rootIconAndCd(dest, selected)
    val mod = Modifier.semantics { contentDescription = cd }
    if (selected) {
        FilledTonalIconButton(onClick = onClick, modifier = mod) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    } else {
        IconButton(onClick = onClick, modifier = mod) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun rootLabelRes(dest: Destination): Int = when (dest) {
    Photos -> R.string.nav_photos
    Collections -> R.string.nav_collections
    Settings -> R.string.nav_settings
    else -> error("$dest is not a root destination")
}

@Composable
private fun rootIconAndCd(dest: Destination, selected: Boolean): Pair<ImageVector, String> = when (dest) {
    Photos -> (if (selected) Icons.Filled.Image else Icons.Outlined.Image) to
        stringResource(R.string.cd_nav_photos)
    Collections -> (if (selected) Icons.Filled.Collections else Icons.Outlined.Collections) to
        stringResource(R.string.cd_nav_collections)
    Settings -> (if (selected) Icons.Filled.Settings else Icons.Outlined.Settings) to
        stringResource(R.string.cd_nav_settings)
    else -> error("$dest is not a root destination")
}
