package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.SettingsPhotos
import com.eight87.shutterboy.ui.settings.catalog.FlashRowController
import com.eight87.shutterboy.ui.settings.catalog.SettingsCatalogEntry

/**
 * R.F.14 — Photos sub-page catalog entries, co-located with the section.
 */
internal object PhotosSectionCatalog {
    const val ID_PHOTOS_DEFAULT_SORT = "settings_photos_default_sort"

    fun entries(): List<SettingsCatalogEntry> = listOf(
        SettingsCatalogEntry(
            id = ID_PHOTOS_DEFAULT_SORT,
            labelRes = R.string.settings_photos_default_sort_label,
            subtitleRes = null,
            keywordsRes = R.string.settings_search_keywords_photos_sort,
            breadcrumbRes = R.string.settings_search_breadcrumb_photos_sort,
            icon = Icons.AutoMirrored.Outlined.Sort,
            navigate = { scope ->
                FlashRowController.flash(ID_PHOTOS_DEFAULT_SORT)
                scope.backStack.push(SettingsPhotos)
            },
        ),
    )
}
