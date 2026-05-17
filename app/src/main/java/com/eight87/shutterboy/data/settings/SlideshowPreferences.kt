package com.eight87.shutterboy.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Phase J.2 — narrow facet for slideshow user preferences. Currently
 * surfaces the Ken Burns toggle (slow zoom + drift during each slide's
 * dwell). Additional knobs (dwell duration, transition style) drop in
 * here as siblings of `kenBurnsEnabled` when their phases land.
 */
interface SlideshowPreferences {
    fun observeKenBurnsEnabled(): Flow<Boolean>
    suspend fun setKenBurnsEnabled(value: Boolean)
}
