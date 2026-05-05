package com.eight87.shutterboy.ui.collections

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Phase E.5 — pure-JUnit tests for [applyCustomOrder]. Pinned shape:
 * pinned items first in `persistedKeys` order, then any new items in
 * their natural position relative to one another, with deleted refs
 * dropped silently.
 */
class CustomOrderTest {

    @Test
    fun `empty persisted collapses to natural`() {
        val natural = listOf("a", "b", "c")
        assertEquals(natural, applyCustomOrder(natural, emptyList<String>()) { it })
    }

    @Test
    fun `pinned order overrides natural order`() {
        val natural = listOf("a", "b", "c", "d")
        val persisted = listOf("c", "a", "d", "b")
        assertEquals(persisted, applyCustomOrder(natural, persisted) { it })
    }

    @Test
    fun `new natural item lands at the end after pinned ones`() {
        val natural = listOf("a", "b", "c", "new")
        val persisted = listOf("c", "a", "b") // user pinned before "new" appeared
        assertEquals(listOf("c", "a", "b", "new"), applyCustomOrder(natural, persisted) { it })
    }

    @Test
    fun `multiple new items preserve natural order among themselves`() {
        val natural = listOf("a", "b", "n1", "c", "n2")
        val persisted = listOf("c", "a")
        // Pinned first ("c", "a"); the rest in natural order: "b", "n1", "n2".
        assertEquals(listOf("c", "a", "b", "n1", "n2"), applyCustomOrder(natural, persisted) { it })
    }

    @Test
    fun `keys absent from natural are dropped silently`() {
        val natural = listOf("a", "b")
        val persisted = listOf("deleted", "a", "ghost", "b")
        assertEquals(listOf("a", "b"), applyCustomOrder(natural, persisted) { it })
    }

    @Test
    fun `keyed variant works with non-trivial key extractor`() {
        data class Item(val id: Long, val label: String)
        val natural = listOf(Item(1, "alpha"), Item(2, "beta"), Item(3, "gamma"))
        val persisted = listOf(3L, 1L)
        val ordered = applyCustomOrder(natural, persisted) { it.id }
        assertEquals(listOf("gamma", "alpha", "beta"), ordered.map { it.label })
    }

    @Test
    fun `empty natural returns empty regardless of persisted`() {
        val natural = emptyList<String>()
        assertEquals(emptyList<String>(), applyCustomOrder(natural, listOf("a", "b")) { it })
    }

    @Test
    fun `single pinned item plus several new ones`() {
        val natural = listOf("a", "b", "c", "d")
        val persisted = listOf("c")
        assertEquals(listOf("c", "a", "b", "d"), applyCustomOrder(natural, persisted) { it })
    }
}
