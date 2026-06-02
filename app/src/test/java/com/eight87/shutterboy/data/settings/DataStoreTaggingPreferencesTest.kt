package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Smart Tags D.3 — [DataStoreTaggingPreferences] defaults + write/read paths.
 * Labeling defaults ON, captions default OFF; both round-trip independently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DataStoreTaggingPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: TaggingPreferences
    private val testScope = TestScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        val file = tempFolder.newFile("tagging.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { file }
        prefs = DataStoreTaggingPreferences(dataStore)
    }

    @Test
    fun `labeling defaults on, captions default off`() = runTest(testScope.testScheduler) {
        assertTrue(prefs.observeEnabled().first())
        assertFalse(prefs.observeCaptionsEnabled().first())
    }

    @Test
    fun `enabled round-trips`() = runTest(testScope.testScheduler) {
        prefs.setEnabled(false)
        assertFalse(prefs.observeEnabled().first())
        prefs.setEnabled(true)
        assertTrue(prefs.observeEnabled().first())
    }

    @Test
    fun `captions toggle is independent of master toggle`() = runTest(testScope.testScheduler) {
        prefs.setCaptionsEnabled(true)
        prefs.setEnabled(false)
        assertTrue(prefs.observeCaptionsEnabled().first())
        assertFalse(prefs.observeEnabled().first())
    }
}
