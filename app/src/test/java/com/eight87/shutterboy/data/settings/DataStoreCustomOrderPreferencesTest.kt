package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.SmartAlbumId
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
 * Phase E.5 — verify [DataStoreCustomOrderPreferences] write + read paths
 * + the decode codecs that drop unknown / malformed entries silently.
 * Robolectric for DataStore's Android-typed module.
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

    // ---- Decoder edge cases (pure helpers exposed via companion) -----------

    @Test
    fun `decodeSmartAlbumOrder drops unknown tokens silently`() {
        val decoded = DataStoreCustomOrderPreferences.decodeSmartAlbumOrder(
            "favorites,unknown,recents,garbage",
        )
        assertEquals(listOf(SmartAlbumId.Favorites, SmartAlbumId.Recents), decoded)
    }

    @Test
    fun `decodeSmartAlbumOrder on null or blank yields empty`() {
        assertTrue(DataStoreCustomOrderPreferences.decodeSmartAlbumOrder(null).isEmpty())
        assertTrue(DataStoreCustomOrderPreferences.decodeSmartAlbumOrder("").isEmpty())
        assertTrue(DataStoreCustomOrderPreferences.decodeSmartAlbumOrder("   ").isEmpty())
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

    // ---- Round-trip write + read -------------------------------------------

    @Test
    fun `unset smart-album order yields empty`() = runTest(testScope.testScheduler) {
        assertTrue(prefs.observeSmartAlbumOrder().first().isEmpty())
    }

    @Test
    fun `setSmartAlbumOrder persists and observes`() = runTest(testScope.testScheduler) {
        val pinned = listOf(SmartAlbumId.Favorites, SmartAlbumId.Camera)
        prefs.setSmartAlbumOrder(pinned)
        assertEquals(pinned, prefs.observeSmartAlbumOrder().first())
    }

    @Test
    fun `setFolderOrder persists and observes`() = runTest(testScope.testScheduler) {
        val pinned = listOf(FolderId(7L), FolderId(42L), FolderId(1L))
        prefs.setFolderOrder(pinned)
        assertEquals(pinned, prefs.observeFolderOrder().first())
    }

    @Test
    fun `smart-album and folder orders are independent`() = runTest(testScope.testScheduler) {
        val albums = listOf(SmartAlbumId.Recents, SmartAlbumId.Camera)
        val folders = listOf(FolderId(99L), FolderId(7L))
        prefs.setSmartAlbumOrder(albums)
        prefs.setFolderOrder(folders)
        assertEquals(albums, prefs.observeSmartAlbumOrder().first())
        assertEquals(folders, prefs.observeFolderOrder().first())
    }

    @Test
    fun `setting a new order overwrites the previous one`() = runTest(testScope.testScheduler) {
        prefs.setFolderOrder(listOf(FolderId(1L), FolderId(2L)))
        prefs.setFolderOrder(listOf(FolderId(2L), FolderId(1L)))
        assertEquals(listOf(FolderId(2L), FolderId(1L)), prefs.observeFolderOrder().first())
    }
}
