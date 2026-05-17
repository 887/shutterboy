package com.eight87.shutterboy.domain

sealed interface ScanProgress {
    data object Idle : ScanProgress
    /**
     * Live scan progress. `total` is non-null and the current count of
     * media items the scanner has surfaced so far. `currentTitle` is the
     * display name of the most-recently-processed item, or null when
     * the scanner has not yet emitted any item (start of a run).
     *
     * Producers throttle emissions of this state to ~5 Hz (200 ms) to
     * avoid forcing a Compose recomposition on every photo on a fast
     * device — see `SCAN_PROGRESS_THROTTLE_MS` in
     * `RoomGalleryRepository`.
     */
    data class Running(
        val processed: Int,
        val total: Int?,
        val currentTitle: String? = null,
    ) : ScanProgress {
        /** 0..1, or 0 when total is null/0 (renders as indeterminate). */
        val fraction: Float
            get() {
                val t = total ?: return 0f
                return if (t <= 0) 0f else processed.toFloat() / t.toFloat()
            }
    }
    data class Done(val deltaCount: Int) : ScanProgress
    data class Failed(val reason: String) : ScanProgress
}
