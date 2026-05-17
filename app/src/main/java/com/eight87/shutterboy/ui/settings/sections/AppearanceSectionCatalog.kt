package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.settings.catalog.FlashRowController
import com.eight87.shutterboy.ui.settings.catalog.SettingsCatalogEntry

/**
 * R.F.14 — appearance / look-and-feel catalog entries, co-located with
 * the section that renders them. The aggregator at
 * `ui/settings/catalog/SettingsCatalog.kt` flattens these into the
 * single search-overlay list.
 */
internal object AppearanceSectionCatalog {
    const val ID_APPEARANCE_THEME = "settings_appearance_theme"

    fun entries(): List<SettingsCatalogEntry> = listOf(
        SettingsCatalogEntry(
            id = ID_APPEARANCE_THEME,
            labelRes = R.string.settings_appearance_theme,
            subtitleRes = null,
            keywordsRes = R.string.settings_search_keywords_theme,
            breadcrumbRes = R.string.settings_search_breadcrumb_appearance_theme,
            icon = Icons.Outlined.Palette,
            navigate = { _ ->
                FlashRowController.flash(ID_APPEARANCE_THEME)
                // Theme row lives on the Settings root; no push needed,
                // pop is handled by the caller.
            },
        ),
    )
}
