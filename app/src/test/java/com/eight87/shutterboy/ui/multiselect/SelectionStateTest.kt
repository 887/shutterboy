package com.eight87.shutterboy.ui.multiselect

import com.eight87.shutterboy.domain.PhotoId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase H.1 — pure-function coverage for [SelectionState] transitions. No Compose, no
 * Robolectric needed — these are plain JVM unit tests.
 */
class SelectionStateTest {

    private val a = PhotoId(1L)
    private val b = PhotoId(2L)
    private val c = PhotoId(3L)

    @Test
    fun enter_active_starts_with_single_id() {
        val s = SelectionState.Idle.enterActive(a)
        assertTrue(s is SelectionState.Active)
        assertEquals(setOf(a), (s as SelectionState.Active).selectedIds)
    }

    @Test
    fun toggle_from_idle_enters_active() {
        val s = SelectionState.Idle.toggle(a)
        assertTrue(s is SelectionState.Active)
        assertEquals(setOf(a), (s as SelectionState.Active).selectedIds)
    }

    @Test
    fun toggle_existing_removes_it_and_collapses_to_idle_when_empty() {
        val s1: SelectionState = SelectionState.Active(setOf(a))
        val s2 = s1.toggle(a)
        assertEquals(SelectionState.Idle, s2)
    }

    @Test
    fun toggle_existing_keeps_active_when_others_remain() {
        val s1: SelectionState = SelectionState.Active(setOf(a, b))
        val s2 = s1.toggle(a)
        assertTrue(s2 is SelectionState.Active)
        assertEquals(setOf(b), (s2 as SelectionState.Active).selectedIds)
    }

    @Test
    fun toggle_new_adds_to_active() {
        val s1: SelectionState = SelectionState.Active(setOf(a))
        val s2 = s1.toggle(b)
        assertTrue(s2 is SelectionState.Active)
        assertEquals(setOf(a, b), (s2 as SelectionState.Active).selectedIds)
    }

    @Test
    fun exit_collapses_to_idle() {
        val s: SelectionState = SelectionState.Active(setOf(a, b))
        assertEquals(SelectionState.Idle, s.exit())
    }

    @Test
    fun select_all_replaces_selection() {
        val s: SelectionState = SelectionState.Active(setOf(a))
        val s2 = s.selectAll(listOf(a, b, c))
        assertTrue(s2 is SelectionState.Active)
        assertEquals(setOf(a, b, c), (s2 as SelectionState.Active).selectedIds)
    }

    @Test
    fun is_selected_returns_false_when_idle() {
        assertFalse(SelectionState.Idle.isSelected(a))
    }

    @Test
    fun is_selected_returns_true_only_for_selected_ids() {
        val s: SelectionState = SelectionState.Active(setOf(a, c))
        assertTrue(s.isSelected(a))
        assertFalse(s.isSelected(b))
        assertTrue(s.isSelected(c))
    }

    @Test
    fun count_reflects_active_size() {
        assertEquals(0, SelectionState.Idle.count)
        assertEquals(2, SelectionState.Active(setOf(a, b)).count)
    }
}
