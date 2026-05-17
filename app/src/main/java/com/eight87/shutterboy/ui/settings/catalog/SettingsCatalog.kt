package com.eight87.shutterboy.ui.settings.catalog

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.eight87.shutterboy.ui.nav.RouteScope
import com.eight87.shutterboy.ui.settings.sections.AboutSectionCatalog
import com.eight87.shutterboy.ui.settings.sections.AppearanceSectionCatalog
import com.eight87.shutterboy.ui.settings.sections.LibrarySectionCatalog
import com.eight87.shutterboy.ui.settings.sections.PhotosSectionCatalog

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
 * I.7 / R.F.14 — aggregator over the per-section catalog files in
 * `ui/settings/sections/`. The catalog itself is intentionally tiny: each
 * section owns its own list, this object just `flatten`s. Adding a new
 * section is one file under `sections/` plus one line in [entries].
 *
 * The `ID_*` constants are re-exposed here for the `SettingsCatalogTest`
 * pin-down assertion, which trips whenever a row legitimately arrives or
 * departs. The shipping source of truth for each id is the section file.
 */
object SettingsCatalog {

    /** Look-and-feel → Theme picker (row lives on the Settings root). */
    const val ID_APPEARANCE_THEME = AppearanceSectionCatalog.ID_APPEARANCE_THEME
    const val ID_LIBRARY_RESCAN = LibrarySectionCatalog.ID_LIBRARY_RESCAN
    const val ID_LIBRARY_CLEAR_CACHE = LibrarySectionCatalog.ID_LIBRARY_CLEAR_CACHE
    const val ID_PHOTOS_DEFAULT_SORT = PhotosSectionCatalog.ID_PHOTOS_DEFAULT_SORT
    const val ID_ABOUT_VERSION = AboutSectionCatalog.ID_ABOUT_VERSION
    const val ID_ABOUT_LICENSE = AboutSectionCatalog.ID_ABOUT_LICENSE
    const val ID_ABOUT_GITHUB = AboutSectionCatalog.ID_ABOUT_GITHUB
    const val ID_ABOUT_LICENSES = AboutSectionCatalog.ID_ABOUT_LICENSES

    fun entries(): List<SettingsCatalogEntry> =
        AppearanceSectionCatalog.entries() +
            LibrarySectionCatalog.entries() +
            PhotosSectionCatalog.entries() +
            AboutSectionCatalog.entries()

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
