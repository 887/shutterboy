package com.eight87.shutterboy.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Forward declaration so the data-layer scanner can depend on it without
 * importing the concrete `SettingsRepository` (which lands in Phase I
 * implementation). DIP-correct direction:
 *
 *   data/scan, data/saf, data/repo  ----depend on----> data/settings
 *
 * Tonearmboy R.A.5 absorbed: data/ never imports ui/settings/. Settings
 * facets that the data layer needs are interfaces published in `data/settings/`,
 * implemented later by the repo when it ships.
 */
interface ScanConfigSource {
    /** Set of persisted SAF tree URIs the multi-source library should walk. */
    val safSourceUris: Flow<Set<String>>
}
