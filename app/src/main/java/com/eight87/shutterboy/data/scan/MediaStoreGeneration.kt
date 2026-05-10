package com.eight87.shutterboy.data.scan

import android.content.Context
import android.os.Build
import android.provider.MediaStore

/**
 * Phase incremental-scan A.1 — thin wrapper around
 * [MediaStore.getGeneration] so the API-26-vs-29 surface lives in one
 * place. `MediaStore.getGeneration` is API 29+; on older SDKs we return
 * the [ALWAYS_RESCAN] sentinel so the gate never matches and the scanner
 * always runs.
 *
 * The returned token is a monotonic counter that bumps **only** when the
 * volume's contents change. Comparing it to a persisted token is the
 * cheapest reliable way to ask "did anything change since the last
 * scan?".
 */
object MediaStoreGeneration {

    /**
     * Sentinel returned on API levels that don't support
     * [MediaStore.getGeneration]. Callers MUST treat this as "always
     * differs from the persisted token" — i.e. always rescan.
     */
    const val ALWAYS_RESCAN: Long = -1L

    fun current(
        context: Context,
        volume: String = MediaStore.VOLUME_EXTERNAL_PRIMARY,
    ): Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.getGeneration(context, volume)
    } else {
        ALWAYS_RESCAN
    }
}
