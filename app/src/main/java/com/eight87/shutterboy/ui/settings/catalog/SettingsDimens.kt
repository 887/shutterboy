package com.eight87.shutterboy.ui.settings.catalog

import androidx.compose.ui.unit.dp

/**
 * Single source of truth for the M3 Expressive grouped-cards layout dimensions
 * shared across every settings surface (root + sub-pages + About). Keeping
 * these as top-level constants makes the "sitting in the middle" inset
 * (16 dp horizontal page padding) consistent and tunable from one place.
 */
object SettingsDimens {
    val PagePadding = 16.dp
    val CardCornerRadius = 16.dp
    val CardSpacing = 16.dp
    val RowVerticalPadding = 14.dp
    val RowHorizontalPadding = 16.dp
    val IconSize = 24.dp
    val IconLabelGap = 16.dp
    val GroupTitleTopPadding = 20.dp
    val GroupTitleBottomPadding = 8.dp
}
