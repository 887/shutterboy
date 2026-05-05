package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.sort.Direction
import com.eight87.shutterboy.domain.sort.PhotoSort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Phase E.5 — verify [DataStoreSortPreferences] write + read paths and
 * the per-folder fallback. DataStore-backed against a temporary file so
 * each test starts from a clean state. Robolectric only because the
 * DataStore preferences module pulls in Android types under the hood.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DataStoreSortPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: SortPreferences
    private val testScope = TestScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        val file = tempFolder.newFile("sort.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { file }
        prefs = DataStoreSortPreferences(dataStore)
    }

    @After
    fun tearDown() {
        // PreferenceDataStoreFactory ties the lifecycle to the scope; the
        // temp file is cleaned up automatically.
    }

    @Test
    fun `unset photos sort defaults to PhotoSort_Default`() = runTest(testScope.testScheduler) {
        assertEquals(PhotoSort.Default, prefs.observePhotosSort().first())
    }

    @Test
    fun `setPhotosSort persists and observePhotosSort emits the new value`() =
        runTest(testScope.testScheduler) {
            prefs.setPhotosSort(PhotoSort.ByName(Direction.DESC))
            assertEquals(PhotoSort.ByName(Direction.DESC), prefs.observePhotosSort().first())
        }

    @Test
    fun `setCollectionsSort is independent of photos sort`() =
        runTest(testScope.testScheduler) {
            prefs.setCollectionsSort(PhotoSort.BySize(Direction.ASC))
            prefs.setPhotosSort(PhotoSort.ByName(Direction.DESC))
            assertEquals(PhotoSort.BySize(Direction.ASC), prefs.observeCollectionsSort().first())
            assertEquals(PhotoSort.ByName(Direction.DESC), prefs.observePhotosSort().first())
        }

    @Test
    fun `folder sort falls back to collections sort when unset`() =
        runTest(testScope.testScheduler) {
            prefs.setCollectionsSort(PhotoSort.ByDateAdded(Direction.ASC))
            assertEquals(
                PhotoSort.ByDateAdded(Direction.ASC),
                prefs.observeFolderSort(FolderId(42L)).first(),
            )
        }

    @Test
    fun `folder sort overrides collections sort when set`() =
        runTest(testScope.testScheduler) {
            prefs.setCollectionsSort(PhotoSort.ByDateAdded(Direction.ASC))
            prefs.setFolderSort(FolderId(42L), PhotoSort.BySize(Direction.DESC))
            assertEquals(
                PhotoSort.BySize(Direction.DESC),
                prefs.observeFolderSort(FolderId(42L)).first(),
            )
            // A different folder still falls back.
            assertEquals(
                PhotoSort.ByDateAdded(Direction.ASC),
                prefs.observeFolderSort(FolderId(99L)).first(),
            )
        }

    @Test
    fun `unset everything yields Default at every read`() =
        runTest(testScope.testScheduler) {
            assertEquals(PhotoSort.Default, prefs.observePhotosSort().first())
            assertEquals(PhotoSort.Default, prefs.observeCollectionsSort().first())
            assertEquals(PhotoSort.Default, prefs.observeFolderSort(FolderId(1L)).first())
        }
}
