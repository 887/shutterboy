package com.eight87.shutterboy.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Smart Tags — narrow facet for the on-device tagging toggles.
 *
 * - [observeEnabled] is the master switch for keyword labeling. Defaults
 *   ON so tagging runs as part of normal scanning; the user turns it off
 *   on the Smart Tags settings page.
 * - [observeCaptionsEnabled] gates the heavier GenAI natural-language
 *   descriptions. Defaults OFF (opt-in) — captioning is slower and only
 *   works on AICore-capable devices.
 */
interface TaggingPreferences {
    fun observeEnabled(): Flow<Boolean>
    suspend fun setEnabled(value: Boolean)
    fun observeCaptionsEnabled(): Flow<Boolean>
    suspend fun setCaptionsEnabled(value: Boolean)
}
