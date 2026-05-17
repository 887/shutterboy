package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eight87.shutterboy.domain.FolderId
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
 * Verify [DataStoreCustomOrderPreferences] write + read paths + the
 * decoder that drops malformed entries silently. Robolectric for
 * DataStore's Android-typed module.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DataStoreCustomOrderPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: CustomOrderPreferences
    private val testScope = TestScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        val file = tempFolder.newFile("custom_order.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { file }
        prefs = DataStoreCustomOrderPreferences(dataStore)
    }

    @Test
    fun `decodeFolderOrder drops malformed Long tokens silently`() {
        val decoded = DataStoreCustomOrderPreferences.decodeFolderOrder("42,abc,17,garbage,3")
        assertEquals(listOf(FolderId(42L), FolderId(17L), FolderId(3L)), decoded)
    }

    @Test
    fun `decodeFolderOrder on null or blank yields empty`() {
        assertTrue(DataStoreCustomOrderPreferences.decodeFolderOrder(null).isEmpty())
        assertTrue(DataStoreCustomOrderPreferences.decodeFolderOrder("").isEmpty())
    }

    @Test
    fun `unset folder order yields empty`() = runTest(testScope.testScheduler) {
        assertTrue(prefs.observeFolderOrder().first().isEmpty())
    }

    @Test
    fun `setFolderOrder persists and observes`() = runTest(testScope.testScheduler) {
        val pinned = listOf(FolderId(7L), FolderId(42L), FolderId(1L))
        prefs.setFolderOrder(pinned)
        assertEquals(pinned, prefs.observeFolderOrder().first())
    }

    @Test
    fun `setting a new order overwrites the previous one`() = runTest(testScope.testScheduler) {
        prefs.setFolderOrder(listOf(FolderId(1L), FolderId(2L)))
        prefs.setFolderOrder(listOf(FolderId(2L), FolderId(1L)))
        assertEquals(listOf(FolderId(2L), FolderId(1L)), prefs.observeFolderOrder().first())
    }
}
