package com.eight87.shutterboy.ui.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.ui.settings.catalog.SettingsCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * I.8 — Catalog filter contract.
 *
 * Empty query returns every entry; non-empty query case-insensitively
 * substring-matches against label, subtitle, and keywords. Reads the
 * real strings.xml via Robolectric so the keyword resources are
 * exercised end-to-end.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsSearchFilterTest {

    private fun app(): Application = ApplicationProvider.getApplicationContext()

    private fun filter(query: String) =
        SettingsCatalog.filter(
            query = query,
            resolve = { resId -> app().getString(resId) },
        )

    @Test
    fun `empty query returns the full catalog`() {
        val all = SettingsCatalog.entries()
        assertEquals(all.size, filter("").size)
        assertEquals(all.size, filter("   ").size)
    }

    @Test
    fun `rescan matches the library rescan row`() {
        val hits = filter("rescan").map { it.id }
        assertTrue(
            "rescan should match the Library rescan entry, got $hits",
            hits.contains(SettingsCatalog.ID_LIBRARY_RESCAN),
        )
    }

    @Test
    fun `filtering is case-insensitive`() {
        val lower = filter("theme").map { it.id }
        val upper = filter("THEME").map { it.id }
        val mixed = filter("ThEmE").map { it.id }
        assertEquals(lower, upper)
        assertEquals(lower, mixed)
        assertTrue(lower.contains(SettingsCatalog.ID_APPEARANCE_THEME))
    }

    @Test
    fun `matches against subtitle text`() {
        // "frees disk space" is in the clear-cache subtitle.
        val hits = filter("frees disk").map { it.id }
        assertTrue(
            "subtitle search should hit clear-cache, got $hits",
            hits.contains(SettingsCatalog.ID_LIBRARY_CLEAR_CACHE),
        )
    }

    @Test
    fun `matches against keyword synonyms`() {
        // "github" keyword on About → GitHub row; also exercise a less
        // obvious one — "spdx" lives in the licenses keyword bag.
        val gh = filter("github").map { it.id }
        assertTrue(gh.contains(SettingsCatalog.ID_ABOUT_GITHUB))
        val spdx = filter("spdx").map { it.id }
        assertTrue(spdx.contains(SettingsCatalog.ID_ABOUT_LICENSES))
    }

    @Test
    fun `no matches returns empty list`() {
        assertTrue(filter("nonexistentXYZQQ").isEmpty())
    }
}
