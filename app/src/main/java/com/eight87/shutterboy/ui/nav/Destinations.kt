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
 * Phase G — fullscreen search overlay. Pushed from the Photos TopAppBar
 * search icon. Pill-shaped search field at the top, filter chip row, results
 * grid below; recent searches below the field when it's empty + focused.
 */
@Serializable
data object Search : Destination

/**
 * Phase C.x — About sub-page (mirrors tonearmboy D.16.4 + D.16.5). Pushed onto
 * the Settings tab from the placeholder root row until Phase I builds the full
 * M3 Expressive grouped-cards Settings root with About underneath
 * Settings → Library.
 */
@Serializable
data object SettingsAbout : Destination

/**
 * oss-licenses Phase B — Open-source licenses sub-page. Pushed from the
 * About sub-page row labelled "Open-source licenses". Renders the inventory
 * generated at build time by the Licensee plugin (see `app/build.gradle.kts`).
 */
@Serializable
data object Licenses : Destination

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

/**
 * Phase D.2 — folder detail. Pushed from a folder tile tap on the
 * Collections root. Body is the same density-zoomed timeline as the
 * Photos tab, filtered to `observePhotosInFolder(folderId)`. `FolderId` is
 * a `value class` so we serialize the underlying [Long] and reconstruct
 * at the screen.
 */
@Serializable
data class FolderDetail(val folderIdValue: Long) : Destination

/**
 * Phase J.1 — fullscreen slideshow route. Pushed (eventually, J.3) from
 * the overflow menu of Photos / FolderDetail / SmartAlbumDetail / Search.
 * Carries the backing photo-id list the [HorizontalPager] iterates over.
 * No `initialPhotoId` — slideshows start at page 0 of the scope.
 */
@Serializable
data class Slideshow(val backingIds: List<Long>) : Destination

/** The three top-level destinations a bottom-nav tap can route to. */
internal val rootDestinations: List<Destination> = listOf(Photos, Collections, Settings)
