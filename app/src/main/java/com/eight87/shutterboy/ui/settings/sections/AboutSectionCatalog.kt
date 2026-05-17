package com.eight87.shutterboy.ui.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Numbers
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.SettingsAbout
import com.eight87.shutterboy.ui.settings.catalog.FlashRowController
import com.eight87.shutterboy.ui.settings.catalog.SettingsCatalogEntry

/**
 * R.F.14 — About sub-page catalog entries, co-located with the section.
 */
internal object AboutSectionCatalog {
    const val ID_ABOUT_VERSION = "settings_about_version"
    const val ID_ABOUT_LICENSE = "settings_about_license"
    const val ID_ABOUT_GITHUB = "settings_about_github"
    const val ID_ABOUT_LICENSES = "settings_about_licenses"

    fun entries(): List<SettingsCatalogEntry> = listOf(
        SettingsCatalogEntry(
            id = ID_ABOUT_VERSION,
            labelRes = R.string.about_version_label,
            subtitleRes = null,
            keywordsRes = R.string.settings_search_keywords_about_version,
            breadcrumbRes = R.string.settings_search_breadcrumb_about_version,
            icon = Icons.Filled.Numbers,
            navigate = { scope ->
                FlashRowController.flash(ID_ABOUT_VERSION)
                scope.backStack.push(SettingsAbout)
            },
        ),
        SettingsCatalogEntry(
            id = ID_ABOUT_LICENSE,
            labelRes = R.string.about_license_row_label,
            subtitleRes = R.string.about_license_row_value,
            keywordsRes = R.string.settings_search_keywords_about_license,
            breadcrumbRes = R.string.settings_search_breadcrumb_about_license,
            icon = Icons.AutoMirrored.Filled.Article,
            navigate = { scope ->
                FlashRowController.flash(ID_ABOUT_LICENSE)
                scope.backStack.push(SettingsAbout)
            },
        ),
        SettingsCatalogEntry(
            id = ID_ABOUT_GITHUB,
            labelRes = R.string.about_github_row_label,
            subtitleRes = R.string.about_github_row_value,
            keywordsRes = R.string.settings_search_keywords_about_github,
            breadcrumbRes = R.string.settings_search_breadcrumb_about_github,
            icon = Icons.Filled.Code,
            navigate = { scope ->
                FlashRowController.flash(ID_ABOUT_GITHUB)
                scope.backStack.push(SettingsAbout)
            },
        ),
        SettingsCatalogEntry(
            id = ID_ABOUT_LICENSES,
            labelRes = R.string.licenses_row_label,
            subtitleRes = R.string.licenses_row_supporting,
            keywordsRes = R.string.settings_search_keywords_about_licenses,
            breadcrumbRes = R.string.settings_search_breadcrumb_about_licenses,
            icon = Icons.AutoMirrored.Outlined.Article,
            navigate = { scope ->
                FlashRowController.flash(ID_ABOUT_LICENSES)
                scope.backStack.push(SettingsAbout)
            },
        ),
    )
}
