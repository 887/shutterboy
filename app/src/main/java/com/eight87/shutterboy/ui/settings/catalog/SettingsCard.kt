package com.eight87.shutterboy.ui.settings.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Material 3 card hosting a group of related rows. Renders with
 * [SettingsDimens.CardCornerRadius] corners and a tonal `surfaceContainer`
 * background so it visually separates from the page background — the
 * "sitting in the middle" pattern from Android system Settings.
 *
 * @param title Optional small section header rendered above the card. When
 *   set, it takes the same horizontal inset as the card itself so the
 *   labels line up with each row's icon column.
 */
@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(
                    start = SettingsDimens.RowHorizontalPadding,
                    top = SettingsDimens.GroupTitleTopPadding,
                    bottom = SettingsDimens.GroupTitleBottomPadding,
                ),
            )
        }
        Card(
            shape = RoundedCornerShape(SettingsDimens.CardCornerRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column { content() }
        }
    }
}
