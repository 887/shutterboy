package com.eight87.shutterboy.ui.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * oss-licenses Phase C.1 — catalog-discipline test.
 *
 * Reads the real Licensee-generated `assets/licenses/artifacts.json` via the
 * Robolectric [Application]'s [android.content.res.AssetManager], parses it
 * through [parseLicensesCatalog] (the same decoder [LicensesScreen] uses at
 * runtime), and enforces:
 *
 *  - The catalog is non-empty.
 *  - Every entry carries an SPDX id from the allowlist.
 *  - Every entry's SPDX has a backing license-text asset under
 *    `app/src/main/assets/licenses/<spdx>.txt` (verified by listing the
 *    assets directory, not by guessing).
 *  - Known shipping samples are present so a build that silently drops a
 *    `implementation` dep from the inventory will fail loud.
 *
 * If a future dep brings in a new SPDX id, the allowlist + the backing text
 * asset both need updating (see `CLAUDE.md` "Licenses" subhead).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LicensesCatalogTest {

    private val allowedSpdx = setOf(
        "Apache-2.0",
        "MIT",
        "BSD-2-Clause",
        "BSD-3-Clause",
    )

    private val requiredSamples = setOf(
        "io.coil-kt.coil3:coil-compose",
        "androidx.exifinterface:exifinterface",
        "androidx.room:room-runtime",
    )

    @Test
    fun `catalog is non-empty, SPDX-allowlisted, asset-backed, and contains known samples`() {
        val app: Application = ApplicationProvider.getApplicationContext()
        val assets = app.assets

        val catalogJson = assets.open("licenses/artifacts.json").use {
            it.bufferedReader().readText()
        }

        // Use the production decoder so this test fails the moment the wire
        // shape and the decoder drift. The resolver returns null for every
        // SPDX — we verify asset backing separately below via assets.list().
        val entries = parseLicensesCatalog(catalogJson) { null }

        assertTrue("catalog should be non-empty", entries.isNotEmpty())

        // Asset directory listing — the source of truth for which SPDX texts
        // actually ship in the APK.
        val licenseAssetNames = assets.list("licenses").orEmpty().toSet()
        val shippedSpdx = licenseAssetNames
            .filter { it.endsWith(".txt") }
            .map { it.removeSuffix(".txt") }
            .toSet()

        entries.forEach { entry ->
            val spdx = entry.spdxId
            assertTrue(
                "SPDX '$spdx' on ${entry.groupId}:${entry.artifactId} is not in the allowlist " +
                    "$allowedSpdx — either add it to licensee.allow(...) and ship " +
                    "assets/licenses/$spdx.txt, or document the exemption.",
                spdx in allowedSpdx,
            )
            assertTrue(
                "SPDX '$spdx' on ${entry.groupId}:${entry.artifactId} has no backing text " +
                    "asset at assets/licenses/$spdx.txt (shipped: $shippedSpdx)",
                spdx in shippedSpdx,
            )
        }

        val presentKeys = entries.map { "${it.groupId}:${it.artifactId}" }.toSet()
        requiredSamples.forEach { sample ->
            assertTrue(
                "expected '$sample' in the catalog — did a shipping dep silently drop out " +
                    "of the Licensee inventory? Present keys: ${presentKeys.size}.",
                sample in presentKeys,
            )
        }
    }
}
