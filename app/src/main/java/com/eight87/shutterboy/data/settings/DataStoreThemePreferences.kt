package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete [ThemePreferences] backed by the shared `shutterboy_settings`
 * DataStore. The on-disk wire format is the [BaseTheme.toStored] string;
 * an absent / malformed key resolves to [BaseTheme.Default].
 */
internal class DataStoreThemePreferences(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferences {

    override fun observeBaseTheme(): Flow<BaseTheme> = dataStore.data
        .map { prefs -> BaseTheme.fromStored(prefs[BaseThemeKey]) }

    override suspend fun setBaseTheme(value: BaseTheme) {
        dataStore.edit { it[BaseThemeKey] = value.toStored() }
    }

    companion object {
        internal val BaseThemeKey = stringPreferencesKey("base_theme")
    }
}
