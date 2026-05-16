package com.eight87.shutterboy.ui.multiselect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.eight87.shutterboy.domain.PhotoId

/**
 * Multi-select state (Phase H.1 scaffold).
 *
 * Sealed type with two cases: [Idle] (no selection chrome) and [Active] (selection chrome
 * shown, with the set of currently-selected ids). Pure transition methods so the state is
 * unit-testable without Compose.
 */
sealed interface SelectionState {
    data object Idle : SelectionState
    data class Active(val selectedIds: Set<PhotoId>) : SelectionState

    val isActive: Boolean get() = this is Active
    val count: Int get() = if (this is Active) selectedIds.size else 0
}

fun SelectionState.enterActive(id: PhotoId): SelectionState =
    SelectionState.Active(selectedIds = setOf(id))

fun SelectionState.toggle(id: PhotoId): SelectionState = when (this) {
    is SelectionState.Idle -> SelectionState.Active(setOf(id))
    is SelectionState.Active -> {
        val next = if (id in selectedIds) selectedIds - id else selectedIds + id
        if (next.isEmpty()) SelectionState.Idle else SelectionState.Active(next)
    }
}

fun SelectionState.exit(): SelectionState = SelectionState.Idle

fun SelectionState.selectAll(allIds: Collection<PhotoId>): SelectionState =
    if (allIds.isEmpty()) this else SelectionState.Active(allIds.toSet())

fun SelectionState.deselectAll(): SelectionState = SelectionState.Idle

fun SelectionState.isSelected(id: PhotoId): Boolean =
    this is SelectionState.Active && id in selectedIds

/**
 * Hoisted, saveable selection state holder. The state survives configuration changes via
 * [rememberSaveable] using a list-of-longs encoding.
 */
@Stable
class SelectionHolder internal constructor(initial: SelectionState) {
    var state: SelectionState by mutableStateOf(initial)
        private set

    fun enterActive(id: PhotoId) { state = state.enterActive(id) }
    fun toggle(id: PhotoId) { state = state.toggle(id) }
    fun exit() { state = state.exit() }
    fun selectAll(allIds: Collection<PhotoId>) { state = state.selectAll(allIds) }
    fun deselectAll() { state = state.deselectAll() }
}

@Composable
fun rememberSelectionHolder(): SelectionHolder {
    val ids = rememberSaveable(saver = SelectionLongListSaver) { mutableListOf<Long>() }
    return remember(ids) {
        val initial: SelectionState =
            if (ids.isEmpty()) SelectionState.Idle
            else SelectionState.Active(ids.map(::PhotoId).toSet())
        SelectionHolder(initial)
    }
}

private val SelectionLongListSaver: Saver<MutableList<Long>, ArrayList<Long>> = Saver(
    save = { ArrayList(it) },
    restore = { it.toMutableList() },
)
