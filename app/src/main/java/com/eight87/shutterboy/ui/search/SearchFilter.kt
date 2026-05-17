package com.eight87.shutterboy.ui.search

import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.eight87.shutterboy.R
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
 * R.F.2 — per-variant `ConditionUi` registry. Each [SearchFilter] variant
 * carries its label + enabled-predicate next to the variant itself, so the
 * chip row iterates the registry instead of `when`-ing over the enum at
 * two separate call sites (label + enabled). Adding a future variant means
 * a new enum case + a matching [ConditionUi] entry; the chip row needs no
 * edit.
 *
 * The folder chip is intentionally not part of this registry — its label is
 * a runtime value (the picked folder name), its trailing icon flips between
 * absent and a clear-X, and tapping opens a bottom sheet rather than
 * toggling a boolean. That asymmetry would dilute the abstraction; the
 * folder chip stays its own composable in `SearchScreen.kt`.
 */
internal data class ConditionUi(
    @StringRes val labelRes: Int,
    /** Resolves to true when the chip should be tappable. The lambda is
     *  passed the runtime flags the screen has on hand: whether favourites
     *  wiring is available, whether the folder source is wired, etc. The
     *  current registry only consults `favoritesWired`, but the lambda
     *  shape keeps the door open for future variant-specific gates without
     *  another `when` chain in the chip row. */
    val enabled: (ChipEnabledContext) -> Boolean,
)

internal data class ChipEnabledContext(
    val favoritesWired: Boolean,
)

/**
 * R.F.2 — single source of truth for chip metadata. Order is `entries`
 * order on the enum, which matches the visual order of the chip row.
 */
internal val SearchFilterUi: Map<SearchFilter, ConditionUi> = mapOf(
    SearchFilter.Photos to ConditionUi(
        labelRes = R.string.search_chip_photos,
        enabled = { true },
    ),
    SearchFilter.Videos to ConditionUi(
        labelRes = R.string.search_chip_videos,
        // Videos chip is permanently disabled in v1; defensive false so a
        // future caller can't accidentally toggle it on.
        enabled = { false },
    ),
    SearchFilter.Gps to ConditionUi(
        labelRes = R.string.search_chip_gps,
        enabled = { true },
    ),
    SearchFilter.Recent to ConditionUi(
        labelRes = R.string.search_chip_recent,
        enabled = { true },
    ),
    SearchFilter.Favorites to ConditionUi(
        labelRes = R.string.search_chip_favorites,
        enabled = { ctx -> ctx.favoritesWired },
    ),
)

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

/**
 * R.F.21 — `Saver` for the active filter list so rotation + process-death
 * don't blow the user's selected chips. Round-trips through the enum
 * names; unknown tokens are dropped silently (downgrade-safe).
 */
internal val SearchFilterListSaver: Saver<SnapshotStateList<SearchFilter>, List<String>> =
    Saver(
        save = { list -> list.map { it.name } },
        restore = { names ->
            mutableStateListOf<SearchFilter>().apply {
                names.forEach { token ->
                    runCatching { SearchFilter.valueOf(token) }.getOrNull()?.let(::add)
                }
            }
        },
    )
