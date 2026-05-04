package com.eight87.shutterboy.ui.nav

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

/**
 * Back stack for the bottom-nav model. Tab taps replace the stack with the
 * root key for that tab; pushes (Viewer, Search, settings sub-pages) layer
 * on top. Pop honours the stack, and the bottom-nav-current-tab is whichever
 * root key sits at index 0.
 *
 * Single stack — phase C choice; per-tab stacks are deferred.
 */
class ShutterboyBackStack(rootKey: Destination = Photos) {

    /** Flat back stack — what `NavDisplay` consumes. */
    val backStack: SnapshotStateList<NavKey> = mutableStateListOf<NavKey>(rootKey)

    /** Currently visible destination. */
    val current: NavKey
        get() = backStack.last()

    /** Whichever root destination is at the bottom of the stack — drives the
     *  bottom-nav selected indicator. */
    val currentTab: Destination
        get() = (backStack.firstOrNull() as? Destination) ?: Photos

    /** Push any destination onto the stack (Viewer, Search, sub-pages). */
    fun push(key: NavKey) {
        backStack.add(key)
    }

    /** Pop the top entry. Pressing back at the root is a no-op (the system
     *  back-press handler will let the activity finish). */
    fun pop() {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    /** Switch tabs — replace the entire stack with a single new root.
     *  Tapping the same tab again is a no-op. */
    fun selectTab(root: Destination) {
        if (currentTab == root && backStack.size == 1) return
        backStack.clear()
        backStack.add(root)
    }

    /** Pop everything above the first occurrence of [key], or push if absent. */
    fun popToFirstOrPush(key: NavKey) {
        val idx = backStack.indexOfFirst { it == key }
        if (idx >= 0) {
            while (backStack.size > idx + 1) backStack.removeAt(backStack.lastIndex)
        } else {
            push(key)
        }
    }
}
