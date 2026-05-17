package com.eight87.shutterboy.data.repo

import com.eight87.shutterboy.domain.LibrarySnapshot
import com.eight87.shutterboy.domain.ScanProgress
import kotlinx.coroutines.flow.Flow

interface LibraryScanner {
    /**
     * Force-rescan / test-only entrypoint. Always walks MediaStore + every
     * SAF tree end-to-end. Settings → Library → "Rescan photos" routes here
     * via [forceRescan] (which also clears the cold-start gate).
     */
    suspend fun runScan(): LibrarySnapshot

    /**
     * Phase incremental-scan B.1 — cold-start gate. Compares
     * `MediaStore.getGeneration()` against the persisted token and the
     * recursive SAF tree fingerprints against the persisted map; if both
     * match, returns the cached snapshot without touching the scanner. If
     * either differs, runs the full scan, then persists the fresh tokens
     * AFTER the scan succeeds (a crashed scan must re-run next boot).
     *
     * This is the entrypoint the UI's first [observePhotos] collection
     * should hit on cold start — NOT [runScan].
     */
    suspend fun scanIfChanged(): LibrarySnapshot

    /**
     * Manual override — clears the persisted gate tokens, then runs a full
     * scan. Settings → Library → "Rescan photos" calls this so a force-
     * rescan also re-seeds the gate.
     */
    suspend fun forceRescan(): LibrarySnapshot

    /**
     * Phase I.3.d — destructive reset. Wipes the Room cache (photos,
     * folders, favorites, FTS shadow) and clears the cold-start scan
     * gate, then runs a fresh scan as if on first launch. This is the
     * heavy-handed counterpart to [forceRescan]: where `forceRescan`
     * only re-evaluates against the existing cache, this rebuilds the
     * cache from zero.
     *
     * Settings → Library → "Reset library cache" routes here (behind a
     * destructive-action confirm dialog).
     */
    suspend fun resetAndRescan(): LibrarySnapshot

    fun scanProgress(): Flow<ScanProgress>
}
