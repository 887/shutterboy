package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CleaningServices
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.SettingsLibrary
import com.eight87.shutterboy.ui.settings.catalog.FlashRowController
import com.eight87.shutterboy.ui.settings.catalog.SettingsCatalogEntry

/**
 * R.F.14 — Library sub-page catalog entries, co-located with the section.
 */
internal object LibrarySectionCatalog {
    const val ID_LIBRARY_RESCAN = "settings_library_rescan"
    const val ID_LIBRARY_CLEAR_CACHE = "settings_library_clear_cache"

    fun entries(): List<SettingsCatalogEntry> = listOf(
        SettingsCatalogEntry(
            id = ID_LIBRARY_RESCAN,
            labelRes = R.string.settings_library_rescan,
            subtitleRes = R.string.settings_library_rescan_subtitle,
            keywordsRes = R.string.settings_search_keywords_library_rescan,
            breadcrumbRes = R.string.settings_search_breadcrumb_library_rescan,
            icon = Icons.Filled.Refresh,
            navigate = { scope ->
                FlashRowController.flash(ID_LIBRARY_RESCAN)
                scope.backStack.push(SettingsLibrary)
            },
        ),
        SettingsCatalogEntry(
            id = ID_LIBRARY_CLEAR_CACHE,
            labelRes = R.string.settings_library_clear_cache,
            subtitleRes = R.string.settings_library_clear_cache_subtitle,
            keywordsRes = R.string.settings_search_keywords_library_clear_cache,
            breadcrumbRes = R.string.settings_search_breadcrumb_library_clear_cache,
            icon = Icons.Outlined.CleaningServices,
            navigate = { scope ->
                FlashRowController.flash(ID_LIBRARY_CLEAR_CACHE)
                scope.backStack.push(SettingsLibrary)
            },
        ),
    )
}
