package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eight87.shutterboy.data.settings.RecentSearchesPreferences.Companion.MAX_RECENT_SEARCHES
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Concrete [RecentSearchesPreferences] backed by `DataStore<Preferences>`.
 * Encoding: a `kotlinx.serialization` JSON array of strings stored under
 * the [RecentSearchesKey] preference. Most-recent-first; max
 * [MAX_RECENT_SEARCHES] entries, deduped case-sensitively (recording an
 * existing entry promotes it to the head).
 *
 * Malformed JSON decodes to an empty list — corruption is silently
 * recovered rather than thrown to UI consumers.
 */
internal class DataStoreRecentSearchesPreferences(
    private val dataStore: DataStore<Preferences>,
) : RecentSearchesPreferences {

    override fun observe(): Flow<List<String>> = dataStore.data
        .map { prefs -> decode(prefs[RecentSearchesKey]) }

    override suspend fun record(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { prefs ->
            val current = decode(prefs[RecentSearchesKey])
            val promoted = promote(current, trimmed)
            prefs[RecentSearchesKey] = encode(promoted)
        }
    }

    companion object {
        private val RecentSearchesKey = stringPreferencesKey("recent_searches")
        private val ListStringSerializer = ListSerializer(String.serializer())
        private val json = Json { ignoreUnknownKeys = true }

        internal fun decode(stored: String?): List<String> {
            if (stored.isNullOrBlank()) return emptyList()
            return try {
                json.decodeFromString(ListStringSerializer, stored)
            } catch (_: SerializationException) {
                emptyList()
            } catch (_: IllegalArgumentException) {
                emptyList()
            }
        }

        internal fun encode(entries: List<String>): String =
            json.encodeToString(ListStringSerializer, entries)

        /**
         * Insert [query] at the head of [current], removing any prior
         * occurrence (case-sensitive dedupe) and trimming the result to
         * [MAX_RECENT_SEARCHES] entries.
         */
        internal fun promote(current: List<String>, query: String): List<String> {
            val withoutDup = current.filterNot { it == query }
            return (listOf(query) + withoutDup).take(MAX_RECENT_SEARCHES)
        }
    }
}
