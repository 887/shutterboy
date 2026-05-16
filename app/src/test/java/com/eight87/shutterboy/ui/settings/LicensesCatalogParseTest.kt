package com.eight87.shutterboy.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JUnit parse test for the Licensee `artifacts.json` payload. Verifies
 * the parser projects the wire shape onto the [LicenseEntry] domain shape,
 * sorts by `groupId:artifactId`, picks up the first SPDX identifier, and
 * surfaces a null `licenseText` when the resolver does not recognise the
 * SPDX id.
 */
class LicensesCatalogParseTest {

    private val fixture = """
        [
          {
            "groupId": "androidx.activity",
            "artifactId": "activity-compose",
            "version": "1.13.0",
            "name": "Activity Compose",
            "spdxLicenses": [
              { "identifier": "Apache-2.0", "name": "Apache License 2.0" }
            ]
          },
          {
            "groupId": "androidx.exifinterface",
            "artifactId": "exifinterface",
            "version": "1.4.1",
            "spdxLicenses": [
              { "identifier": "Apache-2.0", "name": "Apache License 2.0" }
            ]
          },
          {
            "groupId": "com.example",
            "artifactId": "weird",
            "version": "0.0.1",
            "spdxLicenses": []
          }
        ]
    """.trimIndent()

    @Test
    fun `parses fixture into the expected entries`() {
        val entries = parseLicensesCatalog(fixture) { spdx ->
            if (spdx == "Apache-2.0") "FAKE-APACHE-BODY" else null
        }

        assertEquals(3, entries.size)
        // Sorted by groupId:artifactId — activity comes before exifinterface
        // which comes before com.example (lexicographic on the joined key).
        assertEquals("androidx.activity:activity-compose",
            "${entries[0].groupId}:${entries[0].artifactId}")
        assertEquals("androidx.exifinterface:exifinterface",
            "${entries[1].groupId}:${entries[1].artifactId}")
        assertEquals("com.example:weird",
            "${entries[2].groupId}:${entries[2].artifactId}")

        // Field projection — version + spdx + licenseText.
        val activity = entries[0]
        assertEquals("1.13.0", activity.version)
        assertEquals("Apache-2.0", activity.spdxId)
        assertEquals("FAKE-APACHE-BODY", activity.licenseText)

        // Entry with no spdxLicenses falls back to UNKNOWN + null body.
        val weird = entries[2]
        assertEquals("UNKNOWN", weird.spdxId)
        assertNull(weird.licenseText)
    }

    @Test
    fun `ignores unknown JSON fields`() {
        val withExtras = """
            [
              {
                "groupId": "g",
                "artifactId": "a",
                "version": "1",
                "spdxLicenses": [{ "identifier": "MIT", "name": "MIT", "url": "x" }],
                "scm": { "url": "https://example.com" },
                "unexpected": 42
              }
            ]
        """.trimIndent()

        val entries = parseLicensesCatalog(withExtras) { null }
        assertEquals(1, entries.size)
        assertEquals("MIT", entries[0].spdxId)
    }

    @Test
    fun `empty array parses to empty list`() {
        val entries = parseLicensesCatalog("[]") { null }
        assertTrue(entries.isEmpty())
    }
}
