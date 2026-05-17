package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Phase J.2 — concrete [SlideshowPreferences] backed by the shared
 * `shutterboy_settings` DataStore. Defaults `kenBurnsEnabled` to `true`
 * so a fresh install picks up the slow zoom; the toggle on
 * `SettingsPhotosScreen` writes through here.
 */
internal class DataStoreSlideshowPreferences(
    private val dataStore: DataStore<Preferences>,
) : SlideshowPreferences {

    override fun observeKenBurnsEnabled(): Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[KenBurnsEnabledKey] ?: DEFAULT_KEN_BURNS_ENABLED }

    override suspend fun setKenBurnsEnabled(value: Boolean) {
        dataStore.edit { it[KenBurnsEnabledKey] = value }
    }

    companion object {
        internal val KenBurnsEnabledKey = booleanPreferencesKey("slideshow_ken_burns")
        internal const val DEFAULT_KEN_BURNS_ENABLED: Boolean = true
    }
}
