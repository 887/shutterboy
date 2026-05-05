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

/**
 * Phase C.x — About sub-page (mirrors tonearmboy D.16.4 + D.16.5). Pushed onto
 * the Settings tab from the placeholder root row until Phase I builds the full
 * M3 Expressive grouped-cards Settings root with About underneath
 * Settings → Library.
 */
@Serializable
data object SettingsAbout : Destination

/**
 * Phase C.6 — fullscreen viewer route. Pushed from a thumbnail / cover-tile
 * tap on the Photos timeline (and, in later phases, from Collections detail
 * screens, Search results, and the Slideshow). Carries the tapped photo's
 * id plus the backing list of photo ids for the receiving `HorizontalPager`.
 *
 * Phase F replaces the placeholder body with the actual pager + chrome +
 * EXIF panel + delete / share / edit-handoff. The route shape stays stable.
 */
@Serializable
data class PhotoViewer(
    val photoIdValue: Long,
    val backingIds: List<Long>,
) : Destination

/** The three top-level destinations a bottom-nav tap can route to. */
internal val rootDestinations: List<Destination> = listOf(Photos, Collections, Settings)
