package com.eight87.shutterboy.domain

/**
 * Emitted by [com.eight87.shutterboy.data.repo.MediaChangeSource] when the
 * underlying media surface (MediaStore or a SAF tree) reports a change. The
 * scanner subscribes; debouncing / coalescing is the scanner's responsibility.
 */
data class MediaChange(
    val timestampMs: Long,
    val source: ChangeSource,
) {
    enum class ChangeSource { DEVICE_MEDIASTORE, SAF_TREE }
}
