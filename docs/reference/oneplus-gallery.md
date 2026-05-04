# shutterboy — design doc

Settled before any UI lands. References the screenshot bundle in [`screenshots/`](screenshots/) (Aves Gallery + 2018-vintage OnePlus Gallery crops). Locked decisions feed Phase C / D / E / F / I sub-steps directly.

---

## Visual reference target

**Primary reference: Aves Gallery** ([github.com/deckerst/aves](https://github.com/deckerst/aves)). The Aves screenshot bundle captures the modern pattern shutterboy is targeting: inline scroll-with-content month-year section bands (`MAY 2021` / `APRIL 2021`), filter-chip strip across the top, mosaic-style variable-cell grid, minimal-chrome fullscreen viewer with position indicator + filename + date + size, dual basic/metadata info panels, four-icon bottom nav.

**Secondary reference: OnePlus Gallery** (2018-vintage public crops). Useful for the high-level tab pattern and photo-viewer "Set as" sheet shape; predates the year-scrubber + density-zoom features so falls back to Aves for those.

We do *not* mirror Aves' Flutter look-and-feel verbatim — Compose Material 3 surfaces are the rendering target. The visual language Aves anchors (typography weight on the bands, dim band against surface, tight tile gaps, no bevels) carries over.

---

## Tab structure (A.3)

**Decision: bottom navigation, three top-level destinations.**

| Tab | Icon (outlined / filled) | Surface |
|---|---|---|
| Photos | `Image` / `Image` (filled) | Time-ordered grid, density zoom, year scrubber. The headline screen. |
| Collections | `Collections` / `Collections` (filled) | Smart-album chip row + folders grid. Detail screens reuse the Photos surface engine. |
| Settings | `Settings` / `Settings` (filled) | M3 Expressive grouped-cards root + pill search. |

Why bottom-nav, not top-tabs:

- Each destination is a *distinct* surface, not a filtered view of one library. tonearmboy's `LibraryRoot` + top tabs is the right call when every tab queries the same library through a filter (Songs / Albums / Artists / Genres / Playlists). Photos vs Collections vs Settings are independent surfaces — bottom nav signals that more honestly.
- OnePlus Gallery, Google Photos, iOS Photos, Aves, Fossify all use bottom nav. The convention is set; deviating costs without paying.
- `RouteScope` + per-destination `Register(scope)` is the same pattern either way (refactor-solid R.E), so the choice is purely UX, not architectural.

Search lives in the Photos top-bar action (full-screen overlay, mirroring tonearmboy's `SettingsSearch` pattern).

---

## Density zoom (C.3)

**Four levels, pinch-to-cycle.** Pinch-out = denser (more items visible); pinch-in = aggregate (zoom out to year-level). State enum `PhotosZoomLevel`.

| Level | Columns | Tile shape | Section key | Header text |
|---|---|---|---|---|
| **Items** (default) | 4 | square thumbnail | `month-year` | `MAY 2026` (`headlineSmall`, dim) |
| **Days** | 3 | square thumbnail | `day` | `Mon, May 4` |
| **Months** | 2 | rectangular cover (one tile per month-in-year) | `year` | `2026` (`headlineMedium`) |
| **Years** | 1 | hero cover (one tile per year) | none | (year is the tile label) |

Animations: `animateContentSize` on the `LazyVerticalGrid` parent so column count + tile size transitions are smooth. ~250 ms duration, `FastOutSlowInEasing`.

Settings hook: `PhotosSettings.densityOnLaunch` — Default vs Last-used. Default = always boot to Items; Last-used = restore the previous zoom level.

---

## Year-timeline scrubber (C.4)

**Default: reveal-on-scroll.** A thin draggable strip on the right edge that fades in while the grid is scrolling and for ~600 ms after scroll stops, then fades back out. User can tap-to-jump or drag.

Drag bubble:

| Density | Bubble label format |
|---|---|
| Items | `MAY 2026` (full month-year while dragging) |
| Days | `MAY 2026` |
| Months | `2026` |
| Years | `2026` |

Settings hook: `PhotosSettings.yearScrubber` — Always visible / Reveal on scroll (default) / Off.

Implementation: derive the scrubber's drag-target index from `LazyListState.firstVisibleItemIndex` + an index → year/month map computed once per dataset. Drag → `lazyListState.scrollToItem(targetIndex)`. The fade timer is a `LaunchedEffect` on `firstVisibleItemScrollOffset` change.

---

## Sticky vs scroll-with-content section bands

**Decision: scroll-with-content (Aves pattern), no overlay-pin.** Already locked in C.2.

Implementation: full-width `item(span = { GridItemSpan(maxLineSpan) }) { MonthYearBand(...) }` interleaved between per-month photo runs in the same `LazyVerticalGrid`. Bands scroll naturally with the photos beneath them. No `stickyHeader` (which `LazyVerticalGrid` doesn't support natively anyway), no overlay pin.

Rejected:

- **Overlay pin** (Google Photos style: a translucent banner that floats and updates as you scroll) — feels overweight, and the year-scrubber bubble already shows the current month-year while fast-scrolling. Two indicators of the same fact compete.
- **`stickyHeader` workaround** (custom overlay drawn on top of the grid mirroring the visible-range first item's section key) — more code, doesn't match Aves, doesn't pay for the complexity.

Settings hook: `PhotosSettings.dateHeaderStyle` — Subtle (default) / Bold / Hidden. Subtle = `headlineSmall` weight, dim. Bold = `headlineMedium`, full-opacity. Hidden = no bands at all (the year-scrubber bubble carries the wayfinding alone).

---

## Smart-album catalogue (Phase D — v1)

**v1 ships four smart albums.** Defer the rest to v2 unless something forces a re-prioritisation.

| Smart album | Resolution rule | Default order |
|---|---|---|
| **Camera** | `folder.bucket_display_name == "Camera"` (case-insensitive) | 1 |
| **Screenshots** | `folder.bucket_display_name == "Screenshots"` | 2 |
| **Favorites** | join `PhotoFavoriteEntity` | 3 |
| **Recents** | last 30 days by `dateTakenMs` | 4 |

Deferred to v2: Selfies (requires lens-id heuristic or face-orientation hint), Videos (we're not shipping video in v1), Recently Deleted (requires a tombstone table + delete-undo).

Order is user-customisable via drag-reorder (E.4). Default order above lands when no customisation has happened.

---

## Fullscreen viewer chrome (F.2)

| Behaviour | Setting |
|---|---|
| **Initial state** | Chrome visible on entry. |
| **Auto-hide delay** | 3 seconds of no interaction → chrome fades out. |
| **Tap toggle** | Single tap on the photo toggles chrome visibility. |
| **Pinch / pan / swipe** | Chrome stays in current state (visible or hidden). |
| **Photo change (swipe)** | Chrome stays in current state. |

Settings hook: `ViewerSettings.chromeAutoHideDelaySeconds` — `0` (off, always visible) / `1` / `3` (default) / `5` / `10`.

Top bar contents (left → right): back arrow, favorite toggle, overflow menu (Move to album, Set as wallpaper, Use as contact photo).

Bottom action row contents (left → right): Share, Edit handoff, Info, Delete. Equal-weight `IconButton`s.

Info opens a `ModalBottomSheet` with the EXIF table; not a full-screen sheet, so the photo stays visible behind it.

---

## Multi-select gesture (Phase H)

**Confirm: long-press to enter, tap to add/remove, back/exit gesture to leave.**

| Gesture | Effect |
|---|---|
| Long-press a thumbnail in Idle | Enter Active mode, that id selected. |
| Tap a thumbnail in Active | Toggle membership. |
| Tap top-bar Cancel / system back | Exit to Idle. |
| Long-press a thumbnail in Active | Same as tap (no-op vs the existing selection). |

Selection state via `rememberSelectionState()` — pure transition methods (`enterActive(id)`, `toggle(id)`, `exit()`, `selectAll(allIds)`, `deselectAll()`), unit-testable without Compose (R.D.4 locked).

Top-bar in Active: count + Cancel + Select all + Deselect all. Bottom action bar: Share / Move / Favorite / Delete (sliding up over the bottom nav, which the bar replaces while Active).

---

## Sort + custom-order spec (A.4)

### Sort axes (per surface)

| Axis | Comparator | Default direction |
|---|---|---|
| Date taken | `Photo.dateTakenMs DESC` (newest first) | DESC |
| Date added | `Photo.dateAddedMs DESC` | DESC |
| File name | `Photo.displayName ASC`, intelligent (strip leading articles) | ASC |
| File size | `Photo.sizeBytes DESC` (biggest first) | DESC |

`Direction.ASC | DESC` independent of axis.

`PhotoSort` sealed type at `domain/sort/PhotoSort.kt` — each variant carries its `comparator: Comparator<Photo>`. Repository accepts `PhotoSort` and routes to a Room query that `ORDER BY`s the right column; the domain comparator is the test-scoped equivalent.

### Per-tab persistence

| Surface | DataStore key | Falls back to |
|---|---|---|
| Photos | `photos_sort` | (none — explicit default `ByDateTaken(DESC)`) |
| Collections (folders grid) | `collections_sort` | (none — explicit default `ByName(ASC)`) |
| Folder detail | `folder_sort__$folderId` | `collections_sort` |
| Smart-album detail | `smart_album_sort__$id` | `photos_sort` |

### Custom order

Applies to **albums and the smart-album chip row**, NOT the Photos timeline (which is intrinsically date-ordered).

- **Smart-album chip row:** `List<SmartAlbumId>` in DataStore. User can reorder via drag and hide individual chips. Default order is the catalogue order above.
- **Folders grid:** `List<FolderId>` in DataStore. User-pinned order. New folders that haven't been ordered land at the end. Removing a folder removes its entry.

Drag-reorder uses the `DragReorderColumn` helper from tonearmboy (copy-and-adapt for now; extract to a shared library when a third app needs it).

---

## Settings catalogue first cut (A.5)

Sections + rows. Each row carries its own `@Composable Render(entry)` (R.F.13 locked). Rows co-located per section file (R.F.14 locked) — `ui/settings/sections/{LookAndFeelSection,LibrarySection,PhotosSection,AlbumsSection,ViewerSection,AboutSection}.kt`. One aggregator at `ui/settings/catalog/SettingsCatalog.kt` `flatten`s them.

### Look and Feel

| Row | Setting | Default |
|---|---|---|
| Theme | `ThemeSettings.theme: Theme` (System / Light / Dark) | System |
| Dynamic color | `ThemeSettings.dynamicColor: Boolean` (API 31+) | true |
| Black mode | `ThemeSettings.blackMode: Boolean` (AMOLED true-black) | false |
| Grid density default | `PhotosSettings.densityOnLaunch: DensityOnLaunch` (Default / Last-used) | Default |
| Thumbnail quality | `PhotosSettings.thumbnailQuality: Quality` (Low / Medium / High → Coil request size) | Medium |

### Library

| Row | Setting / Action |
|---|---|
| Manage sources | Navigate to `SourcesSubpage` (SAF picker list + Add / Remove) |
| Rescan photos | Action — confirm dialog, kicks `LibraryScanner.runScan()` |
| Clear cache | Action — confirm dialog, drops Coil disk cache + Room thumb cache |

### Photos

| Row | Setting | Default |
|---|---|---|
| Default sort | `PhotosSettings.defaultSort: PhotoSort` | `ByDateTaken(DESC)` |
| Date-header style | `PhotosSettings.dateHeaderStyle: DateHeaderStyle` (Subtle / Bold / Hidden) | Subtle |
| Year scrubber | `PhotosSettings.yearScrubber: ScrubberMode` (Always / RevealOnScroll / Off) | RevealOnScroll |

### Albums

| Row | Setting | Default |
|---|---|---|
| Default sort | `AlbumsSettings.defaultSort: PhotoSort` | `ByName(ASC)` |
| Hide empty folders | `AlbumsSettings.hideEmptyFolders: Boolean` | true |
| Show smart-album chip row | `AlbumsSettings.showSmartAlbumChips: Boolean` | true |

### Viewer

| Row | Setting | Default |
|---|---|---|
| Chrome auto-hide delay | `ViewerSettings.chromeAutoHideDelaySeconds: Int` (0 / 1 / 3 / 5 / 10) | 3 |
| Double-tap zoom | `ViewerSettings.doubleTapZoom: Float` (1.5× / 2× / 3×) | 2× |

### About

| Row | Source |
|---|---|
| Build version | `BuildConfig.GIT_SHA + BUILD_DATE` (triple-tap → easter-egg modal at Phase K) |
| License | MIT (opens dialog with full text) |
| GitHub | `https://github.com/887/shutterboy` |
| Open-source acknowledgments | static text from `THIRD_PARTY_LICENSES` resource |

### Search overlay

Pill-shaped search bar pinned at the top of the Settings root. Catalog-driven (filters on label / subtitle / `keywords` per row). Results use the same row `Render(entry)` + a breadcrumb subtitle. Tap pops the overlay, navigates to the destination, scrolls into view, flashes the matched row's background for 300 ms (mirror tonearmboy's `SettingsSearch`).

---

## Open questions deferred to later phases

- **Shared element transition** from grid thumbnail → fullscreen viewer (F.7). Compose's shared-element APIs stabilised in 1.7 — pinned Compose BOM 2026.03.01 covers them. Decide between full shared-element transition vs simple cross-fade in F.7 implementation.
- **Slideshow Ken Burns effect** (J.2). Defer to slideshow phase.
- **Map view** for GPS-tagged photos (Aves has it). v2.
- **Face / object recognition.** v2 — out of scope for v1 explicitly per main.md non-goals.

---

## Provenance

Synthesized 2026-05-04 from the reference screenshot bundle + tonearmboy's settled-pattern lessons (M3 Expressive settings, R.D engine, R.E `RouteScope`) + the user's design conversations on tab/scroll-band/cage decisions earlier in the build session. Settles A.2 / A.3 / A.4 / A.5; A.1 was already shipped (11 reference screenshots committed).
