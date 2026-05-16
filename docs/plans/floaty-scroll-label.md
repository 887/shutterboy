# shutterboy — floaty scroll-label (fast-scrollbar port)

## Status: in progress — Phases A + B shipped; Phase C wire-up + Phase D verification pending

Port of tonearmboy's `FastScrollbar.kt` into shutterboy. tonearmboy
shipped two relevant changes:

- `40da803` — *selection across every library tab, scroll-progress letter bubble.*
  First introduced a single round letter bubble to the LEFT of the
  thumb, visible during scroll-progress / drag, with a 600 ms linger
  fade after input stops. Pattern was explicitly modeled on
  shutterboy's `YearScrubber` linger.
- `4e2ff73` — *fast-scrollbar: section-letter chips on track, replace single-bubble readout.*
  Superseded the single bubble with per-section letter chips placed
  on the track at each section's fractional Y (so all upcoming
  section boundaries are visible at once, not just the current one).
  Linger bumped to 800 ms. Idle state: just the thumb pill, no
  reserved chrome. `sectionLabelFor: (Int) -> String?` API replaced
  with `sectionStarts: List<Pair<Int, String>>`. Cumulative-size math
  in `ScrollbarThumb` already caches `sizesByIndex` and uses it for
  both `totalContentPx` and chip Y placement, so the chips stay
  coherent with the thumb even on merged surfaces where one item
  dwarfs the others.

shutterboy already has `ui/photos/grid/YearScrubber.kt` (Phase C.4)
and `StickyHeaderBanner.kt` (Phase C.5). These cover the year-axis
case but only the year-axis — they do not generalize to name-sort
(letter buckets), size-sort (size buckets), or arbitrary section
keys. tonearmboy's `FastScrollbar` does generalize, and it adds the
draggable thumb (shutterboy's `YearScrubber` is drag-to-scrub against
a derived markers list, not a true thumb that follows scroll
position with a stable hit target).

Decision: **port the tonearmboy composable verbatim into
`ui/common/FastScrollbar.kt`, then plug it into `GalleryTimelineFrame`
behind a `sectionStarts: List<Pair<Int, String>>?` derived from the
current sort axis.** Keep `YearScrubber` only as long as it earns its
keep visually against the new scrollbar — Phase C of this plan
decides whether to remove it.

---

## Phase A — Port the composable — shipped in commit `45357b6`

- [x] **A.1** Copy `app/src/main/java/com/eight87/tonearmboy/ui/common/FastScrollbar.kt` → `app/src/main/java/com/eight87/shutterboy/ui/common/FastScrollbar.kt`. Rewrite the package + import lines. Keep both public overloads (`LazyListState` + `LazyGridState`) — shutterboy uses `LazyVerticalGrid` for the Photos timeline + folder + smart-album surfaces, `LazyColumn` may appear in Phase G search results. — tonearmboy-specific comments (NowPlaying / queue surfaces) rewritten to reference shutterboy's sticky-header + tile mix.
- [x] **A.2** Confirm shutterboy's `RouteScope` / theming compiles against the ported file — `MaterialTheme.colorScheme.primary` / `.secondaryContainer` / `.onSurfaceVariant` are M3 standard, no rename needed. M3 Expressive (per `m3-expressive.md`) does not change these tokens. — `:app:compileDebugKotlin` clean.
- [x] **A.3** Robolectric unit test `FastScrollbarSectionStartsTest`: given a `sectionKeys` list + a `groupedItems` map, the derived `sectionStarts` indices match the (1 header + N items) layout shutterboy uses in `PhotosGrid`. Pure helper extracted to `ui/common/SectionStarts.kt`; the composable stays untested at the unit-test layer (AVD smoke in Phase D). — 5 cases passing (empty, single, multi-section, missing-count → zero, label-order preserved).

---

## Phase B — Sort-aware label derivation — shipped in commit `037bd07`

- [x] **B.1** New pure helper `ui/photos/grid/SectionLabel.kt` — `fun sectionLabelFor(sort: PhotoSort, photo: Photo, locale: Locale, zone, sizeBucketLabels): String`. Cases: `ByDateTaken` / `ByDateAdded` → re-uses `formatMonthBand` (`MAY 2026` form); `ByName` → first letter of `displayName` locale-uppercased, `#` for non-letter / empty leading char (leading-article strip deferred to post-T.E); `BySize` → bucket label resolved via `SizeBucketLabels` value class (caller passes resolved `R.string.photos_size_bucket_*` strings; struct defaults to English for tests).
- [x] **B.2** New pure helper `ui/photos/grid/SectionStarts.kt` — `fun sectionStartsFrom(timeline, sort, locale, zone, sizeBucketLabels): List<Pair<Int, String>>`. Date axes emit at each `MonthYearBand` / `YearBand` (covers Items + Days+ densities — `YearBand` is the density-collapse path consistent with `stickyHeaderLabel`). Name + Size axes walk `PhotoCell` / tile cover photos and emit at each label transition.
- [x] **B.3** Robolectric tests `SectionLabelTest` (9 cases — `ByDateTaken` MMM-yyyy, `ByDateAdded` uses dateAddedMs, `ByName` first-letter, `ByName` hash fallback for non-letter leading char, Turkish-locale uppercase, all 4 size buckets) + `SectionStartsTest` (6 cases — empty, Items density, Days density via YearBand, ByName transitions, BySize bucket transitions, sort-axis swap on identical input produces different starts). Both Robolectric-runner because `Uri.parse` is used in Photo construction.

---

## Phase C — Wire into GalleryTimelineFrame

- [ ] **C.1** `GalleryTimelineFrame` accepts an optional `sectionStarts: List<Pair<Int, String>>?` parameter and forwards it to `PhotosGrid`. Default null preserves current behaviour during rollout.
- [ ] **C.2** `PhotosGrid` derives `sectionStarts` from the current `(timeline, sort)` via `sectionStartsFrom` (B.2) wrapped in `remember(timeline, sort)`. Box-overlays `FastScrollbar(state = gridState, sectionStarts = sectionStarts, modifier = Modifier.align(Alignment.CenterEnd))` on top of the `LazyVerticalGrid`. **Key safety: sort-aware keys per the standing rule in main.md** — the `(sortAxis, sortDirection, itemId)` tuple already gates `items(key = { ... })`, but C.2 also gates `sectionStarts` on `sort` so a sort-axis swap rebuilds the marker list instead of reusing stale indices that no longer point at the new section boundaries. Reference: tonearmboy `d75b542` ("fix LazyGrid duplicate-key crash, drop sort-icon circle indicator").
- [ ] **C.3** Decide: keep `YearScrubber.kt` + `StickyHeaderBanner.kt`, or retire them. **Default decision: retire `YearScrubber`** (the chips cover its job at every sort axis, not just `ByDateTaken`); **keep `StickyHeaderBanner`** (the pill at top-centre serves a different purpose — confirming the section the eye is *in*, not previewing the boundaries ahead). If retired, delete the file + its tests + its sub-step C.4 status note in `main.md` (leave the commit hash visible, mark as superseded).
- [ ] **C.4** Three call-sites benefit automatically since they all delegate to `GalleryTimelineFrame`: `PhotosScreen`, `FolderDetailScreen`, `SmartAlbumDetailScreen`. No per-screen wiring needed.

---

## Phase D — Verification

- [ ] **D.1** AVD smoke on `emulator-5554` per CLAUDE.md's UI-changes-verified-on-AVD rule. Seed photos via `scripts/fetch-test-photos.sh` (Phase L.1 — may be a parallel dep). Walk through: Photos tab default sort (date taken DESC) → confirm `MAY 2026` style chips render on right edge during scroll + linger 800 ms after; swap to name-sort → confirm letter chips appear; swap to size-sort → confirm bucket chips appear; FolderDetail → same; SmartAlbumDetail → same. Pipe screencaps through `magick - -resize 50%` per CLAUDE.md.
- [ ] **D.2** Sort-axis swap regression: rapidly cycle the sort sheet across all 4 axes × 2 directions on a populated Photos tab. Watch logcat for `LazyGrid: Two items have the same key` — must stay clean. Reference: E.6 standing audit, tonearmboy `d75b542`.
- [ ] **D.3** Cold-start regression check per `cold-start-perf.md` Phase G.1 — 5× `am start -W` runs, median must stay under 1300 ms after the port. The composable is local to the grid surface (not the splash path), so the regression risk is low, but the derived `sectionStarts` list runs on every timeline emission and is worth measuring once.

---

## Standing rule (locked once Phase C lands)

Every shutterboy gallery surface that hosts a `LazyVerticalGrid` over
a sortable photo / folder collection MUST wire `FastScrollbar` with
sort-aware `sectionStarts` derived via `sectionStartsFrom`. Direct
`Modifier.verticalScroll` + custom scrubber implementations are
forbidden on new surfaces — go through `GalleryTimelineFrame` so the
floaty chips ship by default.

Reference commits (tonearmboy): `40da803`, `4e2ff73`.
