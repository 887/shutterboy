package com.eight87.shutterboy.data.tagging

import android.graphics.Bitmap

/** Where a [TagCandidate] came from. */
enum class TagSource(val stored: String) {
    /** Keyword from ML Kit Image Labeling (e.g. "Dog", "Beach"). */
    LABEL("label"),

    /** Natural-language sentence from ML Kit GenAI Image Description. */
    CAPTION("caption"),
}

/** One tag the engine produced for an image. */
data class TagCandidate(
    val label: String,
    val confidence: Float,
    val source: TagSource,
)

/**
 * What the device can actually do. Both can be false (no Play Services /
 * no AICore) — the feature then reports itself unavailable and the app
 * carries on without it.
 */
data class TaggerAvailability(
    val labelsAvailable: Boolean,
    val captionsAvailable: Boolean,
) {
    val any: Boolean get() = labelsAvailable || captionsAvailable

    companion object {
        val NONE = TaggerAvailability(labelsAvailable = false, captionsAvailable = false)
    }
}

/**
 * On-device image tagging seam (DIP). The background pass depends on this
 * abstraction, never on ML Kit directly, so it's testable with a fake and
 * the concrete engine can be swapped without touching the orchestration.
 *
 * Implementations MUST NOT throw from [analyze] — a single undecodable or
 * model-rejected image must not abort the whole pass. Return an empty list
 * instead.
 */
interface ImageTagger {
    /** Probe what the device supports right now (may trigger a model download). */
    suspend fun availability(): TaggerAvailability

    /**
     * Analyse one bitmap. Always returns labels when labeling is available;
     * appends a caption when [wantCaption] is set and captioning is
     * available. Never throws.
     */
    suspend fun analyze(bitmap: Bitmap, wantCaption: Boolean): List<TagCandidate>

    /** Release native resources (GenAI client). Safe to call repeatedly. */
    fun close()
}
