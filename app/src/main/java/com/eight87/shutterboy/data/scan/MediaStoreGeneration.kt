package com.eight87.shutterboy.data.scan

import android.content.Context
import android.os.Build
import android.provider.MediaStore

/**
 * Phase incremental-scan A.1 — seam over [MediaStore.getGeneration] so
 * the API-26-vs-29 surface lives in one place AND so the
 * cold-start-gate test (C.2) can swap in a deterministic fake. The
 * returned token is a monotonic counter that bumps **only** when the
 * volume's contents change; comparing it to a persisted token is the
 * cheapest reliable way to ask "did anything change since the last
 * scan?".
 *
 * `MediaStore.getGeneration` is API 29+; on older SDKs the default
 * impl returns [ALWAYS_RESCAN] so the gate never matches and the
 * scanner always runs.
 *
 * DIP-aligned: high-level repository depends on the interface, the
 * concrete framework call lives behind it. Construct via the [Default]
 * companion factory in production, pass a fake in tests.
 */
interface MediaStoreGenerationSource {
    fun current(volume: String = MediaStore.VOLUME_EXTERNAL_PRIMARY): Long

    companion object {
        const val ALWAYS_RESCAN: Long = -1L

        fun Default(context: Context): MediaStoreGenerationSource =
            FrameworkMediaStoreGenerationSource(context)
    }
}

private class FrameworkMediaStoreGenerationSource(
    private val context: Context,
) : MediaStoreGenerationSource {
    override fun current(volume: String): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.getGeneration(context, volume)
        } else {
            MediaStoreGenerationSource.ALWAYS_RESCAN
        }
}
