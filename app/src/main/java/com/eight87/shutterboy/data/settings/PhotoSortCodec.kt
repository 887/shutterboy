package com.eight87.shutterboy.data.settings

import com.eight87.shutterboy.domain.sort.Direction
import com.eight87.shutterboy.domain.sort.PhotoSort

/**
 * Phase E.2 — round-trip [PhotoSort] through a stable string token for
 * DataStore persistence. Tokens are versioned only by their shape; adding
 * a new sort axis is a new sealed-case + a new branch here. Unknown
 * tokens (e.g. a key written by a future build that this build doesn't
 * understand) decode to [PhotoSort.Default], so a downgrade is non-fatal.
 *
 * Naming: `<axis>_<direction>` lowercase, snake. Mirrors the
 * `<surface>_<role>` convention used elsewhere in shutterboy resource
 * keys.
 */
internal object PhotoSortCodec {

    fun encode(sort: PhotoSort): String {
        val axis = when (sort) {
            is PhotoSort.ByDateTaken -> "date_taken"
            is PhotoSort.ByDateAdded -> "date_added"
            is PhotoSort.ByName -> "name"
            is PhotoSort.BySize -> "size"
        }
        return "${axis}_${sort.direction.name.lowercase()}"
    }

    fun decode(token: String?): PhotoSort = when (token) {
        "date_taken_asc" -> PhotoSort.ByDateTaken(Direction.ASC)
        "date_taken_desc" -> PhotoSort.ByDateTaken(Direction.DESC)
        "date_added_asc" -> PhotoSort.ByDateAdded(Direction.ASC)
        "date_added_desc" -> PhotoSort.ByDateAdded(Direction.DESC)
        "name_asc" -> PhotoSort.ByName(Direction.ASC)
        "name_desc" -> PhotoSort.ByName(Direction.DESC)
        "size_asc" -> PhotoSort.BySize(Direction.ASC)
        "size_desc" -> PhotoSort.BySize(Direction.DESC)
        else -> PhotoSort.Default
    }
}
