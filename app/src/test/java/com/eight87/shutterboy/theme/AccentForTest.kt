package com.eight87.shutterboy.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * m3-expressive Phase D — pure-JUnit contract test for [accentFor].
 *
 * Three invariants:
 *   1. Deterministic — same id maps to the same accent across calls.
 *   2. Spread — across 100 sample ids no single accent dominates more
 *      than 40 %, confirming the hash distributes across the 5-accent
 *      palette.
 *   3. Theme-aware — the dark variant differs from the light variant
 *      for the same id.
 */
class AccentForTest {

    @Test
    fun `same id maps to same accent across calls`() {
        val ids = listOf(
            "settings_about_version",
            "settings_library_manage_sources",
            "settings_photos_grid_density",
            "settings_albums_reorder",
            "settings_lookfeel_theme",
        )
        for (id in ids) {
            assertEquals(accentFor(id, isDark = true), accentFor(id, isDark = true))
            assertEquals(accentFor(id, isDark = false), accentFor(id, isDark = false))
        }
    }

    @Test
    fun `100 sample ids spread across the 5-accent palette`() {
        val counts = IntArray(DarkCategoryPalette.size)
        for (i in 0 until 100) {
            val id = "row_$i"
            val accent = accentFor(id, isDark = true)
            val idx = DarkCategoryPalette.indexOf(accent)
            assertTrue("accent for $id not in palette", idx >= 0)
            counts[idx]++
        }
        val max = counts.max()
        assertTrue(
            "expected no accent to dominate >40% of 100 ids; counts=${counts.toList()}",
            max <= 40,
        )
        // And every accent should show up at least once across 100 samples.
        for ((i, c) in counts.withIndex()) {
            assertTrue("accent $i never picked in 100 samples", c > 0)
        }
    }

    @Test
    fun `dark and light variants return different colours for same id`() {
        val ids = listOf("settings_about_github", "settings_photos_grid_density", "x", "abc123")
        for (id in ids) {
            val dark = accentFor(id, isDark = true)
            val light = accentFor(id, isDark = false)
            assertNotEquals("dark/light container collision for id=$id", dark.container, light.container)
            assertNotEquals("dark/light onContainer collision for id=$id", dark.onContainer, light.onContainer)
        }
    }
}
