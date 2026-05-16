package com.eight87.shutterboy.ui.search

import com.eight87.shutterboy.domain.Photo

/**
 * Phase G — client-side filter chips. The plan calls out Photos / Videos /
 * GPS / Recent. Videos are out of scope in v1 (the chip ships disabled), so
 * only Photos / Gps / Recent participate in the active set.
 *
 * Each variant exposes a `matches(Photo): Boolean`. Active filters AND
 * together; "Photos" alone is a no-op since the whole library is photos.
 *
 * "Recent" applies a 30-day window over `dateTakenMs` against the supplied
 * `now`. Recency is computed at filter-apply time, not stored.
 */
internal enum class SearchFilter {
    Photos,
    Videos, // disabled chip — never active, included for visual parity with the plan.
    Gps,
    Recent,
    ;

    fun matches(photo: Photo, nowMs: Long): Boolean = when (this) {
        Photos -> true
        Videos -> false // shouldn't be in the active set; defensive false.
        Gps -> photo.latitude != null && photo.longitude != null
        Recent -> (nowMs - photo.dateTakenMs) <= RECENT_WINDOW_MS
    }

    companion object {
        const val RECENT_WINDOW_MS: Long = 30L * 24L * 60L * 60L * 1000L
    }
}

internal fun applyFilters(
    photos: List<Photo>,
    active: Set<SearchFilter>,
    nowMs: Long,
): List<Photo> {
    if (active.isEmpty()) return photos
    return photos.filter { p -> active.all { it.matches(p, nowMs) } }
}
