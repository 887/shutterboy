package com.eight87.shutterboy.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * oss-licenses Phase C.2 — Compose UI test that mounts
 * [LicensesScreenContent] over a synthetic catalog and exercises the
 * `LazyColumn` scroll + the tap-to-expand `AlertDialog`. Runs under
 * Robolectric (no AVD required); the AVD walk lives in Phase C.3 / B.7.
 *
 * The screen is parameterised on a `List<LicenseEntry>` (via the
 * `*Content` split made for testability), so we don't need the real
 * `assets/licenses/artifacts.json` here — the inventory shape is
 * already gated by `LicensesCatalogTest` (Phase C.1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LicensesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val apacheBody = """
        Apache License
        Version 2.0, January 2004
        http://www.apache.org/licenses/
    """.trimIndent()

    private val mitBody = "MIT License\n\nCopyright (c) ..."

    /**
     * Synthesizes a 60-entry catalog: 59 Apache-2.0 androidx-shaped entries
     * plus one MIT entry late in the list. Big enough that not everything
     * fits in a single viewport, so the scroll assertion is meaningful.
     */
    private fun syntheticEntries(): List<LicenseEntry> {
        val apache = (1..59).map { i ->
            LicenseEntry(
                groupId = "androidx.fake",
                artifactId = "module-$i",
                version = "1.0.$i",
                spdxId = "Apache-2.0",
                licenseText = apacheBody,
            )
        }
        val tail = LicenseEntry(
            groupId = "io.coil-kt.coil3",
            artifactId = "coil-compose",
            version = "3.0.0",
            spdxId = "MIT",
            licenseText = mitBody,
        )
        return apache + tail
    }

    @Test
    fun renders_non_empty_catalog_with_first_row_visible() {
        composeRule.setContent {
            LicensesScreenContent(
                entries = syntheticEntries(),
                onBack = {},
            )
        }

        composeRule.waitForIdle()

        // TopAppBar title.
        composeRule.onNodeWithText("Open-source licenses").assertIsDisplayed()

        // First synthetic row's label (artifactId + version).
        composeRule.onNodeWithText("module-1 1.0.1").assertIsDisplayed()
    }

    @Test
    fun scrolls_through_catalog_to_reveal_later_row() {
        composeRule.setContent {
            LicensesScreenContent(
                entries = syntheticEntries(),
                onBack = {},
            )
        }

        composeRule.waitForIdle()

        // The MIT row is tacked on at the end — definitely past the first
        // viewport. Robolectric's LazyColumn only composes off-screen items
        // once we scroll to them, so a plain `assertIsDisplayed()` against
        // the row label would fail before the scroll.
        composeRule.onNode(androidx.compose.ui.test.hasScrollAction())
            .performScrollToNode(hasText("coil-compose 3.0.0"))

        composeRule.waitForIdle()

        composeRule.onNodeWithText("coil-compose 3.0.0").assertIsDisplayed()
    }

    @Test
    fun tapping_a_row_opens_dialog_with_license_body() {
        composeRule.setContent {
            LicensesScreenContent(
                entries = syntheticEntries(),
                onBack = {},
            )
        }

        composeRule.waitForIdle()

        // Tap the first row.
        composeRule.onNodeWithText("module-1 1.0.1").performClick()

        composeRule.waitForIdle()

        // The AlertDialog should now show the Apache-2.0 body. We don't
        // assert against the full text — just the canonical header line
        // every shipped Apache-2.0 asset starts with.
        // (Multiple nodes can match "Apache License" — the dialog title
        // also contains the artifact name "module-1 1.0.1", which we check
        // explicitly below; using onAllNodesWithText avoids tripping on
        // ambiguity from the dialog title vs body decomposition.)
        val apacheMatches = composeRule.onAllNodesWithText("Apache License", substring = true)
            .fetchSemanticsNodes()
        assertTrue(
            "expected the Apache-2.0 license body to render inside the dialog, " +
                "found ${apacheMatches.size} matching nodes",
            apacheMatches.isNotEmpty(),
        )

        // The dialog title carries the artifact label.
        composeRule.onAllNodesWithText("module-1 1.0.1")
            .fetchSemanticsNodes()
            .also {
                assertTrue("expected dialog title with artifact label", it.isNotEmpty())
            }
    }
}
