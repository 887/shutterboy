package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Sell
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.SettingsSmartTags
import com.eight87.shutterboy.ui.settings.catalog.FlashRowController
import com.eight87.shutterboy.ui.settings.catalog.SettingsCatalogEntry

/**
 * Smart Tags sub-page catalog entries, co-located with the section
 * (R.F.14 pattern). Both rows live on [SettingsSmartTags]; the search
 * overlay pushes that page and flashes the matched row.
 */
internal object SmartTagsSectionCatalog {
    const val ID_SMARTTAGS_ENABLE = "settings_smarttags_enable"
    const val ID_SMARTTAGS_CAPTIONS = "settings_smarttags_captions"

    fun entries(): List<SettingsCatalogEntry> = listOf(
        SettingsCatalogEntry(
            id = ID_SMARTTAGS_ENABLE,
            labelRes = R.string.settings_smarttags_enable_label,
            subtitleRes = R.string.settings_smarttags_enable_subtitle,
            keywordsRes = R.string.settings_search_keywords_smarttags_enable,
            breadcrumbRes = R.string.settings_search_breadcrumb_smarttags,
            icon = Icons.Outlined.Sell,
            navigate = { scope ->
                FlashRowController.flash(ID_SMARTTAGS_ENABLE)
                scope.backStack.push(SettingsSmartTags)
            },
        ),
        SettingsCatalogEntry(
            id = ID_SMARTTAGS_CAPTIONS,
            labelRes = R.string.settings_smarttags_captions_label,
            subtitleRes = R.string.settings_smarttags_captions_subtitle,
            keywordsRes = R.string.settings_search_keywords_smarttags_captions,
            breadcrumbRes = R.string.settings_search_breadcrumb_smarttags,
            icon = Icons.Outlined.Description,
            navigate = { scope ->
                FlashRowController.flash(ID_SMARTTAGS_CAPTIONS)
                scope.backStack.push(SettingsSmartTags)
            },
        ),
    )
}
