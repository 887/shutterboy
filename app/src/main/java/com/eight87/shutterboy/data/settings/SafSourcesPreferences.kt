package com.eight87.shutterboy.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Phase I.3.b — persisted set of SAF tree URIs the multi-source library
 * walks. Mutation surface for the Settings → Library → Manage sources
 * screen; reads back through [ScanConfigSource.safSourceUris] (the
 * data-layer-facing facet).
 *
 * URIs are stored as their `Uri.toString()` form (e.g.
 * `content://com.android.externalstorage.documents/tree/primary%3APictures`).
 * Persistable-URI-permission lifecycle (`takePersistableUriPermission` on
 * add, `releasePersistableUriPermission` on remove) lives in the UI layer
 * since the `ContentResolver` call is intent-result-bound.
 */
interface SafSourcesPreferences {
    /** Most-recent persisted snapshot, hot-flow style. */
    fun observeSources(): Flow<Set<String>>

    /** Add [uri] to the set. No-op if already present. */
    suspend fun add(uri: String)

    /** Remove [uri] from the set. No-op if absent. */
    suspend fun remove(uri: String)
}
