package com.eight87.shutterboy.ui.viewer

/**
 * F.4 — pure helpers for the viewer's backing-id list mutation on delete.
 *
 * The viewer's [androidx.compose.foundation.pager.HorizontalPager] ranges
 * over a `mutableStateListOf<Long>` seeded from the route arg. When a
 * delete settles, we remove the deleted id from the list and need to land
 * the pager on the next-in-line page. These helpers are pure so they live
 * here and get covered by a fast JUnit test instead of a Compose-rule
 * smoke test.
 */
internal object ViewerBackingListMath {

    /** Result of removing a photo id from the backing list. */
    data class RemovalResult(
        val newList: List<Long>,
        /**
         * Page index the pager should land on after the removal. When the
         * removed id was at the tail, this clamps to the new last index
         * (size - 1). When the list becomes empty, returns -1 — the
         * caller should pop the viewer.
         */
        val newPosition: Int,
    )

    /**
     * Remove [deletedId] from [list] and compute the pager's new page
     * index given the [currentPosition] the user was viewing.
     *
     * Behaviour:
     *  - If the removed id was at or before the current position and the
     *    list still has tail items, [newPosition] keeps the same visual
     *    "next in line" slot (i.e. shifts down by one when the removed
     *    item was before the current cursor, or stays put when it was
     *    the current cursor itself with a successor available).
     *  - If the removed id was the tail item, [newPosition] clamps to the
     *    new last index.
     *  - If [deletedId] is not in the list, returns the list unchanged
     *    plus the original position.
     *  - If the list becomes empty, [newPosition] is -1.
     */
    fun removeId(
        list: List<Long>,
        deletedId: Long,
        currentPosition: Int,
    ): RemovalResult {
        val idx = list.indexOf(deletedId)
        if (idx < 0) return RemovalResult(list, currentPosition)
        val next = list.toMutableList().apply { removeAt(idx) }
        if (next.isEmpty()) return RemovalResult(next, -1)
        val newPos = when {
            idx < currentPosition -> currentPosition - 1
            idx == currentPosition -> currentPosition.coerceAtMost(next.size - 1)
            else -> currentPosition
        }.coerceIn(0, next.size - 1)
        return RemovalResult(next, newPos)
    }
}
