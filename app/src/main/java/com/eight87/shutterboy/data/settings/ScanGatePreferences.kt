package com.eight87.shutterboy.data.settings

import kotlinx.coroutines.flow.Flow

/**
 * Phase incremental-scan A.2 + A.3 — persistence of the cold-start scan
 * gate. Two coarse caches:
 *
 * - **MediaStore generation token** (per volume). Compared against
 *   `MediaStore.getGeneration(context, volume)` on cold boot; matching
 *   token = skip the full scan.
 * - **SAF tree fingerprint map** (uri → recursive image-leaf count).
 *   SAF doesn't expose a generation API, so a coarse child-count
 *   comparison is the cheapest invalidator that catches add / remove.
 *
 * Writes are suspend; the repository persists tokens AFTER the scan
 * succeeds (per plan B.1), so a crashed scan re-runs next boot.
 */
interface ScanGatePreferences {
    fun observeMediaStoreGeneration(volume: String): Flow<Long?>
    suspend fun setMediaStoreGeneration(volume: String, token: Long)

    fun observeSafFingerprint(): Flow<Map<String, Int>>
    suspend fun setSafFingerprint(map: Map<String, Int>)

    /** Manual override path — Settings → Library → Rescan photos. */
    suspend fun clear()
}
