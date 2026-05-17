package com.eight87.shutterboy.ui.settings.catalog

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

/**
 * I.7 — pending-flash channel for the settings search overlay.
 *
 * When the user taps a search result the overlay calls [flash] with the
 * matched row id, then pops itself. The receiving sub-page composes
 * shortly after, observes [LocalHighlightedSettingId] (provided at the
 * app root from this object's [state]), and any [SettingsRow] whose `id`
 * matches briefly highlights its background then clears the value.
 *
 * Kept as a process-global object — not a `RouteScope` field — because
 * the flash hop is a one-shot signal between two compositions that don't
 * share a parent (the overlay is popped before the sub-page commits).
 * A static channel is simpler than threading the state through
 * SavedStateHandle and survives navigation just as well.
 */
object FlashRowController {

    /** Mutable composition state — `null` means no pending flash. */
    val state: MutableState<String?> = mutableStateOf(null)

    /** Schedule the row with [id] to highlight on next composition. */
    fun flash(id: String) {
        state.value = id
    }

    /** Test hook — reset between cases. */
    fun reset() {
        state.value = null
    }
}

/**
 * Composition local read by [SettingsRow] to decide whether it should
 * briefly highlight. Provided at the app root with [FlashRowController.state].
 * The default is a no-op `null` state so screens composed outside the app
 * (Robolectric, previews) still work.
 */
val LocalHighlightedSettingId = compositionLocalOf<MutableState<String?>> {
    mutableStateOf(null)
}
