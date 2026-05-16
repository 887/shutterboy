package com.eight87.shutterboy.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * F.4 — pure-JUnit coverage of the backing-list mutation that lands when
 * a viewer delete settles. The viewer itself is Compose-side; pulling
 * the math out lets us verify it without a Robolectric mount.
 */
class ViewerBackingListMathTest {

    @Test
    fun delete_middle_item_keeps_position_pointing_at_next_in_line() {
        // Given a 3-photo list [10, 20, 30] viewing index 1 (id=20),
        // deleting id=20 should leave [10, 30] with position still at
        // index 1 (which is the formerly-next photo, id=30).
        val result = ViewerBackingListMath.removeId(
            list = listOf(10L, 20L, 30L),
            deletedId = 20L,
            currentPosition = 1,
        )
        assertEquals(listOf(10L, 30L), result.newList)
        assertEquals(1, result.newPosition)
    }

    @Test
    fun delete_first_item_when_viewing_it_lands_on_new_first() {
        val result = ViewerBackingListMath.removeId(
            list = listOf(10L, 20L, 30L),
            deletedId = 10L,
            currentPosition = 0,
        )
        assertEquals(listOf(20L, 30L), result.newList)
        assertEquals(0, result.newPosition)
    }

    @Test
    fun delete_tail_item_clamps_position_to_new_last_index() {
        val result = ViewerBackingListMath.removeId(
            list = listOf(10L, 20L, 30L),
            deletedId = 30L,
            currentPosition = 2,
        )
        assertEquals(listOf(10L, 20L), result.newList)
        assertEquals(1, result.newPosition)
    }

    @Test
    fun delete_item_before_cursor_shifts_position_down_by_one() {
        // Viewing id=30 at index 2; deleting id=10 leaves [20, 30] —
        // cursor should follow id=30 to its new index (1).
        val result = ViewerBackingListMath.removeId(
            list = listOf(10L, 20L, 30L),
            deletedId = 10L,
            currentPosition = 2,
        )
        assertEquals(listOf(20L, 30L), result.newList)
        assertEquals(1, result.newPosition)
    }

    @Test
    fun delete_last_remaining_item_signals_pop_via_negative_position() {
        val result = ViewerBackingListMath.removeId(
            list = listOf(42L),
            deletedId = 42L,
            currentPosition = 0,
        )
        assertEquals(emptyList<Long>(), result.newList)
        assertEquals(-1, result.newPosition)
    }

    @Test
    fun delete_id_not_in_list_is_a_noop() {
        val result = ViewerBackingListMath.removeId(
            list = listOf(10L, 20L, 30L),
            deletedId = 99L,
            currentPosition = 1,
        )
        assertEquals(listOf(10L, 20L, 30L), result.newList)
        assertEquals(1, result.newPosition)
    }
}
