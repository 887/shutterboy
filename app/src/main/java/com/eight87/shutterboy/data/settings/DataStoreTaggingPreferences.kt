package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete [TaggingPreferences] over the shared `shutterboy_settings`
 * DataStore. Labeling defaults ON; GenAI captions default OFF.
 */
internal class DataStoreTaggingPreferences(
    private val dataStore: DataStore<Preferences>,
) : TaggingPreferences {

    override fun observeEnabled(): Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[EnabledKey] ?: DEFAULT_ENABLED }

    override suspend fun setEnabled(value: Boolean) {
        dataStore.edit { it[EnabledKey] = value }
    }

    override fun observeCaptionsEnabled(): Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[CaptionsEnabledKey] ?: DEFAULT_CAPTIONS_ENABLED }

    override suspend fun setCaptionsEnabled(value: Boolean) {
        dataStore.edit { it[CaptionsEnabledKey] = value }
    }

    companion object {
        internal val EnabledKey = booleanPreferencesKey("tagging_enabled")
        internal val CaptionsEnabledKey = booleanPreferencesKey("tagging_captions_enabled")
        internal const val DEFAULT_ENABLED: Boolean = true
        internal const val DEFAULT_CAPTIONS_ENABLED: Boolean = false
    }
}
