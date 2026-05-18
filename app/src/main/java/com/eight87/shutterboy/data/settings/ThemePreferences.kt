package com.eight87.shutterboy.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Light/dark mode override. [System] follows the OS setting; [Light]
 * and [Dark] pin the UI regardless of the system value.
 */
enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    companion object {
        val Default: ThemeMode = System

        fun fromStored(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: Default
    }
}

/**
 * Narrow facet for theme persistence — both the [BaseTheme] colour
 * scheme picker (Material You / brand / pure black / custom seed) and
 * the [ThemeMode] light/dark override.
 */
interface ThemePreferences {
    fun observeBaseTheme(): Flow<BaseTheme>
    suspend fun setBaseTheme(value: BaseTheme)

    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(value: ThemeMode)
}
