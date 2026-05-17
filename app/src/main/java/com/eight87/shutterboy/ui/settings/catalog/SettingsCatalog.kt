package com.eight87.shutterboy.ui.settings.catalog

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.ui.graphics.vector.ImageVector
import com.eight87.shutterboy.R
import com.eight87.shutterboy.ui.nav.Licenses
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.nav.SettingsAbout
import com.eight87.shutterboy.ui.nav.SettingsLibrary
import com.eight87.shutterboy.ui.nav.SettingsPhotos

/**
 * I.7 — One settings entry. The catalog is the single source of truth for
 * the search overlay: every row reachable from the Settings root has a
 * matching entry, and the overlay filters this list.
 *
 * The runtime UI continues to render rows via the Settings sub-page
 * composables directly; the catalog's job is to feed search + provide the
 * `navigate` lambda that takes the user to the row's hosting sub-page.
 * The matched row is flashed via [LocalHighlightedSettingId].
 *
 * Pre-Manage-sources: the SAF source manager row is not yet shipped, so
 * it's deliberately absent from the catalog. Adding it is one entry here +
 * its sub-page row, no further plumbing.
 */
data class SettingsCatalogEntry(
    /** Stable identifier — matches the `id` on the rendered [SettingsRow]
     *  so the flash highlight knows which row to brighten. */
    val id: String,
    @StringRes val labelRes: Int,
    @StringRes val subtitleRes: Int?,
    @StringRes val keywordsRes: Int?,
    /** Translated breadcrumb string (e.g. "Look and Feel · Theme") shown
     *  beneath the label on each search result. */
    @StringRes val breadcrumbRes: Int,
    val icon: ImageVector,
    /** Navigate to the sub-page that hosts this row and seed the flash
     *  state via [LocalHighlightedSettingId]. */
    val navigate: (RouteScope) -> Unit,
)

/**
 * I.7 — Catalog of every settings row searchable from the Settings
 * overlay. Each entry knows how to navigate to its sub-page and which
 * row id to flash on arrival. Adding a setting is one entry here plus
 * a binding on the corresponding sub-page (the [SettingsRow]
 * `id` parameter ties the two together).
 */
object SettingsCatalog {

    /** Look-and-feel → Theme picker (row lives on the Settings root). */
    const val ID_APPEARANCE_THEME = "settings_appearance_theme"
    const val ID_LIBRARY_RESCAN = "settings_library_rescan"
    const val ID_LIBRARY_CLEAR_CACHE = "settings_library_clear_cache"
    const val ID_PHOTOS_DEFAULT_SORT = "settings_photos_default_sort"
    const val ID_ABOUT_VERSION = "settings_about_version"
    const val ID_ABOUT_LICENSE = "settings_about_license"
    const val ID_ABOUT_GITHUB = "settings_about_github"
    const val ID_ABOUT_LICENSES = "settings_about_licenses"

    fun entries(): List<SettingsCatalogEntry> = listOf(
        SettingsCatalogEntry(
            id = ID_APPEARANCE_THEME,
            labelRes = R.string.settings_appearance_theme,
            subtitleRes = null,
            keywordsRes = R.string.settings_search_keywords_theme,
            breadcrumbRes = R.string.settings_search_breadcrumb_appearance_theme,
            icon = Icons.Outlined.Palette,
            navigate = { scope ->
                FlashRowController.flash(ID_APPEARANCE_THEME)
                // Theme row lives on the Settings root; no push needed,
                // pop is handled by the caller.
            },
        ),
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

    /**
     * I.7 — filter entries by case-insensitive substring match against
     * label + subtitle + keywords. Empty / blank query returns the full
     * list (the overlay can render that as a "browse all settings" view).
     * Resource-resolution is the caller's job: pass a [resolve] that
     * looks up the matching strings via `Context.getString`.
     */
    fun filter(
        query: String,
        resolve: (Int) -> String,
        entries: List<SettingsCatalogEntry> = entries(),
    ): List<SettingsCatalogEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return entries
        return entries.filter { entry ->
            val label = resolve(entry.labelRes).lowercase()
            val subtitle = entry.subtitleRes?.let { resolve(it).lowercase() } ?: ""
            val keywords = entry.keywordsRes?.let { resolve(it).lowercase() } ?: ""
            label.contains(q) || subtitle.contains(q) || keywords.contains(q)
        }
    }
}
