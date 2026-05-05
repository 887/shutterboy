package com.eight87.shutterboy.ui.settings.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * One row inside a [SettingsCard]. Always has a leading icon (the Android
 * Settings convention is "no orphan rows without icons") + a label + an
 * optional subtitle. The trailing slot hosts the affordance — `Switch`,
 * navigation chevron, picker indicator, etc.
 *
 * Tap the whole row when [onClick] is supplied; rows without a click target
 * (informational rows like the Open-source acknowledgments placeholder) skip
 * the [Modifier.clickable] entirely.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(
                horizontal = SettingsDimens.RowHorizontalPadding,
                vertical = SettingsDimens.RowVerticalPadding,
            )
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(SettingsDimens.IconSize),
        )
        Spacer(Modifier.size(SettingsDimens.IconLabelGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.size(8.dp))
            trailing()
        }
    }
}

/**
 * Subtle in-card divider between consecutive [SettingsRow]s. Inset to align
 * with the row labels (skips the icon gutter) so the dividers read as
 * row-separators rather than full-card splits.
 */
@Composable
fun SettingsRowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(
            start = SettingsDimens.RowHorizontalPadding +
                SettingsDimens.IconSize +
                SettingsDimens.IconLabelGap,
            end = SettingsDimens.RowHorizontalPadding,
        ),
    )
}
