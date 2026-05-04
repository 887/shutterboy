package com.eight87.shutterboy.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 destinations for shutterboy.
 *
 * Bottom-nav model: three top-level destinations ([Photos], [Collections],
 * [Settings]). Phase F adds a fullscreen [Viewer] (pushed onto whatever the
 * current stack is); Phase G adds [Search]; Phase I adds settings sub-pages.
 *
 * Single back stack for now — tapping a bottom-nav tab replaces the stack with
 * `[<that root>]`. Per-tab back stacks are deferred until a real complaint
 * surfaces (state-preserving tab switching is a UX call, not an architectural
 * blocker).
 *
 * All keys are `@Serializable` because [androidx.navigation3.runtime.rememberNavBackStack]
 * round-trips the stack through `SavedStateHandle` for config-change /
 * process-death survival.
 */
sealed interface Destination : NavKey

/** Photos timeline — the headline. Density-zoom + year-scrubber + inline bands. */
@Serializable
data object Photos : Destination

/** Collections — folders + smart-album chip row. Folder/smart-album detail
 *  screens are pushed onto this tab in Phase D. */
@Serializable
data object Collections : Destination

/** Settings root + sub-pages (sub-pages added in Phase I). */
@Serializable
data object Settings : Destination

/** The three top-level destinations a bottom-nav tap can route to. */
internal val rootDestinations: List<Destination> = listOf(Photos, Collections, Settings)
