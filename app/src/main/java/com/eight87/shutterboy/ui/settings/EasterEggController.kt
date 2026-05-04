package com.eight87.shutterboy.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Pure tap-counter state for the About-page easter egg, mirroring tonearmboy's
 * D.16.5. Three taps within the configured window trigger the reveal; outside
 * the window, the counter resets so a stray earlier tap doesn't compound with
 * fresh ones.
 *
 * Pure data + transition function so unit tests can exhaustively pin the state
 * machine without spinning up Compose.
 */
data class EasterEggTapState(
    val tapCount: Int = 0,
    val firstTapAtMs: Long? = null,
)

/**
 * Apply a tap at [now] given a [windowMs] reset window. If the previous first
 * tap is older than the window, this tap starts a new run; otherwise the count
 * advances inside the same window.
 */
internal fun EasterEggTapState.afterTap(now: Long, windowMs: Long): EasterEggTapState {
    val first = firstTapAtMs
    return if (first == null || now - first > windowMs) {
        EasterEggTapState(tapCount = 1, firstTapAtMs = now)
    } else {
        EasterEggTapState(tapCount = tapCount + 1, firstTapAtMs = first)
    }
}

/** Reveal threshold — keeps tonearmboy's 3-tap convention. */
internal const val EASTER_EGG_TAP_THRESHOLD: Int = 3

/** Default tap-window: 2 seconds, matching tonearmboy's D.16.5 behaviour. */
internal const val EASTER_EGG_WINDOW_MS: Long = 2_000L

/**
 * Bindings the About screen consumes — a single onTap callback for the
 * version row, a `revealed` flag the screen passes to the dialog, and an
 * `onDismiss` for tap-outside / back-button.
 */
class EasterEggBindings(
    val onVersionTap: () -> Unit,
    private val getRevealed: () -> Boolean,
    val onDismiss: () -> Unit,
) {
    val revealed: Boolean get() = getRevealed()
}

/**
 * Compose helper that wires the pure state machine into a remembered
 * [EasterEggBindings]. The window default is 2 s; tests inject their own
 * window via the pure [afterTap] function.
 */
@Composable
fun rememberEasterEggBindings(
    nowProvider: () -> Long = { System.currentTimeMillis() },
    windowMs: Long = EASTER_EGG_WINDOW_MS,
): EasterEggBindings {
    var state by remember { mutableStateOf(EasterEggTapState()) }
    var revealed by remember { mutableStateOf(false) }
    return remember {
        EasterEggBindings(
            onVersionTap = {
                val next = state.afterTap(nowProvider(), windowMs)
                if (next.tapCount >= EASTER_EGG_TAP_THRESHOLD) {
                    revealed = true
                    state = EasterEggTapState()
                } else {
                    state = next
                }
            },
            getRevealed = { revealed },
            onDismiss = { revealed = false },
        )
    }
}
