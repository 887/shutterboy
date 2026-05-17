package com.eight87.shutterboy.ui.search

import com.eight87.shutterboy.domain.FolderId
import com.eight87.shutterboy.domain.Photo

/**
 * Phase G — client-side filter chips. The plan calls out Photos / Videos /
 * GPS / Recent / Favorites / Folder. Videos are out of scope in v1 (the
 * chip ships disabled), so only Photos / Gps / Recent / Favorites
 * participate in the active set. Folder is handled as a separate single-
 * value selection (see [applyFilters]) since its label / clear affordance
 * differ from the on/off chips.
 *
 * Each variant exposes a `matches(Photo, ...): Boolean`. Active filters
 * AND together; "Photos" alone is a no-op since the whole library is photos.
 *
 * "Recent" applies a 30-day window over `dateTakenMs` against the supplied
 * `now`. Recency is computed at filter-apply time, not stored.
 *
 * "Favorites" intersects with the favorite-id set passed at apply time.
 */
internal enum class SearchFilter {
    Photos,
    Videos, // disabled chip — never active, included for visual parity with the plan.
    Gps,
    Recent,
    Favorites,
    ;

    fun matches(photo: Photo, nowMs: Long, favoriteIds: Set<Long>): Boolean = when (this) {
        Photos -> true
        Videos -> false // shouldn't be in the active set; defensive false.
        Gps -> photo.latitude != null && photo.longitude != null
        Recent -> (nowMs - photo.dateTakenMs) <= RECENT_WINDOW_MS
        Favorites -> photo.id.value in favoriteIds
    }

    companion object {
        const val RECENT_WINDOW_MS: Long = 30L * 24L * 60L * 60L * 1000L
    }
}

/**
 * Applies the active boolean filters AND-ed with an optional folder
 * selection. AND semantics across all conditions, mirroring the chip
 * row + folder-picker bottom-sheet UX:
 *
 *  - empty active set + null folder → return list unchanged
 *  - "Favorites" + folder selected + text-matched list → photos must be
 *    in `favoriteIds` AND in the selected folder AND in the original list.
 */
internal fun applyFilters(
    photos: List<Photo>,
    active: Set<SearchFilter>,
    nowMs: Long,
    favoriteIds: Set<Long> = emptySet(),
    selectedFolderId: FolderId? = null,
): List<Photo> {
    if (active.isEmpty() && selectedFolderId == null) return photos
    return photos.filter { p ->
        active.all { it.matches(p, nowMs, favoriteIds) } &&
            (selectedFolderId == null || p.folderId == selectedFolderId)
    }
}
