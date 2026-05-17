package com.eight87.shutterboy.ui.settings.catalog

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eight87.shutterboy.theme.CategoryAccent
import com.eight87.shutterboy.theme.accentFor
import kotlinx.coroutines.delay

/**
 * One row inside a [SettingsCard]. Always has a leading icon (the Android
 * Settings convention is "no orphan rows without icons") + a label + an
 * optional subtitle. The trailing slot hosts the affordance — `Switch`,
 * navigation chevron, picker indicator, etc.
 *
 * Tap the whole row when [onClick] is supplied; rows without a click target
 * (informational rows like the Open-source acknowledgments placeholder) skip
 * the [Modifier.clickable] entirely.
 *
 * m3-expressive Phase D — when [accent] or [id] is supplied, the leading
 * glyph is wrapped in a [CategoryAvatar] (40-dp coloured circle). Per
 * m3-expressive Finding 4, the auto-accent fallback fires here at the
 * row composable, not at every call site: pass `id` and the catalog
 * binding picks up the colour without per-row wiring. Rows that opt
 * out of the avatar (neither [accent] nor [id]) render the old
 * monochrome glyph for callers that haven't migrated yet.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    id: String? = null,
    accent: CategoryAccent? = null,
) {
    val resolvedAccent: CategoryAccent? = accent ?: id?.let { accentFor(it) }

    // I.7 — settings-search flash highlight. The overlay seeds
    // FlashRowController.state with the matched row's id before popping
    // itself; the receiving sub-page composes shortly after and any row
    // whose id matches briefly tints its background.
    val highlightState = LocalHighlightedSettingId.current
    val highlighted = id != null && highlightState.value == id
    val target = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val animatedBg by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 300),
        label = "settings_row_highlight",
    )
    if (highlighted) {
        LaunchedEffect(id) {
            // Hold the flash for ~300ms past peak, then clear so a later
            // navigation to the same id re-fires.
            delay(600)
            if (highlightState.value == id) highlightState.value = null
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(animatedBg)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(
                horizontal = SettingsDimens.RowHorizontalPadding,
                vertical = SettingsDimens.RowVerticalPadding,
            )
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (resolvedAccent != null) {
            CategoryAvatar(
                icon = icon,
                accent = resolvedAccent,
                contentDescription = null,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(SettingsDimens.IconSize),
            )
        }
        Spacer(Modifier.size(SettingsDimens.IconLabelGap))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // m3-expressive C.5 — title is titleMedium, subtitle is bodyMedium
            // + onSurfaceVariant. M3E settings rows read at the higher type
            // weight than baseline-M3's titleSmall / bodySmall.
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
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

