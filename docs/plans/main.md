# shutterboy — build plan

## Status: in progress (Phase 0 not started)

Modern Android photo gallery, sibling app to [`tonearmboy`](https://github.com/887/tonearmboy). Reference UX target: the OxygenOS 15/16 Gallery — Photos / Collections / Explore tab structure, pinch-zoom density levels (Years → Months → Days → Items), year-timeline fast scrubber, smart albums, multi-select bulk actions. Same minimalist scope discipline as tonearmboy: read-only viewer, no cloud, no editing, no video playback, no ML.

**SOLID discipline is locked into every phase below.** Cross-cutting standing rules + per-phase pre-emption mapping live in [`refactor-solid.md`](refactor-solid.md). Self-check against the cross-cutting rules (ISP narrow facets, DIP no wrong-direction imports, OCP sealed types, SRP file-size heuristics, no `SettingsSnapshot`, composition root) before ticking any phase header.

Reference screenshots in `docs/reference/screenshots/`:

- **`oneplus-gallery/`** — 2018-vintage public crops (tabbed chrome + photo viewer + Set-as sheet). Confirms the high-level tab structure but predates the year-scrubber and density-zoom features. Sourced from a Gadget Hacks tutorial.
- **`aves-gallery/`** — 6 screenshots from the open-source [Aves](https://github.com/deckerst/aves) gallery. **This is the gold reference** for the modern pattern: sticky month-year headers (`MAY 2021`, `APRIL 2021`), filter chips at the top (`Favorite`, `birds`, `South Korea`), mosaic-style variable-size grid, four-icon bottom-nav, minimal-chrome fullscreen viewer with position indicator + filename + date + size, dual info panels (basic vs metadata), statistics + map surfaces. Built in Flutter, but the visual language transfers cleanly to Compose.

Pull more web references as needed — Fossify Gallery, Simple Gallery, Google Photos review crops are all viable. Avoid asking the user to screenshot their own device.

---

## Phase 0 — Bootstrap — shipped in commits `79a9999` (0.1+0.2+0.3), `4e82582` (0.4+0.5+0.6), `224c5a9` (0.7+0.8+0.9)

Scaffold the Android project, get a hello-world cold-boot on `emulator-5554`, and move the tiger artwork into the project tree. Mirror tonearmboy's Phase 0 shape.

- [x] **0.1** Scaffold via `android create --name=shutterboy --output=. <template>` (pick the empty-Compose-activity template). Package: `com.eight87.shutterboy`. minSdk 26, targetSdk + compileSdk = current Android (36 at time of writing). Application id matches package.
- [x] **0.2** Wire Gradle dependencies: Compose BOM + material3 + activity-compose; Coil 3 (`io.coil-kt.coil3:coil-compose`, `io.coil-kt.coil3:coil-network-okhttp` not needed — local only); Room (runtime + ksp + compose paging-room if needed); `androidx.exifinterface:exifinterface`; `androidx.datastore:datastore-preferences`; `kotlinx-serialization-json`; `androidx.navigation:navigation-compose`; Robolectric + JUnit + truth for unit tests. Single `:app` module.
- [x] **0.3** Verify `JAVA_HOME=/usr/lib/jvm/java-26-openjdk ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug` builds clean. Install on `emulator-5554`, confirm cold-boot lands on a Compose "shutterboy" placeholder activity. — `BUILD SUCCESSFUL in 44s`. Cold-boot on `emulator-5554` rendered "Hello Android!" cleanly.
- [x] **0.4** Move `docs/artwork/easter_egg_tiger.png` into `app/src/main/res/drawable-nodpi/easter_egg_tiger.png`. Keep a copy at the original path so the source artwork stays trackable in `docs/`.
- [x] **0.5** First-pass app icon — adaptive launcher with the tiger artwork as the foreground (proper cutout + Pentax SLR composition lands in Phase K). Source at `app/src/main/res/drawable-nodpi/ic_launcher_source.png` (1024×1536 RGBA, tiger fit-into-720×720 centered). Generated `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png` at 108/162/216/324/432 px from the source's center 1024×1024. Monochrome layer at the same densities, alpha-extracted. `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` updated to `<adaptive-icon>` with `@color/launcher_background` (`#1A1717`) + `@mipmap/ic_launcher_foreground` + `@mipmap/ic_launcher_monochrome`. Scaffold's per-density `ic_launcher.webp` / `ic_launcher_round.webp` and the `drawable/ic_launcher_*.xml` vector drawables removed. Verified on `emulator-5554` — tiger renders as "shutterboy" in the app drawer.
- [x] **0.6** Splash screen via `androidx.core.splashscreen` — `Theme.Shutterboy` parent `Theme.SplashScreen` with `windowSplashScreenBackground` = `@color/launcher_background`, `windowSplashScreenAnimatedIcon` = `@mipmap/ic_launcher_foreground`, `windowSplashScreenIconBackgroundColor` = `@color/launcher_background` (so the icon area blends with the splash, doubling the visible icon size to 160dp inside 240dp). `postSplashScreenTheme` = `Theme.Shutterboy.Main`. `MainActivity.onCreate` calls `installSplashScreen()` before `super.onCreate`. AndroidManifest `android:theme` updated.
- [x] **0.7** Wire `scripts/start-avd.sh` and `scripts/build-release-apk.sh` adapted from tonearmboy (rename app references, drop music-specific bits). Confirm `scripts/build-release-apk.sh` builds a `release/shutterboy-0.1-<sha7>.apk`. — Both scripts copied from tonearmboy and `sed`'d through (`tonearmboy → shutterboy`, `TONEARM_RELEASE → SHUTTERBOY_RELEASE`). Verified: `./scripts/build-release-apk.sh` produced `release/shutterboy-0.1-4e82582.apk` (23M) with `latest.apk` symlink and SHA-256 emitted to stdout. End-to-end summary: `build: OK`.
- [x] **0.8** **i18n discipline locked from this phase forward.** Seed `app/src/main/res/values/strings.xml` with `app_name=shutterboy`, the channel-name strings the scaffolding hello-world needs, and a leading XML comment documenting the naming scheme (`<surface>_<role>` lowercase snake; surfaces: `photos_`, `collections_`, `viewer_`, `search_`, `settings_`, `dialog_`, `error_`, `cd_`). Add the lint-anchor: `<resources xmlns:tools="http://schemas.android.com/tools" tools:locale="en">`. Confirm `<application>` doesn't pin a locale (default behaviour follows system locale). **Every UI string from Phase A onward goes through `stringResource(R.string.…)` — no inline `Text("…")` literals on shipped UI.** This eliminates a Phase T.A back-extraction phase entirely; shutterboy starts disciplined, locked from day one.
- [x] **0.9** README "Status" line flips from "Pre-Phase 0" to "Phase 0 shipped — see `docs/plans/main.md`."

---

## Phase A — Reference research + design doc — shipped in commit `061d777`

Capture the visual language we're targeting, document the decisions before any UI lands, so Phases C/D/E/F build against a settled spec.

- [x] **A.1** Reference screenshot bundle. 5× OnePlus Gallery 2018-vintage tabs/viewer + 6× Aves Gallery (inline section bands / filter chips / fullscreen viewer / info panels / stats / map) committed under [`docs/reference/screenshots/`](../reference/screenshots/). Existing crops cover the patterns the design doc needs (inline scroll-with-content month bands, mosaic-grid, minimal viewer chrome, filter chips). No further web pulls required for A.2 — Fossify / Simple Gallery / Google Photos / iOS Photos remain on the table as fallbacks if a future phase reaches a question these don't answer.
- [x] **A.2** [`docs/reference/oneplus-gallery.md`](../reference/oneplus-gallery.md) — written design doc. Settles tab structure, density-zoom levels (Items / Days / Months / Years column counts + tile shapes), year-scrubber behaviour (default RevealOnScroll, bubble label per density), inline-scroll-with-content bands (Aves pattern, no overlay-pin, three rejected alternatives documented), smart-album catalogue (Camera / Screenshots / Favorites / Recents — Selfies / Videos / Recently-Deleted deferred to v2), fullscreen-viewer chrome timing (3 s default auto-hide + tap toggle), and multi-select gesture (long-press + tap-toggle, exit on Cancel / system back).
- [x] **A.3** Tab vs bottom-nav: **bottom nav** (Photos / Collections / Settings). Documented in the design doc with rationale (each destination is distinct, not a filtered view of one library; convention is set across the gallery-app market).
- [x] **A.4** Sort + custom-order spec. Locked: 4 sort axes (Date taken DESC default, Date added, File name ASC default with intelligent leading-article strip, File size); per-surface DataStore keys (`photos_sort`, `collections_sort`, `folder_sort__$folderId`, `smart_album_sort__$id`) with documented fallbacks. Custom order = drag-reorder for the smart-album chip row + the folders grid; does not apply to the Photos timeline.
- [x] **A.5** Settings catalogue + `Setting<T>` abstraction — first cut. Six sections (Look and Feel / Library / Photos / Albums / Viewer / About) with full row inventory + setting types + defaults documented. `Setting<T>(key, default, encode, decode)` + `EnumSetting<E>` abstraction locked. Five facet interfaces (`ThemeSettings`, `LibrarySettings`, `PhotosSettings`, `AlbumsSettings`, `ViewerSettings`). `SettingsSnapshot` forbidden. Each row carries its own `@Composable Render(entry)` (R.F.13). Rows co-located per section file in `ui/settings/sections/` (R.F.14). Catalog aggregator at `ui/settings/catalog/SettingsCatalog.kt` flattens. Search overlay catalog-driven (label / subtitle / keywords) — mirror of tonearmboy's `SettingsSearch`.

---

## Phase B — Data layer — shipped in commits `6968e01` (B.1), `7b1f1d6` (B.2+B.3), `6f9ea68` (B.4), `da45328` (B.5+B.6), `eb55537` (B.7)

Room schema + MediaStore.Images scanner + EXIF cache + multi-source SAF + repository interface. Pure data — no UI in this phase.

- [x] **B.1** Room v1 schema. **One file per entity** (R.F.7 locked): `data/db/PhotoEntity.kt`, `data/db/FolderEntity.kt`, `data/db/PhotoFavoriteEntity.kt`, `data/db/PhotoFts.kt`. No mega-`Entities.kt`. **Domain-vs-scan split** (R.F.4 locked): `domain/Photo` is the cache-faithful read shape that UI consumes; `data/scan/ScannedPhoto` is the scan-only superset (raw EXIF blob, multi-value tag splits, source provenance) that the scanner produces — never imported by UI. `Mapping.toDomain(scanned: ScannedPhoto): Photo` lives at the data-layer boundary. Entities: `PhotoEntity(id, contentUri, displayName, dateTakenMs, dateAddedMs, width, height, sizeBytes, mimeType, folderId, exifLensModel?, exifFocalLength?, exifIso?, exifAperture?, exifShutterSpeed?, latitude?, longitude?)`; `FolderEntity(id, displayName, sourceType: DEVICE|SAF, safTreeUri?, photoCount, coverPhotoId?)`; `PhotoFavoriteEntity(photoId)` for the Favorites smart-album. FTS shadow `PhotoFts(display_name, exif_lens_model)` over PhotoEntity (folder-name match composed at query time via JOIN, decided in B.1 implementation). Domain types also shipped: `Photo / Folder / SmartAlbumId (sealed) / ScanProgress (sealed) / MediaChange / DeleteRequest (sealed) / LibrarySnapshot / sort/PhotoSort (sealed)`. DAOs: `PhotoDao` (with `replaceWithDelta` transaction), `FolderDao`, `PhotoFavoriteDao`, `PhotoSearchDao`. Database `ShutterboyDatabase` v1 with `exportSchema = true`; v1 schema JSON at `app/schemas/com.eight87.shutterboy.data.db.ShutterboyDatabase/1.json`. **R.F.8 dead-code purge done** — scaffold's `data/DataRepository.kt` + `ui/main/MainScreen*` + `Navigation.kt` + `NavigationKeys.kt` + matching test files deleted; MainActivity replaced with a minimal placeholder Compose tree until Phase C lands real navigation. Build green: BUILD SUCCESSFUL in 7s. Cold-boot on `emulator-5554` renders the centered `app_name` placeholder.
- [x] **B.2** `data/scan/MediaStoreScanner.kt` — queries `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` projected to the columns we care about (`_ID`, `DISPLAY_NAME`, `DATE_TAKEN`, `DATE_ADDED`, `WIDTH`, `HEIGHT`, `SIZE`, `MIME_TYPE`, `BUCKET_ID`, `BUCKET_DISPLAY_NAME`). Maps cursor rows to `ScannedPhoto`. Bucket id + bucket name deduplicated into `ScannedFolder` (folder roll-up). Returns a typed `ScanResult(photos, folders)`. Pre-EXIF — enrichment is B.3's concern. Falls back to `DATE_ADDED * 1000` when `DATE_TAKEN` is missing. Runs on `Dispatchers.IO`. Idempotency lives at the repo layer (B.5): scanner returns the full set, repo diffs against cache via `PhotoDao.replaceWithDelta`.
- [x] **B.3** `data/scan/ExifEnricher.kt` — folds EXIF capture metadata into a `ScannedPhoto`: lens model (with `TAG_MODEL` fallback), focal length, ISO, aperture (`TAG_F_NUMBER`), shutter speed (parsed from "1/250"-style fraction or decimal seconds), GPS lat/long. Orientation NOT extracted — Coil 3 handles that transparently. Runs on `Dispatchers.IO` per-photo; failures (unreadable stream, malformed EXIF) silently return the input unchanged. `enrichBatch(scanned: List<ScannedPhoto>)` for repo-side iteration.
- [x] **B.4** `data/saf/SafSourceManager.kt` — multi-source library via SAF. Walks user-picked tree URIs (sourced from `ScanConfigSource.safSourceUris: Flow<Set<String>>` — interface defined in `data/settings/` per R.A.5 so the data layer doesn't import `ui/settings/`). For each tree, recurses via `DocumentFile.fromTreeUri` collecting `image/*` MIME-typed leaves; emits `ScannedPhoto` rows with `source = SAF_TREE`. Image dimensions read via `BitmapFactory.Options(inJustDecodeBounds = true)` without decoding the bitmap. Stable per-tree folder ids via `Long.MIN_VALUE | hash(treeUriStr)` so the SAF id space can't collide with MediaStore bucket ids (which are positive). Revoked-tree URIs silently produce 0 photos for that tree. Returns `SafScanResult(photos, folders)`. The `Intent.ACTION_OPEN_DOCUMENT_TREE` UI + `takePersistableUriPermission` call lives in Phase I.3 (settings → Manage sources). New dep: `androidx.documentfile:documentfile`.
- [x] **B.5** **Eight narrow facet interfaces, not one fat repo** (R.A locked, mirroring tonearmboy's audit lesson). `RoomGalleryRepository` implements all eight; `AppGraph` exposes each separately so consumers depend only on what they read.
  - `PhotoSource` — `observePhotos(sort): Flow<List<Photo>>`, `observePhotosInFolder(folderId, sort): Flow<List<Photo>>`.
  - `FolderSource` — `observeFolders(): Flow<List<Folder>>`, `observeFolder(id): Flow<Folder?>`.
  - `SmartAlbumSource` — `observeSmartAlbum(id: SmartAlbumId): Flow<List<Photo>>`, `observeSmartAlbumCovers(): Flow<Map<SmartAlbumId, Photo?>>`.
  - `PhotoSearch` — `searchPhotos(query, filters): Flow<List<Photo>>`, `recentSearches(): Flow<List<String>>`, `recordSearch(q)`.
  - `LibraryScanner` — `runScan(): LibrarySnapshot` (R.F.6 locked: typed snapshot data class, never raw lists), `scanProgress(): Flow<ScanProgress>`.
  - `FavoriteCommands` — `toggleFavorite(photoId)`.
  - `PhotoDeleter` — `deletePhotos(ids): DeleteRequest` (sealed `Immediate | Consent(IntentSenderRequest) | Failure(reason)`, mirroring tonearmboy's three-branch SDK split).
  - `MediaChangeSource` — `observeChanges(): Flow<MediaChange>` (`ContentObserver`-backed). Phase B keeps the scanner reactive without UI direct-calls.
  Wrong-direction imports forbidden (R.A.5 locked): `data/` never imports `ui/`. Settings → scanner dependency is a `ScanConfigSource` interface defined in `data/`, implemented by `SettingsRepository`.
- [x] **B.6** Smart-album resolution. `SmartAlbumId` sealed type (defined in B.1) with concrete cases `Camera` / `Screenshots` (case-insensitive `bucket_display_name` match via `PhotoDao.observeInBucketByName`), `Favorites` (`PhotoFavoriteDao.observeFavoritePhotos` joins `photo_favorites`), `Recents` (last 30 days by `dateTakenMs`, window constant on `SmartAlbumId.Recents.WINDOW_MS`). `SmartAlbumSource.observeSmartAlbum(id)` `when`-dispatches on the sealed id (exhaustive — adding a new variant compiles or it doesn't). `observeSmartAlbumCovers()` produces `Map<SmartAlbumId, Photo?>` via `combine` over the four album Flows; UI consumes one map and updates atomically.
- [x] **B.7** Robolectric tests — 26 cases across four classes, all green:
  - `data/scan/ExifEnricherTest` (8) — `parseShutterSpeed` covers fractional ("1/250"), decimal, integer-seconds, blank, null, unparseable, divide-by-zero, long-shutter inputs.
  - `domain/sort/PhotoSortTest` (6) — every sort axis + direction permutation + Default sentinel.
  - `data/db/MappingTest` (4) — PhotoEntity↔Photo round-trip preserves every field; FolderEntity→Folder maps DEVICE/SAF source types; garbled `sourceType` falls back to DEVICE; ScannedPhoto→PhotoEntity preserves every cached EXIF + GPS field.
  - `data/repo/SmartAlbumResolutionTest` (8) — in-memory Room DB. Camera bucket case-insensitive match; Screenshots resolves only its bucket; Favorites joins photo_favorites; Recents window filtering; OnConflictStrategy.IGNORE on duplicate favorite-add; defaultOrder matches design doc; fromStorageKey is invertible for every case; replaceWithDelta idempotent on identical re-scan.
  - Robolectric pinned to `sdk = [33]` via `@Config` since `compileSdk = 36` (Android 16) is too new for the current Robolectric jar bundle.

---

## Phase C — App chrome + Photos tab + density zoom + year scrubber — shipped in commits `08e1ee0` (C.1), `10981ae` (C.2), `12f9d98` (C.3), `7baf56e` (C.4 + C.5 + C.6 + C.7)

The headline feature. Bottom-nav scaffold lands here, then the Photos tab on top of it.

- [x] **C.1** Bottom navigation scaffold + `RouteScope` pattern locked from the first route. Three destinations: Photos (`Icons.Outlined.Image` / filled when active), Collections (`Icons.Outlined.Collections`), Settings (`Icons.Outlined.Settings`). Single root `Scaffold` hosts the `NavigationBar`; per-destination content swaps in via `NavDisplay` from `androidx.navigation3.ui`. Top app bar deferred to C.2 (per-tab top bars land with the tab content). **`RouteScope` interface defined** (R.E locked) carrying `graph`, `backStack`, `snackbar`, three narrow facets (`photoSource`, `folderSource`, `smartAlbumSource`); more facets join as later phases need them. Per-destination `Register(scope)` extensions on the sealed `Destination` (NavKey) interface live one file per area: `routes/PhotosRoutes.kt`, `routes/CollectionsRoutes.kt`, `routes/SettingsRoutes.kt`. `ShutterboyBackStack` ships with `selectTab(root)` for tab swaps (single back stack model — tab tap clears + sets `[<root>]`); `popToFirstOrPush` for deeplink-style pushes; `currentTab` derives the bottom-nav selected indicator. **`ShutterboyApp.kt` is 89 LOC** — well under the 150 ceiling. Placeholder `PhotosScreen` / `CollectionsScreen` / `SettingsScreen` until C.2 / D / I land. AVD-verified on `emulator-5554`: tabbing through Photos → Collections → Settings swaps body cleanly + selected indicator follows.
- [x] **C.2** `ui/photos/PhotosScreen.kt` is **scaffold + dispatch only — 28 LOC** (R.D 200-LOC ceiling holds with room to spare). Per-piece sub-files: `ui/photos/grid/PhotoSection.kt` (pure grouping helper + locale-aware band format), `ui/photos/grid/PhotosGrid.kt` (the `LazyVerticalGrid` body, takes the narrow `PhotoSource` facet — not the wholesale repo), `ui/photos/grid/MonthYearBand.kt` (the Aves inline section header), `ui/photos/grid/EmptyPhotosState.kt`. `YearScrubber.kt` lands in C.4. Default density: 4 columns. Each cell uses `coil3.compose.AsyncImage` against the `PhotoEntity.contentUri`. **Inline month-year section headers — Aves pattern, scroll-with-content (NOT pinned to top)** via `item(span = { GridItemSpan(maxLineSpan) }) { MonthYearBand(...) }` interleaved between the per-month photo runs in the same `LazyVerticalGrid`. Band format: `MAY 2026` in `headlineSmall` weight, dim against `onSurfaceVariant`. Locale-aware via `DateTimeFormatter.ofPattern("MMMM yyyy", LocalConfiguration.current.locales[0])` (German "MAI 2026" verified by test). Empty-state shows centered photo-library icon + `photos_empty_title` + subtitle pointing at Settings → Library → Manage sources. Robolectric tests (`PhotoSectionTest`, 6 cases): empty input, multi-month grouping preserves input order, single-month bucket, oscillating non-monotonic input creates separate sections, en-US format, German locale format. AVD-verified empty state cold-boots cleanly on `emulator-5554`; with-photos visual lands in Phase L when `scripts/fetch-test-photos.sh` seeds the AVD.
- [x] **C.3** Density zoom. Pinch gesture (`Modifier.transformable(...)`) cycles the grid through density levels: **Years** (one tile per year, 1 column hero with 16:9 cover), **Months** (~2 columns), **Days** (~3 columns), **Items** (default 4-column). `PhotosZoomLevel` enum with clamped `zoomIn()` / `zoomOut()`; `ZoomAccumulator(level, pendingZoom)` folds Compose's continuous `zoomChange` floats with asymmetric thresholds (1.5× in / 0.66× out) so a steady pinch advances multiple levels without snapping back. `PhotosScreen` owns the accumulator state; pinch dispatched via `rememberTransformableState`. `PhotosGrid` does sealed-dispatch on `TimelineDisplayItem` (`MonthYearBand` / `YearBand` / `PhotoCell` / `DayCell` / `MonthCell` / `YearCell`); per-density `buildTimeline` folder shipped. `CoverTile` (Coil `AsyncImage` + bottom-gradient scrim + bottom-left label-with-count) reused at Days / Months / Years. `animateContentSize` smooths the column-count cross-fade. Tests: `PhotosZoomLevelTest` (11 pure-JUnit cases — clamps + threshold semantics + accumulator state), `BuildTimelineTest` (6 Robolectric cases — per-level fold). 11 + 6 = 17 new cases.
- [x] **C.4** Year-timeline scrubber. `ui/photos/grid/YearScrubber.kt` — right-edge strip that fades in while the grid is scrolling and lingers for 600 ms after; stays visible during a drag. Drag y-fraction maps to a `YearMarker` via the pure helper `yearAtFraction`; jump fires `LazyGridState.scrollToItem(marker.timelineIndex)`. `extractYearMarkers(timeline)` walks the flat timeline once and emits one marker per distinct year (works at every density — Items / Days / Months / Years all carry years on their items). Tap-to-jump supported alongside drag. Drag-bubble shows the active year in `headlineSmall` against `surfaceContainerHighest`. Empty markers → composable returns; gestures noop on the strip when no years are present.
- [x] **C.5** Sticky date headers. `ui/photos/grid/StickyHeaderBanner.kt` — translucent pill at top-centre, `surfaceContainerHigh` @ 92% alpha, fades in / out over 200 ms (`tween`) while `gridState.isScrollInProgress`. Pure derivation in `stickyHeaderLabel(timeline, visibleIndex, level, locale)` walks back from the visible-range first item to the most recent dated band / tile and formats it: `MAY 2026` at `Items`, `2026` at `Days` / `Months` / `Years`. Out-of-range index clamps to last valid item. Banner doesn't intercept touch (composed inside the same `Box` as `LazyVerticalGrid` + `YearScrubber`).
- [x] **C.6** Tap thumbnail → push fullscreen viewer route. New sealed-case `PhotoViewer(photoIdValue: Long, backingIds: List<Long>)` registered in `Destinations.kt` (`@Serializable data class` so navigation3 round-trips through `SavedStateHandle`). `entry<PhotoViewer>` wired in `ShutterboyApp.kt`; `PhotoViewer.Register(scope)` extension in `PhotosRoutes.kt`. `ui/viewer/PhotoViewerScreen.kt` placeholder: `TopAppBar` with back-arrow + the tapped photo id + backing-list size + a phase-F note. `PhotosScreen.onPhotoTap` widened to `(PhotoId, List<Long>) -> Unit` so the grid hands the surrounding photo-id list to the route on every tap. Placeholder strings live under `viewer_placeholder_*` (i18n discipline preserved). Phase F replaces the body without touching the route shape.
- [x] **C.7** Robolectric + pure-JUnit tests. `PhotosZoomLevelTest` (C.3, 11 cases) covers `zoomIn` / `zoomOut` transitions, accumulator threshold semantics, asymmetric in/out, clamps. `ScrubberHelpersTest` (C.4 + C.5, 16 Robolectric cases): `extractYearMarkers` empty / Items / Years / single-year; `yearAtFraction` empty / first / last / clamp / midpoint; `stickyHeaderLabel` empty / Items / Items-walks-back / Days / Years / out-of-range-clamp; sanity checks against `buildTimeline`. Empty-library state validated via `BuildTimelineTest.empty input returns empty timeline at every level` + `EmptyPhotosState` early-return path in `PhotosGrid` (AVD-verified — cold-boot lands on the centred photo-library icon + `photos_empty_title` + subtitle pointing at Settings → Library → Manage sources). 82 unit tests total, 0 failures.

---

## Phase D — Collections tab (folders + smart albums)

- [x] **D.1** `ui/collections/CollectionsScreen.kt` — single `LazyVerticalGrid(GridCells.Fixed(2))` hosts both surfaces: full-span "Smart albums" header + horizontal LazyRow of chips (Camera / Screenshots / Favorites / Recents / + Manage), then full-span "Folders" header + 2-column folder tiles. Smart-album chip taps push `SmartAlbumDetail(storageKey)`; folder taps push `FolderDetail(folderIdValue)`; Manage chip surfaces a snackbar (Phase I.3 lands the real Manage sources page). `SmartAlbumChipRow.kt` + `FolderTile.kt` carved out per R.F.7. Folder covers resolve via the new `FolderSource.observeFolderCovers(): Flow<Map<FolderId, Photo>>` facet method (mirrors `observeSmartAlbumCovers` shape; one batch read per emission). Smart-album covers via the existing `observeSmartAlbumCovers`. Empty-folders state shipped inline (D.4 will do the revoked-SAF re-add CTA).
- [x] **D.2** `ui/collections/FolderDetailScreen.kt` — `Scaffold` + `TopAppBar` with the folder display name + back-arrow; body delegates to `GalleryTimelineFrame` with a `PhotoStream { sort -> photoSource.observePhotosInFolder(folderId, sort) }`. Repository-side filtering preserved (R.F.12 locked). Year-scrubber-only-for-spans-≥-90-days deferred to a later polish pass — for now the scrubber renders against whatever the timeline emits.
- [x] **D.3** `ui/collections/SmartAlbumDetailScreen.kt` — same shape as FolderDetail; resolves the route's `storageKey` via `SmartAlbumId.fromStorageKey()` (unrecognised key renders the back-button + a generic title + an empty body — handles `SavedStateHandle` round-trips from older builds). `PhotoStream { _ -> smartAlbumSource.observeSmartAlbum(id) }`; sort param ignored since smart albums have a fixed v1 order.
- [x] **D.3.5** Engine extract landed **upfront**, not retroactively, since all three surfaces shipped in the same change. `ui/photos/grid/PhotoStream.kt` defines `fun interface PhotoStream { fun observe(sort): Flow<List<Photo>> }`. `ui/photos/grid/GalleryTimelineFrame.kt` hosts the `ZoomAccumulator` state + `rememberTransformableState` + delegates to `PhotosGrid`. `PhotosGrid.kt` now consumes `PhotoStream` instead of the wholesale `PhotoSource` facet — open-for-extension via new `PhotoStream` strategies (e.g. Phase G search results), closed for grid-rendering modification. Three call-sites: `PhotosScreen`, `FolderDetailScreen`, `SmartAlbumDetailScreen` — each is now a one-line `PhotoStream { ... }` + a `GalleryTimelineFrame(stream, ...)`.
- [ ] **D.4** Empty-folder state: "This folder is empty" + a CTA to manage sources if the folder is SAF-mounted but the tree was revoked.
- [ ] **D.5** Robolectric: smart-album chip tap navigates to the right detail; folder tile tap navigates with the right folderId; revoked-SAF folder shows the consent-revoked state with a re-add CTA.

---

## Phase E — Custom sorting

Per-tab + per-folder sort persistence; drag-reorder for the smart-album chip row and the folders grid order.

- [ ] **E.1** `domain/sort/PhotoSort.kt` sealed type: `ByDateTaken(direction)`, `ByDateAdded(direction)`, `ByName(direction)`, `BySize(direction)`. `Direction.ASC | DESC`. Each has a `comparator: Comparator<Photo>`.
- [ ] **E.2** Sort sheet — `ModalBottomSheet` invoked from the top-bar overflow on Photos, Collections, and folder-detail. Radio list of sort axes + ASC/DESC toggle + Apply. Per-tab persistence in DataStore (`photos_sort`, `collections_sort`, per-folder override keyed by folderId — falls back to `collections_sort` if unset).
- [ ] **E.3** Repository extension: `observePhotos(sort: PhotoSort)` / `observePhotosInFolder(folderId, sort)` accept a sort and route to a Room query that ORDER BYs the right column. Domain comparator is the test-scoped equivalent.
- [ ] **E.4** Custom order = drag-reorder for the smart-album chip row (user can hide a chip and reorder remaining ones; persisted as `List<SmartAlbumId>` in DataStore) and for the folders grid (user-pinned order; new folders land at the end). Reuse the `DragReorderColumn` helper from tonearmboy (extract into a published library or copy-and-adapt — probably copy until a third app needs it).
- [ ] **E.5** Robolectric: sort persistence per tab; per-folder override + fallback; drag-reorder mutates persisted order; new folder appended after existing custom order.

---

## Phase F — Fullscreen viewer

The pager + chrome + actions + EXIF panel.

- [ ] **F.1** `ui/viewer/PhotoViewerScreen.kt` — `HorizontalPager` over the surrounding photo-id list. Each page hosts a Coil `AsyncImage` with `Modifier.transformable` for pinch-zoom + pan; double-tap toggles 1× ↔ 2× zoom; pinch range 1× to 6×.
- [ ] **F.2** Chrome — top bar (back, favorite toggle, overflow [Move to album, Set as wallpaper, Use as contact photo]) and bottom action row (Share, Edit handoff via `Intent.ACTION_EDIT`, Info, Delete). Tap-to-toggle chrome with `AnimatedVisibility(slideIn/slideOut)`. Auto-hide chrome after 3 s of no interaction.
- [ ] **F.3** Info bottom-sheet (`ModalBottomSheet`) — EXIF table: Date taken, Camera (make + model), Lens, Focal length, Aperture, Shutter speed, ISO, Dimensions, File size, File path, GPS (formatted lat/long, opens system map intent on tap if present). Read from the cached `PhotoEntity` (no fresh EXIF read — Phase B already enriched).
- [ ] **F.4** Delete — calls `repository.deletePhotos(listOf(photoId))`. Routes the sealed `DeleteRequest` to the system consent dialog via `IntentSenderRequest` on API 30+, falls back to direct `contentResolver.delete` on API 26-28 (mirror tonearmboy's `TrackDeleter` three-branch SDK split). On success, the pager removes the deleted page and advances.
- [ ] **F.5** Share — `Intent.ACTION_SEND` with the content URI + chooser.
- [ ] **F.6** Edit handoff — `Intent.ACTION_EDIT` chooser; we don't ship our own editor in v1.
- [ ] **F.7** Shared element transition from grid thumbnail → viewer page (Compose shared element APIs once stable; placeholder simple fade if not). Decide based on the Compose version pinned in B.0.
- [ ] **F.8** Robolectric: pager ranges over the right id list; favorite toggle persists; delete routes to the right SDK branch on each API; EXIF panel renders cached values without re-reading the file.

---

## Phase G — Search

- [ ] **G.1** Top-bar search action expands into a full-screen search overlay (mirror tonearmboy's `SettingsSearch` pattern). Pill-shaped search field at the top, results below.
- [ ] **G.2** Query → `repository.searchPhotos(q)` → FTS match on `displayName`, `folderName`, `lensModel`; case-insensitive substring fallback for the no-match path.
- [ ] **G.3** Filter chips above the results: Favorites, This year, Has GPS, Folder = X (via a folder-picker bottom sheet). Active chips combine with the text query (AND).
- [ ] **G.4** Tap a result → fullscreen viewer with the result list as the pager backing. Clean back-stack pop returns to the search results, not the tab root.
- [ ] **G.5** Recent searches — last 10 queries persisted in DataStore; shown as chips below the search field when the field is empty.
- [ ] **G.6** Robolectric: query "vacation" returns photos whose `displayName` matches; chip combinations narrow results correctly; recent searches persist + dedupe.

---

## Phase H — Multi-select + bulk actions

- [ ] **H.1** Selection state via **`rememberSelectionState()` with pure transition methods** (R.D.4 locked: hoisted out of the screen body, unit-testable without Compose). `SelectionState` sealed type (`Idle | Active(selectedIds: Set<PhotoId>)`). Pure functions: `enterActive(id)`, `toggle(id)`, `exit()`, `selectAll(allIds)`, `deselectAll()`. Long-press a thumbnail → `enterActive(id)`. Tap in Active → `toggle(id)`. Tapping out / back-button → `exit()`.
- [ ] **H.2** Selection chrome — top bar in Active mode: count + Cancel + Select all + Deselect all. Bottom action bar: Share, Move to album, Favorite, Delete.
- [ ] **H.3** Bulk delete — single system consent dialog covering all selected ids on API 30+ (`MediaStore.createDeleteRequest(uris)`); per-item fallback chain on older APIs. Animate the deleted thumbnails out of the grid.
- [ ] **H.4** Bulk move — folder-picker bottom sheet → `MediaStore.createMoveRequest` (API 30+) / direct copy + delete fallback older. Same consent UX.
- [ ] **H.5** Robolectric: long-press enters Active with the right initial selection; tap toggles; back-button exits cleanly; bulk delete routes to the right SDK branch.

---

## Phase I — Settings

Mirror tonearmboy's M3 Expressive grouped-cards + pill-search settings root. Single source of truth at `ui/settings/catalog/SettingsCatalog.kt` so the search overlay and the sub-pages render from the same registry.

- [ ] **I.1** `SettingsRoot` — pill-shaped search bar pinned at the top, grouped rounded cards below: Look and Feel / Library / Photos / Albums / About. Each card lists its rows with leading icon (`Icons.Outlined.*`), label, subtitle, breadcrumb-aware navigate target.
- [ ] **I.2** Look and Feel sub-page — Theme (System / Light / Dark radio), Dynamic color (toggle, API 31+), Black mode (toggle for AMOLED), Grid density default (Compact / Comfortable / Spacious), Thumbnail quality (Low / Medium / High — controls Coil request size).
- [ ] **I.3** Library sub-page — Manage sources (SAF picker list + Add / Remove), Rescan photos, Clear cache. Source rows show display name + photo count + leading icon (DEVICE = `Icons.Outlined.PhoneAndroid`, SAF = `Icons.Outlined.Folder`). Confirmation dialogs on Rescan + Clear cache.
- [ ] **I.4** Photos sub-page — Default sort, Date-header style (Subtle / Bold / Hidden), Year-scrubber (Always visible / Reveal on scroll / Off), Density on launch (default vs last-used).
- [ ] **I.5** Albums sub-page — Default sort, Hide empty folders toggle, Show smart-album chip row toggle.
- [ ] **I.6** About sub-page — Build version (triple-tap → easter egg modal), License (MIT, opens dialog with full text), GitHub link (`https://github.com/887/shutterboy`), Open-source acknowledgments.
- [ ] **I.7** Search overlay — same pattern as tonearmboy's `SettingsSearch`. Catalog-driven, breadcrumb subtitle, tap pops overlay + scrolls + flashes the matched row for 300 ms.
- [ ] **I.8** Robolectric: catalog wiring (every entry has a unique id + reachable destination), breadcrumb derivation, search filter against label / subtitle / keywords.

---

## Phase J — Slideshow

- [ ] **J.1** `ui/slideshow/SlideshowScreen.kt` — full-screen `HorizontalPager` advancing on a timer (default 4 s, settings-controlled). Tap to pause, tap again to resume. Back-button exits.
- [ ] **J.2** Optional Ken Burns effect — slow zoom + pan during each photo's dwell; togglable in Settings → Photos → Slideshow style.
- [ ] **J.3** Scope picker — slideshow can run over Folder / Smart album / Search results. Launched from the overflow menu of the matching screen.
- [ ] **J.4** Robolectric: timer advances pager; pause stops the timer without losing position; Ken Burns disabled = static photo.

---

## Phase K — Easter egg + launcher icon polish

- [ ] **K.1** Easter egg — triple-tap the build version row in Settings → About reveals a fullscreen modal `Dialog` showing `R.drawable.easter_egg_tiger`. 70% black scrim background. Tap-outside or back-button dismiss. Mirror tonearmboy's `EasterEggController` (tap counter + window-lapse reset + repeatable reveal).
- [ ] **K.2** Launcher icon polish pass. Confirm the tiger cutout + Pentax composition reads at 48×48; refine the monochrome themed-icon layer for Android 13+; verify the splash screen icon and the launcher icon stay visually consistent.
- [ ] **K.3** Robolectric: tap counter behaviour matches tonearmboy's spec (single tap, reveal at three taps, repeatable, window-lapse reset, mixed pattern).

---

## Phase L — Release pipeline + v1.0

- [ ] **L.1** `scripts/fetch-test-photos.sh` — pulls a small set of CC-licensed sample photos from a stable source (e.g. Unsplash CC0 collection or the user's own seed set hosted somewhere stable) into `test-photos/` (gitignored). Tag with synthetic EXIF (`exiftool -DateTimeOriginal=...`) covering at least three different years so the year-scrubber is exercised.
- [ ] **L.2** `scripts/push-test-photos.sh` — `adb push test-photos/. /sdcard/DCIM/shutterboy-test/` + media-scanner kick.
- [ ] **L.3** `scripts/gallery-smoke-test.sh` — exercise scan + Photos tab + density zoom + year scrubber + folder open + viewer pager + delete consent + multi-select bulk delete. Maestro-compatible YAML at `.maestro/gallery-smoke.yaml` for portability.
- [ ] **L.4** `scripts/ui-smoke-test.sh` — exercise tabs, settings, search, slideshow.
- [ ] **L.5** First production release: `scripts/build-release-apk.sh --gh-release` → `v1.0-<sha7>` on `https://github.com/887/shutterboy/releases`. Obtainium picks it up via the README's deep-link.
- [ ] **L.6** README "Status" line flips to "v1.0 shipped" with the release URL.
- [ ] **L.7** Update `.maestro/README.md` (create if needed) with the smoke-test invocation pattern.

---

## Phase T — Translations

**Translations are produced by the user + Claude, per-language, in dedicated sessions.** That's the canonical workflow, not a fallback. Mirrors the [`tonearmboy` translations plan](https://github.com/887/tonearmboy/blob/main/docs/plans/translations.md) — same constraints, same workflow — but skips the back-extraction phase since shutterboy's i18n discipline is locked from Phase 0 (every UI string is already resource-backed).

Locked constraints (mirror tonearmboy):
- No third-party translation service (no Crowdin, Lokalise, Weblate).
- No new build dependency — just Android's built-in `values-<locale>/strings.xml` + a small POSIX shell script for the README progress table.
- Zero CI minutes — `translation-progress.sh` runs locally inside `scripts/build-release-apk.sh`.
- English is canonical. Locale variants are partial overrides; missing keys fall back to English at runtime.

### T.A — extract is N/A

Skipped — shutterboy ships with `stringResource(R.string.…)` from Phase A onward (locked in **Phase 0.8**). Every user-facing string already lives in `app/src/main/res/values/strings.xml` by the time Phase L closes; no back-extraction phase needed. tonearmboy's T.A (357-string mechanical pass) is the cost shutterboy pays nothing for by starting clean.

### T.B — locale infrastructure

- [ ] **T.B.1** Confirm `<application>` doesn't pin a locale (default behaviour follows system locale). Already confirmed in Phase 0.8; this is a re-verification once real UI exists.
- [ ] **T.B.2** Confirm the `<resources xmlns:tools="http://schemas.android.com/tools" tools:locale="en">` lint-anchor is intact in `values/strings.xml` — Phase 0.8 added it; this verifies it survived through Phase L.
- [ ] **T.B.3** AVD locale-switch smoke under `de-DE` with **no** `values-de/` directory yet. Confirm every screen renders English (the fallback path) and nothing crashes on `getString` lookups. This proves the layout works before the first locale lands.
- [ ] **T.B.4** Ship + tick.

**Effort:** XS (1 hour). **Risk:** none.

### T.C — translation-progress script + README markers

- [ ] **T.C.1** Write `scripts/translation-progress.sh` (POSIX shell + sed + grep). Parses `app/src/main/res/values/strings.xml` → set of canonical keys (excluding `translatable="false"`); for each `app/src/main/res/values-<locale>/strings.xml` parses translated keys; computes `done / total`; prints a markdown table with locale display names and a tilde-delimited progress bar (plain ASCII, no rendering surprises on github).
- [ ] **T.C.2** Add `<!-- TRANSLATIONS-START -->` / `<!-- TRANSLATIONS-END -->` markers in `README.md` (new "Translations" section). Script `sed`-replaces between markers; idempotent (byte-for-byte stable on re-run).
- [ ] **T.C.3** Wire into `scripts/build-release-apk.sh` immediately before the `git tag` step. Regenerate the README block; `git diff --quiet README.md` to confirm intentional change vs noise; release commit picks up the updated table.
- [ ] **T.C.4** Sanity tests: golden files under `scripts/tests/translation-progress/` exercising 0%, 100%, partial, and missing-locale cases. Run via `bash scripts/translation-progress.sh --test`.
- [ ] **T.C.5** Verify: README section renders correctly on github.com; auto-update is stable on re-run.
- [ ] **T.C.6** Ship + tick.

**Effort:** S–M (½–1 day). **Risk:** low.

### T.D — README "Translations" section content

- [ ] **T.D.1** 2-sentence intro above the auto-table: *translations are produced by the user + Claude per-language, English is canonical, missing keys fall back to English*. **Not** a "we welcome contributions" pitch.
- [ ] **T.D.2** Linkify each language row in the auto-table to its `values-<locale>/strings.xml` on github so the user jumps straight to "edit this file" from the table.
- [ ] **T.D.3** Ship + tick.

**Effort:** XS (15 min). **Risk:** none.

### T.E — produce locales (one session per language)

This is **the ship vector**. Every supported language lands here, in a dedicated user + tiger session. Standing per-language workflow:

1. User opens a Claude session in this repo, names the target locale.
2. Tiger reads `values/strings.xml` + the editorial brief from CLAUDE.md.
3. Tiger drafts `values-<locale>/strings.xml` with every translatable key, **same key order as `values/strings.xml`** for diff-friendly review.
4. Per-entry user review. Anything off → user redirects → tiger revises in place.
5. Commit signed-off entries; leave anything unconfirmed missing (English-fallback is the right answer, not a placeholder).
6. Run `scripts/translation-progress.sh` to refresh the README table.
7. AVD smoke under the new locale; watch for layout overflow on long compound words (German `flowRow` / `wrapContentWidth` patches as needed).

Per-locale ticks (extend as new languages land):

- [ ] **T.E.1** German (`values-de/`) — user is local, primary review channel.
- [ ] **T.E.2** Next locale — user picks; same workflow.
- [ ] **T.E.3** Next locale — same workflow.
- [ ] (… one sub-step per locale shipped)

This phase is **never "done"** in the conventional sense — it stays open as long as new locales are added. Tick the parent phase header with the commit range when the user declares a particular set of locales the canonical shipped set; reopen later when adding a new one.

**Per-locale effort:** M (½–1 day, mostly editorial review). **Risk:** low. **Blast radius:** `values-<locale>/` + README.

---

## What this plan deliberately does NOT include

- **No `CONTRIBUTING-TRANSLATIONS.md`.** Replaced by the **Translations** section in CLAUDE.md.
- **No PR template addendum** for translation contributions.
- **No "welcome mat" copy** in README pitching community translations.
- **No `<!-- needs-translation -->` placeholder convention** — leave keys missing; Android falls back to English; the progress script counts honestly.
- **No reviewer-pair rule, no language-native verifier requirement** — the user reviews per-entry inside the session.
- **No T.A back-extraction phase** — shutterboy is i18n-disciplined from Phase 0; tonearmboy's 1.5-day mechanical pass is a cost shutterboy doesn't owe.

## Migration path (kept open, not scheduled)

If at some future point the user decides to open community translations:
- Layout is unchanged — `values-<locale>/strings.xml` is the standard Weblate / Crowdin input format.
- Add a `CONTRIBUTING-TRANSLATIONS.md` then.
- Add a PR template addendum then.
- This plan covers none of that work pre-emptively.
