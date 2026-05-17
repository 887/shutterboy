package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase I.3.b — verify [DataStoreSafSourcesPreferences] empty-default,
 * add / remove round-trip, idempotent re-add, idempotent remove of
 * absent URI, and `ScanConfigSource.safSourceUris` mirroring.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DataStoreSafSourcesPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: DataStoreSafSourcesPreferences
    private val testScope = TestScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        val file = tempFolder.newFile("saf_sources.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { file }
        prefs = DataStoreSafSourcesPreferences(dataStore)
    }

    @Test
    fun `unset sources yield empty set`() = runTest(testScope.testScheduler) {
        assertTrue(prefs.observeSources().first().isEmpty())
    }

    @Test
    fun `add persists and observes`() = runTest(testScope.testScheduler) {
        prefs.add("content://tree/a")
        prefs.add("content://tree/b")
        assertEquals(setOf("content://tree/a", "content://tree/b"), prefs.observeSources().first())
    }

    @Test
    fun `re-adding existing uri is a no-op`() = runTest(testScope.testScheduler) {
        prefs.add("content://tree/a")
        prefs.add("content://tree/a")
        assertEquals(setOf("content://tree/a"), prefs.observeSources().first())
    }

    @Test
    fun `remove drops the uri`() = runTest(testScope.testScheduler) {
        prefs.add("content://tree/a")
        prefs.add("content://tree/b")
        prefs.remove("content://tree/a")
        assertEquals(setOf("content://tree/b"), prefs.observeSources().first())
    }

    @Test
    fun `remove of absent uri is a no-op`() = runTest(testScope.testScheduler) {
        prefs.add("content://tree/a")
        prefs.remove("content://tree/does-not-exist")
        assertEquals(setOf("content://tree/a"), prefs.observeSources().first())
    }

    @Test
    fun `safSourceUris mirrors observeSources for ScanConfigSource consumers`() =
        runTest(testScope.testScheduler) {
            prefs.add("content://tree/a")
            assertEquals(setOf("content://tree/a"), prefs.safSourceUris.first())
            prefs.remove("content://tree/a")
            assertTrue(prefs.safSourceUris.first().isEmpty())
        }
}
