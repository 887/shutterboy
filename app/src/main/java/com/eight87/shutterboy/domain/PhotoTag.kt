package com.eight87.shutterboy.domain

/**
 * A single on-device-derived tag for a photo, in domain shape for the UI.
 * [isCaption] distinguishes a natural-language description (GenAI) from a
 * keyword label (ML Kit Image Labeling) so the viewer can render them
 * differently — keywords as chips, a caption as a sentence.
 */
data class PhotoTag(
    val label: String,
    val confidence: Float,
    val isCaption: Boolean,
)
