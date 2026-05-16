package com.eight87.shutterboy.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Narrow facet for theme persistence. Surfaces a single [BaseTheme]
 * preference: the four-way picker (Material You / brand / pure black /
 * custom seed). Consumers observe the current value as a Flow and
 * write through the suspend setter.
 */
interface ThemePreferences {
    fun observeBaseTheme(): Flow<BaseTheme>
    suspend fun setBaseTheme(value: BaseTheme)
}
