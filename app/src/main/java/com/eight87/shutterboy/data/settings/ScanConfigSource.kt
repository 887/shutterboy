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

    /**
     * R.F.26 — drop revoked SAF tree URIs from the persisted set. The
     * scanner detects revocation (no permission, tree disappeared, etc.)
     * and calls back here so the next observation surfaces the cleaned set.
     * Default no-op so test/fake impls don't have to implement.
     */
    suspend fun pruneRevoked(uris: Set<String>) {}
}
