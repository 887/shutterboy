package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete [SafSourcesPreferences] backed by `DataStore<Preferences>`.
 * Stores the set of SAF tree URIs under [SafTreeUrisKey] as a native
 * `Set<String>` preference. Default (unset) reads as the empty set.
 */
internal class DataStoreSafSourcesPreferences(
    private val dataStore: DataStore<Preferences>,
) : SafSourcesPreferences, ScanConfigSource {

    override fun observeSources(): Flow<Set<String>> = dataStore.data
        .map { prefs -> prefs[SafTreeUrisKey] ?: emptySet() }

    override val safSourceUris: Flow<Set<String>> = observeSources()

    override suspend fun add(uri: String) {
        dataStore.edit { prefs ->
            val current = prefs[SafTreeUrisKey] ?: emptySet()
            if (uri !in current) prefs[SafTreeUrisKey] = current + uri
        }
    }

    override suspend fun remove(uri: String) {
        dataStore.edit { prefs ->
            val current = prefs[SafTreeUrisKey] ?: return@edit
            if (uri in current) prefs[SafTreeUrisKey] = current - uri
        }
    }

    companion object {
        internal val SafTreeUrisKey = stringSetPreferencesKey("saf_tree_uris")
    }
}
