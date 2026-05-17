package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.eight87.shutterboy.data.settings.RecentSearchesPreferences.Companion.MAX_RECENT_SEARCHES
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
 * Verify [DataStoreRecentSearchesPreferences] round-trip, dedupe-promote,
 * max-10 trim, blank-on-record no-op, and corrupt-JSON tolerance.
 * Robolectric for DataStore's Android-typed module.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [33])
class DataStoreRecentSearchesPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: RecentSearchesPreferences
    private val testScope = TestScope(StandardTestDispatcher())

    @Before
    fun setUp() {
        val file = tempFolder.newFile("recent_searches.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = testScope) { file }
        prefs = DataStoreRecentSearchesPreferences(dataStore)
    }

    @Test
    fun `decode on null or blank yields empty`() {
        assertTrue(DataStoreRecentSearchesPreferences.decode(null).isEmpty())
        assertTrue(DataStoreRecentSearchesPreferences.decode("").isEmpty())
        assertTrue(DataStoreRecentSearchesPreferences.decode("   ").isEmpty())
    }

    @Test
    fun `decode on malformed JSON yields empty`() {
        assertTrue(DataStoreRecentSearchesPreferences.decode("{not json").isEmpty())
        assertTrue(DataStoreRecentSearchesPreferences.decode("[1, 2, 3]").isEmpty())
        assertTrue(DataStoreRecentSearchesPreferences.decode("\"plain string\"").isEmpty())
    }

    @Test
    fun `unset recent searches yields empty`() = runTest(testScope.testScheduler) {
        assertTrue(prefs.observe().first().isEmpty())
    }

    @Test
    fun `record persists and observes most-recent-first`() = runTest(testScope.testScheduler) {
        prefs.record("alpha")
        prefs.record("bravo")
        prefs.record("charlie")
        assertEquals(listOf("charlie", "bravo", "alpha"), prefs.observe().first())
    }

    @Test
    fun `record dedupes by promoting existing entry to head`() = runTest(testScope.testScheduler) {
        prefs.record("alpha")
        prefs.record("bravo")
        prefs.record("charlie")
        prefs.record("alpha")
        assertEquals(listOf("alpha", "charlie", "bravo"), prefs.observe().first())
    }

    @Test
    fun `record trims to MAX_RECENT_SEARCHES`() = runTest(testScope.testScheduler) {
        repeat(MAX_RECENT_SEARCHES + 5) { i -> prefs.record("q$i") }
        val observed = prefs.observe().first()
        assertEquals(MAX_RECENT_SEARCHES, observed.size)
        // Most-recent first: last write is at the head.
        assertEquals("q${MAX_RECENT_SEARCHES + 4}", observed.first())
        // Oldest survivor is q5 (q0..q4 dropped beyond the cap).
        assertEquals("q5", observed.last())
    }

    @Test
    fun `record blank query is a no-op`() = runTest(testScope.testScheduler) {
        prefs.record("alpha")
        prefs.record("")
        prefs.record("   ")
        assertEquals(listOf("alpha"), prefs.observe().first())
    }

    @Test
    fun `record trims whitespace before persisting`() = runTest(testScope.testScheduler) {
        prefs.record("  alpha  ")
        assertEquals(listOf("alpha"), prefs.observe().first())
    }

    @Test
    fun `promote treats identical query as in-place head move`() {
        val out = DataStoreRecentSearchesPreferences.promote(
            current = listOf("a", "b", "c"),
            query = "b",
        )
        assertEquals(listOf("b", "a", "c"), out)
    }

    @Test
    fun `encode-decode round-trips a non-trivial list`() {
        val list = listOf("vacation 2024", "münchen", "\"quoted\"", "a,b,c")
        val encoded = DataStoreRecentSearchesPreferences.encode(list)
        assertEquals(list, DataStoreRecentSearchesPreferences.decode(encoded))
    }
}
