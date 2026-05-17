package com.eight87.shutterboy.ui.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.eight87.shutterboy.ui.settings.catalog.SettingsCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * I.8 — Catalog wiring discipline.
 *
 * Every entry must:
 *   - have a unique id (no collisions between two entries),
 *   - carry a non-null `navigate` lambda (the row is reachable),
 *   - resolve its label / subtitle / keywords / breadcrumb string
 *     resources to non-empty strings (no dangling `@StringRes`).
 *
 * Reads the real `app/src/main/res/values/strings.xml` via the Robolectric
 * Application, mirroring how `LicensesCatalogTest` exercises the real
 * assets directory.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsCatalogTest {

    @Test
    fun `every entry has a unique id`() {
        val entries = SettingsCatalog.entries()
        val ids = entries.map { it.id }
        assertEquals(
            "Duplicate ids in SettingsCatalog: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}",
            ids.size,
            ids.toSet().size,
        )
    }

    @Test
    fun `every entry has a non-null navigate lambda`() {
        SettingsCatalog.entries().forEach { entry ->
            assertNotNull(
                "Entry ${entry.id} has a null navigate lambda",
                entry.navigate,
            )
        }
    }

    @Test
    fun `every entry has resolvable string resources`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        SettingsCatalog.entries().forEach { entry ->
            val label = app.getString(entry.labelRes)
            assertTrue("Empty label for ${entry.id}", label.isNotBlank())
            val breadcrumb = app.getString(entry.breadcrumbRes)
            assertTrue("Empty breadcrumb for ${entry.id}", breadcrumb.isNotBlank())
            entry.subtitleRes?.let {
                val subtitle = app.getString(it)
                assertTrue("Empty subtitle for ${entry.id}", subtitle.isNotBlank())
            }
            entry.keywordsRes?.let {
                val keywords = app.getString(it)
                assertTrue("Empty keywords for ${entry.id}", keywords.isNotBlank())
            }
        }
    }

    @Test
    fun `catalog covers every known stable id`() {
        // Pin the expected id set so accidentally removing an entry trips
        // the test. Add to the expected set when a row legitimately
        // arrives or departs.
        val expectedIds = setOf(
            SettingsCatalog.ID_APPEARANCE_THEME,
            SettingsCatalog.ID_LIBRARY_RESCAN,
            SettingsCatalog.ID_LIBRARY_CLEAR_CACHE,
            SettingsCatalog.ID_PHOTOS_DEFAULT_SORT,
            SettingsCatalog.ID_ABOUT_VERSION,
            SettingsCatalog.ID_ABOUT_LICENSE,
            SettingsCatalog.ID_ABOUT_GITHUB,
            SettingsCatalog.ID_ABOUT_LICENSES,
        )
        val actualIds = SettingsCatalog.entries().map { it.id }.toSet()
        assertEquals(expectedIds, actualIds)
    }
}
