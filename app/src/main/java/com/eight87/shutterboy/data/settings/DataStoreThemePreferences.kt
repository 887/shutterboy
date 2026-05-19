package com.eight87.shutterboy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Concrete [ThemePreferences] backed by the shared `shutterboy_settings`
 * DataStore. Two keys: [BaseThemeKey] stores the [BaseTheme.toStored]
 * string; [ThemeModeKey] stores the [ThemeMode] enum name.
 */
internal class DataStoreThemePreferences(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferences {

    override fun observeBaseTheme(): Flow<BaseTheme> = dataStore.data
        .map { prefs -> BaseTheme.fromStored(prefs[BaseThemeKey]) }

    override suspend fun setBaseTheme(value: BaseTheme) {
        dataStore.edit { it[BaseThemeKey] = value.toStored() }
    }

    override fun observeThemeMode(): Flow<ThemeMode> = dataStore.data
        .map { prefs -> ThemeMode.fromStored(prefs[ThemeModeKey]) }

    override suspend fun setThemeMode(value: ThemeMode) {
        dataStore.edit { it[ThemeModeKey] = value.name }
    }

    override fun observeTintColor(): Flow<Long?> = dataStore.data
        .map { prefs -> prefs[TintColorKey] }

    override suspend fun setTintColor(rgb: Long?) {
        dataStore.edit { prefs ->
            if (rgb == null) prefs.remove(TintColorKey)
            else prefs[TintColorKey] = rgb and 0xFFFFFFL
        }
    }

    companion object {
        internal val BaseThemeKey = stringPreferencesKey("base_theme")
        internal val ThemeModeKey = stringPreferencesKey("theme_mode")
        internal val TintColorKey = longPreferencesKey("tint_color")
    }
}
