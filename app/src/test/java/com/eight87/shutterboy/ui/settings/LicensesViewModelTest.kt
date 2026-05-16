package com.eight87.shutterboy.ui.settings

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric test for [LicensesViewModel]. Reads the real
 * `app/src/main/assets/licenses/artifacts.json` produced by the Licensee
 * plugin and asserts the VM yields a non-empty, well-formed inventory.
 *
 * Phase A's pre-build step copies the generated JSON into `assets/licenses/`
 * before this test ever runs (`mergeDebugAssets` depends on
 * `copyLicenseeInventory`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LicensesViewModelTest {

    @Test
    fun `loads the bundled artifacts catalog and resolves SPDX ids`() {
        val app: android.app.Application = ApplicationProvider.getApplicationContext()
        val vm = LicensesViewModel(app.assets)
        val entries = vm.entries.value

        assertTrue("catalog should be non-empty", entries.isNotEmpty())
        // Every entry has the projected fields populated.
        entries.forEach { entry ->
            assertTrue("groupId blank for $entry", entry.groupId.isNotBlank())
            assertTrue("artifactId blank for $entry", entry.artifactId.isNotBlank())
            assertTrue("version blank for $entry", entry.version.isNotBlank())
            assertTrue("spdxId blank for $entry", entry.spdxId.isNotBlank())
        }
        // Sorted by groupId:artifactId.
        val keys = entries.map { "${it.groupId}:${it.artifactId}" }
        assertTrue("entries should be sorted", keys == keys.sorted())
        // Apache-2.0 is universal across the resolved classpath; assert at
        // least one entry resolved its SPDX text.
        val resolved = entries.firstOrNull { it.licenseText != null }
        assertNotNull("expected at least one entry with resolved licenseText", resolved)
    }
}
