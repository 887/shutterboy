package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
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

/**
 * Round-trip read/write for each [BaseTheme] variant through a real
 * DataStore-backed [DataStoreThemePreferences] against a temporary
 * file. Robolectric because the preferences module pulls in Android
 * types under the hood.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DataStoreThemePreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: ThemePreferences
    private val testScope = TestScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        val file = tempFolder.newFile("theme.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { file }
        prefs = DataStoreThemePreferences(dataStore)
    }

    @Test
    fun `unset base theme defaults to Default`() = runTest(testScope.testScheduler) {
        assertEquals(BaseTheme.Default, prefs.observeBaseTheme().first())
    }

    @Test
    fun `DefaultAndroid round-trips through DataStore`() = runTest(testScope.testScheduler) {
        prefs.setBaseTheme(BaseTheme.DefaultAndroid)
        assertEquals(BaseTheme.DefaultAndroid, prefs.observeBaseTheme().first())
    }

    @Test
    fun `PureBlack round-trips through DataStore`() = runTest(testScope.testScheduler) {
        prefs.setBaseTheme(BaseTheme.PureBlack)
        assertEquals(BaseTheme.PureBlack, prefs.observeBaseTheme().first())
    }

    @Test
    fun `tint color round-trips through DataStore`() = runTest(testScope.testScheduler) {
        prefs.setTintColor(0xB94A1AL)
        assertEquals(0xB94A1AL, prefs.observeTintColor().first())
    }

    @Test
    fun `clearing tint color writes null`() = runTest(testScope.testScheduler) {
        prefs.setTintColor(0x123456L)
        prefs.setTintColor(null)
        assertEquals(null, prefs.observeTintColor().first())
    }

    @Test
    fun `Custom seed round-trips through DataStore`() = runTest(testScope.testScheduler) {
        prefs.setBaseTheme(BaseTheme.Custom(0xB94A1AL))
        assertEquals(BaseTheme.Custom(0xB94A1AL), prefs.observeBaseTheme().first())
    }

    @Test
    fun `setting a new base theme overwrites the previous one`() =
        runTest(testScope.testScheduler) {
            prefs.setBaseTheme(BaseTheme.PureBlack)
            prefs.setBaseTheme(BaseTheme.DefaultAndroid)
            assertEquals(BaseTheme.DefaultAndroid, prefs.observeBaseTheme().first())
        }
}
