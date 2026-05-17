package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eight87.shutterboy.ui.photos.grid.PhotosZoomLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DataStoreDisplayPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: DisplayPreferences
    private val testScope = TestScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        val file = tempFolder.newFile("display.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { file }
        prefs = DataStoreDisplayPreferences(dataStore)
    }

    @Test
    fun `unset density yields Items default`() = runTest(testScope.testScheduler) {
        assertEquals(PhotosZoomLevel.Items, prefs.observeDefaultGridDensity().first())
    }

    @Test
    fun `unset quality yields Medium default`() = runTest(testScope.testScheduler) {
        assertEquals(ThumbnailQuality.Medium, prefs.observeThumbnailQuality().first())
    }

    @Test
    fun `density round-trips for every level`() = runTest(testScope.testScheduler) {
        for (level in PhotosZoomLevel.entries) {
            prefs.setDefaultGridDensity(level)
            assertEquals(level, prefs.observeDefaultGridDensity().first())
        }
    }

    @Test
    fun `quality round-trips for every variant`() = runTest(testScope.testScheduler) {
        for (q in ThumbnailQuality.entries) {
            prefs.setThumbnailQuality(q)
            assertEquals(q, prefs.observeThumbnailQuality().first())
        }
    }

    @Test
    fun `decodeDensity falls back to Items on malformed`() {
        assertEquals(PhotosZoomLevel.Items, DataStoreDisplayPreferences.decodeDensity("garbage"))
        assertEquals(PhotosZoomLevel.Items, DataStoreDisplayPreferences.decodeDensity(null))
    }

    @Test
    fun `decodeQuality falls back to Medium on malformed`() {
        assertEquals(ThumbnailQuality.Medium, DataStoreDisplayPreferences.decodeQuality("garbage"))
        assertEquals(ThumbnailQuality.Medium, DataStoreDisplayPreferences.decodeQuality(null))
    }
}
