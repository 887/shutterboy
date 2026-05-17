# shutterboy — SOLID design + refactor plan

## Status: 🟢 GREEN — Phase 0 shipped clean; standing rules in force, no drift to recover from yet

---

## Why this file exists

Tonearmboy's [`docs/plans/refactor-solid.md`](https://github.com/887/tonearmboy/blob/main/docs/plans/refactor-solid.md) is a recovery plan — 50 audit findings collapsing five god-objects (`LibraryRepository` 565 LOC, `SettingsRepository` 826 LOC, `PlaybackUiController` 882 LOC, `LibraryScreen` 1528 LOC, `TonearmboyApp` 820 LOC) into six lettered phases. Shutterboy starts post-tonearmboy-audit, so those lessons are **absorbed pre-emptively** into [`main.md`](main.md) — design discipline locked into the right phase instead of refactored out later.

This file tracks:
1. The **cross-cutting standing rules** every phase has to honour (always-on enforcement).
2. A **lessons-absorbed mapping** from tonearmboy's R.A → R.F to shutterboy phases that encode each lesson by design.
3. The **standing backlog** — small items that surface during construction but don't fit a single main-plan phase.
4. The **audit cadence** that catches drift before it grows into another god-object.

When tonearmboy's refactor plan eventually flips to `## Status: ✅ DONE`, that repo's `refactor-solid.md` will become a discipline-tracking doc just like this one. The two converge.

---

## Cross-cutting standing rules (always-on, every phase enforces)

These rules are non-negotiable from Phase A onward. CLAUDE.md's "Design principles — SOLID, applied to Kotlin + Compose" is the long-form reference; this is the operational checklist.

- **ISP — narrow facet interfaces over fat handles.** A composable that needs `track.title + onClick` does not import `LibraryRepository`. The `data/` layer publishes per-concern interfaces (`PhotoSource`, `FolderSource`, `SmartAlbumSource`, `PhotoSearch`, `LibraryScanner`, `FavoriteCommands`, `PhotoDeleter`, `MediaChangeSource`). One `RoomGalleryRepository` implements all of them. The composition root (`AppGraph`) exposes each separately.
- **DIP — wrong-direction imports are bugs.** `data/` never imports `ui/`. `playback/` (or `service/`) never imports `MainActivity`. If a service needs an Activity intent, define a `SessionActivityIntentFactory` interface and inject it.
- **OCP — sealed types over `when`-over-type chains.** Tab dispatchers, filter conditions, navigation destinations, smart-album resolution, `DeleteRequest` outcomes — all sealed. Adding a new variant compiles or it doesn't; it never silently misses a branch.
- **SRP — file-size soft heuristics.** ~500 LOC of non-trivial Kotlin = second look. ~800 LOC = almost always needs splitting. Five reasons-to-change in one file = split it now even if it's 200 LOC.
- **No `SettingsSnapshot` mega-flow.** Every preference is a `Setting<T>` value type. Sub-pages take their facet (`ThemeSettings`, `LibrarySettings`, `PhotosSettings`, `AlbumsSettings`, `ViewerSettings`) and read narrow `Flow<T>`s directly. Combining them all into one snapshot Flow causes cross-screen recomposition (toggling theme should not recompose the photos timeline).
- **Composition root pattern.** `AppGraph` is the *only* place that knows concrete types. ViewModels and composables take interfaces or function-typed params.
- **i18n discipline (already locked in Phase 0.8).** Every user-facing string through `stringResource(R.string.…)`. No inline `Text("…")` literals on shipped UI.
- **Per-phase SOLID self-check before ticking the phase header.** Walk the cross-cutting rules against the diff. If any fail, the phase isn't done — fix or open a backlog item below.

---

## Phase R.A — narrow data interfaces (pre-empted in main.md Phase B)

**Lesson absorbed pre-emptively.** Tonearmboy's R.A was 8 sub-steps to extract focused readers from a 30-method `LibraryRepository`. Shutterboy's [`main.md` Phase B.5](main.md) ships those facets as the *first* repo design — there's no fat repo to extract from.

- [x] **R.A.1** Eight narrow facets defined as the canonical repo shape: `PhotoSource`, `FolderSource`, `SmartAlbumSource`, `PhotoSearch`, `LibraryScanner`, `FavoriteCommands`, `PhotoDeleter`, `MediaChangeSource`. Locked into Phase B.5.
- [x] **R.A.2** `RoomGalleryRepository` implements all eight (single class, multiple narrow contracts). Locked into Phase B.5.
- [x] **R.A.3** `AppGraph` exposes each interface separately. No `@Deprecated libraryRepository` migration needed — there's no fat repo to deprecate. Phase B's first commit lands the right shape.
- [x] **R.A.4** UI call sites take the narrow interface from the start (Phase C onward).
- [x] **R.A.5** Data → UI imports are forbidden by the cross-cutting rule above. `ScanConfigSource` interface defined in `data/` for the settings → scanner dependency; `SettingsRepository` implements it (Phase A.5 + Phase B.4).
- [x] **R.A.6** `RoomGalleryRepository` constructor takes all collaborators explicitly from `AppGraph`. No constructor self-defaults.
- [ ] **R.A.7** Verification gate: when Phase B ships, audit the actual repo against these rules.

---

## Phase R.B — settings: `Setting<T>` + facets (pre-empted in main.md Phases A.5 + I)

**Lesson absorbed pre-emptively.** Tonearmboy's R.B compressed ~300 LOC of `stringPreferencesKey + Flow + setter + snapshot field` boilerplate behind a `Setting<T>` value type and split a 27-field `SettingsSnapshot` into five facets. Shutterboy ships the abstraction *first*.

- [x] **R.B.1** `Setting<T>(key, default, encode, decode)` + `EnumSetting<E>` value types defined in `data/settings/` before the first preference key lands. Locked into Phase A.5.
- [x] **R.B.2** Every preference declared as a `Setting<T>` from day one. No hand-rolled quartet boilerplate.
- [x] **R.B.3** Facet interfaces from day one: `ThemeSettings`, `LibrarySettings`, `PhotosSettings`, `AlbumsSettings`, `ViewerSettings`. `SettingsRepository` implements all five.
- [x] **R.B.4** Sub-pages take only their facet — never the wholesale `SettingsRepository`.
- [x] **R.B.5** `SettingsSnapshot` mega-Flow forbidden by the cross-cutting rule above.
- [x] **R.B.6** UI-only helpers (theme picker options, tab-order parsing) live in `ui/settings/` from day one, never in the data repo.
- [ ] **R.B.7** Verification gate: when Phase I ships, audit the actual settings layout against these rules.

---

## Phase R.C — controller-split discipline (no playback equivalent in shutterboy)

**Mostly N/A** — shutterboy has no playback controller. The *pattern* (don't let one class own connection-lifecycle + state + commands + listener-callbacks across six axes) generalises to:

- The gallery scanner (`MediaStoreScanner` + `ExifEnricher` + `SafTreeWalker` already split in Phase B.2/B.3/B.4 — good).
- Any future cross-cutting controller (slideshow timer, multi-select state hoisting, etc.).

- [x] **R.C.1** Phase B.2/B.3/B.4 split scanning into three pure components by axis. No mega-`GalleryController`.
- [ ] **R.C.2** Watch for accidental controller-bloat during Phase C–G; flag as a backlog item if a single class crosses ~5 axes.

---

## Phase R.D — UI surface split + `GallerySurface<T>` engine (pre-empted in main.md Phase C+D)

**Lesson absorbed pre-emptively.** Tonearmboy's R.D extracted a `TabSpec<T>` strategy from a 1528-LOC `LibraryScreen.kt` housing five near-duplicate tab dispatchers. Shutterboy bakes the strategy in *before* the duplicates form.

- [x] **R.D.1** `PhotosScreen.kt` is scaffold + top-bar + dispatch only (target ~200 LOC). Per-piece sub-files: `tabs/PhotosGrid.kt`, `tabs/MonthYearBand.kt` (the Aves pattern), `tabs/YearScrubber.kt`, `multiselect/SelectionBar.kt`, `multiselect/SelectionState.kt`, `tabs/EmptyState.kt`. Locked into Phase C.
- [x] **R.D.2** `GallerySurface<T>` interface (`observe(filter)`, `sectionKey(item)`, `comparator(sort)`, `Tile(item, callbacks)`) defined when Phase D adds the second similar surface. One renderer engine, multiple strategies.
- [x] **R.D.3** Multi-select state via `rememberSelectionState()` — pure transition methods, unit-testable. Locked into Phase H.
- [x] **R.D.4** Sort comparators in pure `LibrarySorting.kt` (or `PhotoSorting.kt`) — testable without Compose.
- [ ] **R.D.5** Verification gate: when Phase D ships and there are two `GallerySurface` impls, audit for duplicate logic that should have been in the engine.

---

## Phase R.E — `ShutterboyApp` shrink: `RouteScope` + per-route `Register(scope)` (pre-empted in main.md Phase C chrome + Phase E + the navigation work)

**Lesson absorbed pre-emptively.** Tonearmboy's R.E shrunk `TonearmboyApp.kt` from 820 LOC to 149 LOC by extracting `RouteScope` + per-destination `Register(scope)` extensions. Shutterboy ships that pattern from the *first* route after MainActivity.

- [x] **R.E.1** `RouteScope` interface defined when the second route lands. Carries `graph`, `backStack`, `snackbar`, narrow facets, command interfaces.
- [x] **R.E.2** Per-destination `Register(scope)` extensions on the sealed `Destination` interface, grouped per area: `routes/PhotosRoutes.kt`, `routes/CollectionsRoutes.kt`, `routes/ViewerRoutes.kt`, `routes/SettingsRoutes.kt`.
- [x] **R.E.3** `ShutterboyApp.kt` ceiling: 150 LOC. Anything beyond means a route renderer leaked back inline; extract.
- [x] **R.E.4** Cross-cutting concerns (SAF picker for delete-consent, share-intent launcher, slideshow scope picker) lifted into `remember*Controller` helpers. Not inline in `ShutterboyApp`.
- [x] **R.E.5** `SessionActivityIntentFactory` interface defined when the first background scope (rescan worker, watcher service) needs an Activity intent. Service layer never imports the UI module.
- [ ] **R.E.6** Verification gate: at end of Phase E, `ShutterboyApp.kt` LOC count audited against the 150 ceiling.

---

## Phase R.F — standing polish backlog (open items)

Independent small wins that don't block phases. Pick whichever lands in front of the next feature you touch.

- [x] **R.F.1** Unify the row composables behind one composable + sealed action type, the moment a second similar row appears (e.g. `PhotoRow` in viewer info-sheet vs `PhotoTile` in grid → factor `PhotoRowContextAction` sealed). STANDING — trigger not yet fired, audited 2026-05-17. Searched for `*Row.kt` / `*Tile.kt` composables: only `SettingsRow`, `FolderTile`, `CoverTile` exist. `FolderTile` (Column: square cover above name+count) and `CoverTile` (Box: scrim overlay with label inside) have different layouts; the only shared concept is "cover image + label". No `PhotoRow` in viewer info-sheet (`ExifInfoPanel.kt` uses its own internal row layout). Premature to unify two divergent tile shapes.
- [x] **R.F.2** Per-variant `ConditionUi` registry on `FilterCondition` (label + summary + `@Composable Editor` + default factory) when search filter chips ship in Phase G. Shipped in commit (worktree) — added `ConditionUi` data class + `SearchFilterUi` registry on `SearchFilter` carrying per-variant `labelRes` + `enabled(ChipEnabledContext)` lambda. Chip row in `SearchScreen.kt` now iterates `SearchFilter.entries` and looks up `SearchFilterUi[filter]` instead of `when`-ing twice (label + enabled). Folder chip stays its own composable — runtime-resolved label + clear affordance asymmetry doesn't fit the boolean-chip abstraction. Tests green: `SearchFilterTest` (5).
- [x] **R.F.3** Extract shared transport rows the moment a second instance appears (e.g. fullscreen viewer chrome vs slideshow controls). STANDING — trigger not yet fired, audited 2026-05-17. Searched `PhotoViewerScreen.kt` (Share / Edit / Favorite / Delete + Back chrome row) vs `SlideshowScreen.kt` + `SlideshowOverlay.kt` (no Share/Edit/Delete/Favorite controls — slideshow transport is play/pause only). No structural overlap to extract.
- [x] **R.F.4** `Photo` (cache-faithful domain) vs `ScannedPhoto` (scan-only superset with EXIF + multi-value tags) split from day one. Locked into Phase B.1.
- [x] **R.F.5** Frozen migration concerns (e.g. legacy JSON parsing) live in a sibling object next to the live type, not in the live type itself. Standing rule.
- [x] **R.F.6** `runScan()` returns a typed `LibrarySnapshot` data class, not raw lists. Locked into Phase B.5.
- [x] **R.F.7** One file per Room entity (`PhotoEntity.kt`, `FolderEntity.kt`, `PhotoFavoriteEntity.kt`). Locked into Phase B.1.
- [x] **R.F.8** Scaffold vestiges purged in Phase B.1 (see main.md B.1 — `DataRepository.kt` / `MainScreen*` / `Navigation.kt` / `NavigationKeys.kt` deleted; MainActivity replaced with a placeholder, then the real Compose root in Phase C). Re-verified clean on the 2026-05-17 sweep — none of those files exist anywhere under `app/src/main/` or `app/src/test/`.
- [x] **R.F.9** Extract a `MediaChangeObserver` shared between the scanner's reactive trigger and the (future) optional background watcher service. One debounce policy. STANDING — trigger not yet fired, audited 2026-05-17. Searched for `RescanService` / background watcher / `MediaChangeObserver`: none exist. The only `MediaChangeSource` consumer is `RoomGalleryRepository`'s reactive scan trigger (in-process). No second consumer to deduplicate against.
- [x] **R.F.10** Drop `runBlocking` if it appears anywhere in the scanner; everything `suspend` from the start. STANDING — trigger not yet fired, audited 2026-05-17. `grep -r 'runBlocking' app/src/main/java/com/eight87/shutterboy/data/` returns zero hits. Scanner code is fully `suspend`-driven.
- [x] **R.F.11** Service-layer split discipline if Phase B ships a `RescanService` — keep `onCreate` to wiring only. STANDING — trigger not yet fired, audited 2026-05-17. No `RescanService` exists. Scanner runs in-process via `LibraryScanner.scanIfChanged()`; no Android Service component shipped.
- [x] **R.F.12** Repository-side filtered Flows from day one. Detail screens (folder detail, smart-album detail) read `observePhotosInFolder(folderId)` / `observeSmartAlbum(id)`, never client-side `.filter` over the unfiltered Flow. STANDING-honored, audited 2026-05-17. `FavoritesScreen` uses `favoriteCommands.observeFavoritePhotos()` (repo-side filter). `FolderDetailScreen` uses `scope.photoSource.observePhotosInFolder(folderId, sort)`. No client-side `.filter` over a wide Flow in either detail screen.
- [x] **R.F.13** Settings catalog rows carry their own `@Composable Render(entry)` — no central `RowKind` `when` chain. Locked into Phase A.5/I. STANDING-honored, audited 2026-05-17. `SettingsRow` is a generic parameterised composable (icon + label + subtitle + trailing slot); sub-pages dispatch row rendering directly. The search overlay renders results via the uniform `SearchResultRow` in `SettingsSearchScreen.kt` — no `RowKind` discriminator, no central `when` chain. The `SettingsCatalogEntry` carries metadata + `navigate` lambda, not a `Render` — adopting per-entry render would only be needed if/when search results need variant-specific composables; for the current uniform-list overlay the uniform row is correct.
- [x] **R.F.14** `SettingsCatalog` rows co-located per section file (`ui/settings/sections/PhotosSection.kt` etc.); one aggregator `flatten`s. No central edit-magnet. Shipped in commit (worktree) — extracted `AppearanceSectionCatalog.kt`, `LibrarySectionCatalog.kt`, `PhotosSectionCatalog.kt`, `AboutSectionCatalog.kt` under `ui/settings/sections/`, each owning its own `ID_*` consts + `entries()` list. `SettingsCatalog.kt` is now a thin aggregator that re-exposes the consts (for `SettingsCatalogTest` pin-down) and flattens the per-section lists. `SettingsCatalogTest` (4) green; `SettingsSearchFilterTest` (6) green; expected-id pin-down assertion unchanged.
- [ ] **R.F.15** Group enum / `GroupRef` discipline if it comes up — render order via list position, never via enum-ordinal hack.
- [ ] **R.F.16** Helper-name accuracy gate: if you write a function whose name implies one contract but the impl does another, fix the impl OR rename the function. Standing rule.
- [ ] **R.F.17** `rememberSettingPicker<T>(...)` helper before the first picker boilerplate gets duplicated.
- [ ] **R.F.18** Callback grouping by audience (`PhotoInteractions` / `NavInteractions`) when a tile takes more than ~4 unrelated callbacks.
- [ ] **R.F.19** Dialog-state sealed types from the first multi-state dialog. No `var showXyzDialog by remember { mutableStateOf(false) }` chains.

---

## Audit cadence

- **Per-phase self-check.** When ticking the final sub-step of any main-plan phase, walk the cross-cutting rules against the diff. Soft heuristics: file LOC counts, ISP (does this composable take a wider interface than it reads?), DIP (any data → ui imports? service → activity?), OCP (any new `when (x: Type) { is A → ; is B → }` chains that should be sealed-dispatched?). If anything fails, fix-or-flag-as-backlog before ticking.
- **Mid-phase quick audits.** Spot-check a randomly-chosen file at ~50% through each phase. If file is over 500 LOC or a god-object is forming, flag a backlog item.
- **End-of-major-phase formal audit.** When Phase B ships (data layer settled), Phase E ships (chrome + nav settled), and Phase L ships (v1.0): spawn a multi-agent SOLID audit (4 parallel subagents reviewing data / ui-photos+collections / ui-settings / nav+session). Reports collapse into new R.F items. Mirror tonearmboy's 2026-05-03 four-agent audit cadence.
- **Standing backlog hygiene.** When an R.F item lands, tick it with a commit ID. When a new finding surfaces, append a new R.F item rather than letting it drift in chat.

---

## How to use this plan

- **Default mode:** the lessons under R.A → R.E are *already locked* into main.md's phase definitions. No separate work to do — just enforce them as each phase ships.
- **Standing rules** are eternal — the cross-cutting list at the top is enforced every commit.
- **R.F items** are independent small wins; pick whichever is in front of the next feature, or save for the end-of-major-phase audit.
- **Audit findings** land as new R.F items with the tonearmboy-style suffix (e.g. `Data-F1`, `UI-F1`) for traceability if multi-agent audits are run later.
- **When this plan flips to `## Status: ✅ DONE`,** every R.F item is ticked AND the three formal audits (post-B, post-E, post-L) have landed clean.

## Audit provenance

Synthesized 2026-05-04 from tonearmboy's [`docs/plans/refactor-solid.md`](https://github.com/887/tonearmboy/blob/main/docs/plans/refactor-solid.md) — the lessons of a 50-finding four-agent SOLID audit (run 2026-05-03 against tonearmboy commit `9388357`) absorbed pre-emptively into shutterboy's design before the equivalent god-objects could form. No shutterboy-specific audit run yet; first formal audit lands at end-of-Phase-B.
