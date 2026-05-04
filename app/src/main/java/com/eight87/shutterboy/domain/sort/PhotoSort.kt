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

    /** Newest first by default — matches Photos timeline expectation. */
    data class ByDateTaken(override val direction: Direction = Direction.DESC) : PhotoSort {
        override val comparator: Comparator<Photo> =
            direction.applyTo(compareBy { it.dateTakenMs })
    }

    /** File mtime — useful when capture date is unreliable. */
    data class ByDateAdded(override val direction: Direction = Direction.DESC) : PhotoSort {
        override val comparator: Comparator<Photo> =
            direction.applyTo(compareBy { it.dateAddedMs })
    }

    /** Name ASC by default; intelligent leading-article strip lands when a
     *  multilingual sort comparator extension is added (post-Phase-T.E). */
    data class ByName(override val direction: Direction = Direction.ASC) : PhotoSort {
        override val comparator: Comparator<Photo> =
            direction.applyTo(compareBy { it.displayName.lowercase() })
    }

    /** Biggest first by default. */
    data class BySize(override val direction: Direction = Direction.DESC) : PhotoSort {
        override val comparator: Comparator<Photo> =
            direction.applyTo(compareBy { it.sizeBytes })
    }

    companion object {
        val Default: PhotoSort = ByDateTaken(Direction.DESC)
    }
}

enum class Direction {
    ASC, DESC;

    fun <T> applyTo(comp: Comparator<T>): Comparator<T> =
        if (this == ASC) comp else comp.reversed()
}
