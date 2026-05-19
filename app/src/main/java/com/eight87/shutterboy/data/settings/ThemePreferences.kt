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

    /**
     * Optional accent-colour overlay. When non-null, the resolved
     * [BaseTheme] scheme keeps its surface/background colours but the
     * primary / secondary / tertiary slots are derived from this
     * seed. Surface NEVER tracks tint — so the app background stays
     * whatever the base theme dictates (dynamic surface or pure
     * black), regardless of accent.
     *
     * Stored as a `Long` 24-bit RGB; null = no tint (use the base
     * scheme's primaries as-is).
     */
    fun observeTintColor(): Flow<Long?>
    suspend fun setTintColor(rgb: Long?)
}
