package com.eight87.shutterboy.domain.sort

import com.eight87.shutterboy.domain.Photo

/**
 * Locked in design doc A.4 — four axes with documented default directions.
 * The repository accepts a [PhotoSort] and routes to a Room query that
 * `ORDER BY`s the right column; the [comparator] here is the test-scoped
 * equivalent (used by Robolectric tests + by any client-side resort that needs
 * to honour the same ordering without going through Room).
 */
sealed interface PhotoSort {
    val direction: Direction
    val comparator: Comparator<Photo>

    /**
     * SQL ORDER BY fragment. Variants are a sealed type with hardcoded fragments
     * — never user input — so this is injection-safe by construction.
     * Repository routes through Room's `@RawQuery` to push the sort to the DB.
     */
    val sqlOrderBy: String

    /** Newest first by default — matches Photos timeline expectation. */
    data class ByDateTaken(override val direction: Direction = Direction.DESC) : PhotoSort {
        override val comparator: Comparator<Photo> =
            direction.applyTo(compareBy { it.dateTakenMs })
        override val sqlOrderBy: String = "date_taken_ms ${direction.sqlKeyword}"
    }

    /** File mtime — useful when capture date is unreliable. */
    data class ByDateAdded(override val direction: Direction = Direction.DESC) : PhotoSort {
        override val comparator: Comparator<Photo> =
            direction.applyTo(compareBy { it.dateAddedMs })
        override val sqlOrderBy: String = "date_added_ms ${direction.sqlKeyword}"
    }

    /** Name ASC by default; intelligent leading-article strip lands when a
     *  multilingual sort comparator extension is added (post-Phase-T.E). */
    data class ByName(override val direction: Direction = Direction.ASC) : PhotoSort {
        override val comparator: Comparator<Photo> =
            direction.applyTo(compareBy { it.displayName.lowercase() })
        override val sqlOrderBy: String = "display_name COLLATE NOCASE ${direction.sqlKeyword}"
    }

    /** Biggest first by default. */
    data class BySize(override val direction: Direction = Direction.DESC) : PhotoSort {
        override val comparator: Comparator<Photo> =
            direction.applyTo(compareBy { it.sizeBytes })
        override val sqlOrderBy: String = "size_bytes ${direction.sqlKeyword}"
    }

    companion object {
        val Default: PhotoSort = ByDateTaken(Direction.DESC)
    }
}

enum class Direction(val sqlKeyword: String) {
    ASC("ASC"),
    DESC("DESC");

    fun <T> applyTo(comp: Comparator<T>): Comparator<T> =
        if (this == ASC) comp else comp.reversed()
}
