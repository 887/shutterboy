package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase incremental-scan C.1 — round-trip + edge cases for the cold-
 * start scan gate persistence. Verifies generation tokens are
 * per-volume, SAF fingerprint encode/decode handles malformed input,
 * and `clear()` wipes both surfaces (manual override path).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DataStoreScanGatePreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: ScanGatePreferences
    private val testScope = TestScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        val file = tempFolder.newFile("scan_gate.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { file }
        prefs = DataStoreScanGatePreferences(dataStore)
    }

    @Test
    fun `unset generation token observes as null`() = runTest(testScope.testScheduler) {
        assertNull(prefs.observeMediaStoreGeneration("external_primary").first())
    }

    @Test
    fun `set generation token persists and observes`() = runTest(testScope.testScheduler) {
        prefs.setMediaStoreGeneration("external_primary", 12345L)
        assertEquals(12345L, prefs.observeMediaStoreGeneration("external_primary").first())
    }

    @Test
    fun `generation tokens are per-volume`() = runTest(testScope.testScheduler) {
        prefs.setMediaStoreGeneration("external_primary", 100L)
        prefs.setMediaStoreGeneration("external_sd", 200L)
        assertEquals(100L, prefs.observeMediaStoreGeneration("external_primary").first())
        assertEquals(200L, prefs.observeMediaStoreGeneration("external_sd").first())
    }

    @Test
    fun `unset SAF fingerprint observes as empty map`() = runTest(testScope.testScheduler) {
        assertEquals(emptyMap<String, Int>(), prefs.observeSafFingerprint().first())
    }

    @Test
    fun `SAF fingerprint round-trips a populated map`() = runTest(testScope.testScheduler) {
        val map = mapOf(
            "content://com.android.externalstorage.documents/tree/primary%3APictures" to 42,
            "content://com.android.externalstorage.documents/tree/primary%3ADownload" to 7,
        )
        prefs.setSafFingerprint(map)
        assertEquals(map, prefs.observeSafFingerprint().first())
    }

    @Test
    fun `SAF fingerprint round-trips an empty map`() = runTest(testScope.testScheduler) {
        prefs.setSafFingerprint(mapOf("uri" to 5))
        prefs.setSafFingerprint(emptyMap())
        assertEquals(emptyMap<String, Int>(), prefs.observeSafFingerprint().first())
    }

    @Test
    fun `clear wipes both surfaces`() = runTest(testScope.testScheduler) {
        prefs.setMediaStoreGeneration("external_primary", 999L)
        prefs.setSafFingerprint(mapOf("uri" to 3))
        prefs.clear()
        assertNull(prefs.observeMediaStoreGeneration("external_primary").first())
        assertEquals(emptyMap<String, Int>(), prefs.observeSafFingerprint().first())
    }

    @Test
    fun `decodeSafFingerprint drops malformed entries silently`() {
        val encoded = "uri1=10;;uri2=notanumber;uri3=;=42;uri4=5"
        val decoded = DataStoreScanGatePreferences.decodeSafFingerprint(encoded)
        // uri1=10 and uri4=5 are well-formed; the rest are dropped.
        assertEquals(mapOf("uri1" to 10, "uri4" to 5), decoded)
    }

    @Test
    fun `decodeSafFingerprint handles null and empty`() {
        assertEquals(emptyMap<String, Int>(), DataStoreScanGatePreferences.decodeSafFingerprint(null))
        assertEquals(emptyMap<String, Int>(), DataStoreScanGatePreferences.decodeSafFingerprint(""))
    }
}
