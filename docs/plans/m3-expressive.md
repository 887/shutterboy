# shutterboy — Material 3 Expressive (M3E) plan

## Status: 🟡 PLANNED — Phase 0 inventory complete, Phase A next

## Why this exists

Android 16 / Android 16 QPR1 (Sept 2025) shipped a system-wide redesign
on **Material 3 Expressive (M3E)** — vibrant per-row coloured circular
icon avatars, an elevated `surfaceContainer*` ladder so grouped cards
read as lifted against a flatter page, divider-less stacks, M3E motion /
shape / typography defaults. Baseline M3 (what shutterboy was scaffolded
on) reads flat / monochrome by comparison.

Sister project **tonearmboy** ran ahead and shipped the migration there
(plan: [`tonearmboy/docs/plans/m3-expressive.md`](../../../tonearmboy/docs/plans/m3-expressive.md);
commits `54eaa85` opt-in + initial splash, `4bae805` Phase B+C + first
splash shrink, `2bb042e` auto-accent + retune splash to 60 %). Five
findings surfaced; this plan absorbs them up front so shutterboy
doesn't repeat the discoveries.

Two consequences for the build:

1. **Migrate the surfaces shutterboy already ships** (Photos / Collections /
   Settings-About / Viewer-stub, plus the chrome that wraps them).
2. **Design unshipped phases (F Viewer / G Search / H Multi-select /
   I Settings / J Slideshow / K About-overlay) with M3E patterns from
   day one** so we don't ship a baseline-M3 surface and re-migrate it
   in the next quarter.

## Findings shipped in tonearmboy (apply here too)

1. **`material3:1.4.0` keeps `MaterialExpressiveTheme` /
   `expressive*ColorScheme` `internal`.** The Compose BOM
   `2026.03.01` resolves to `material3:1.4.0` but the Kotlin metadata
   marks the expressive APIs `internal` even though the JVM bytecode
   is public. Override the BOM in `gradle/libs.versions.toml` with an
   explicit `composeMaterial3 = "1.5.0-alpha18"` (the alpha that
   promoted them). Drop the override once `1.5.0` stable lands
   (Phase F here).
2. **`expressiveDarkColorScheme()` does NOT exist in 1.5.0-alpha18** —
   only `expressiveLightColorScheme()` ships. Dark mode stays on
   `darkColorScheme(...)` and inherits the surface-tier ladder.
3. **`surfaceContainer` is too quiet on AMOLED-leaning dark
   palettes.** Use `surfaceContainerHigh` for the card container
   colour in dark. Light mode at `surfaceContainer` is fine.
4. **Auto-derive accent from row `id` at the row composable**, not at
   every call site. Pattern: `accent ?: id?.let { accentFor(it) }`
   inside `SettingsRow`. Direct callers (a hand-rolled About / Licenses
   that bypasses bindings) get the avatar for free.
5. **Android 12+ splash icon is hard circle-clipped** — the layer-list
   `android:windowBackground` workaround does NOT work; the system
   splash paints `windowSplashScreenBackground` over the whole window.
   Working approach: ship `mipmap-<d>/ic_launcher_splash.png` per
   density with the design shrunk to **60 %** + transparent padding.
   tonearmboy's first attempt at 70.7 % (1/√2) left corners grazing
   the mask; 60 % gives proper headroom. Wire as
   `windowSplashScreenAnimatedIcon = @mipmap/ic_launcher_splash` +
   `windowSplashScreenIconBackgroundColor = @color/launcher_background`
   so the bigger 240-dp icon area kicks in.
6. **(shutterboy-only)** Splash work already shipped in commits
   `723f6f9` (initial), `6af4b3e` (full square art), `68e23a5`
   (70.7 % shrink), `837fa25` (revert custom splash; system circle
   accepted). **Splash is locked — do not re-touch under this plan.**
7. **The two-stage Compose splash bypass does NOT pay off.** tonearmboy
   tried it (`f249b0e` — empty AVD on `windowSplashScreenAnimatedIcon`
   + `SplashOverlay` composable for ~700 ms) and reverted in `51d1769`:
   the square overlay was visible for ~half a second before fading and
   wasn't worth the moving parts. Even Google's own M3E reference app
   (`github.com/android/androidify`) ships zero custom splash and
   accepts the system circle. Don't try the bypass under this plan.
8. **App-root `Scaffold` + per-screen `TopAppBar` double-consumes the
   status bar inset.** tonearmboy bug fixed in `5bb2fd2`. The pattern:
   the outer app-shell `Scaffold` (the one with the bottom-nav) wraps
   `NavDisplay` and applies `innerPadding.calculateTopPadding()` (=
   status bar inset) to every NavDisplay child; each child screen's
   inner `Scaffold` + `TopAppBar` then consumes the inset *again* via
   its default `windowInsets`. Result: every chrome screen reads
   double-padded under the status bar. **Fix: set the outer Scaffold's
   `contentWindowInsets = WindowInsets(0)` so inner-screen TopAppBars
   own the inset.** Shutterboy has the same pattern at
   `ui/nav/ShutterboyApp.kt:60`. Phase B addresses this.
9. **Don't reach for `TopAppBar(expandedHeight = …)` overrides as a
   bandaid for double-inset.** tonearmboy went 64 → 48 → 32 dp
   (`2da7c07`, `3903d71`) chasing the symptom, then dropped the
   override entirely in `088390d` once the real inset bug was fixed.
   The M3 default 64-dp expanded region reads correctly with a single
   inset; only override if the visual evidence (after the inset fix)
   demands it. The `expandedHeight` parameter was promoted to stable
   in `material3:1.5.0-alpha18`.
10. **(future-proofing — does not affect shutterboy v1, capture for
    the record.)** When a chrome-tint feature lands (e.g. tinting the
    page surface from a folder cover or a smart-album cover), blend
    the FULL `surfaceContainer{Lowest..Highest}` ladder *and*
    `secondaryContainer` alongside `surface` / `surfaceVariant` /
    `background`. tonearmboy fixed this in `7876789` after Finding 5
    above bit them. Sticky headers, tile placeholders, multi-select
    bars all pull from the container ladder; if the tint only blends
    the bottom rung, those elements drift visibly off the page colour.
11. **Active-state colours stay pinned, not theme-derived.**
    tonearmboy's queue-active-row hijack (`7876789`): when a user
    picks an orange primary, the active-row colour came from
    `primaryContainer` and went orange too. Fix: pin to a stable
    `Color(0xFF…)`. **Carry this into shutterboy's main.md Phase H
    multi-select chrome** — the selected-tile / selection-bar accent
    should be a stable colour, not `primaryContainer`. Phase F.3
    folds this into the multi-select plan.
12. **Multi-select tile-selection visual** (from `7876789`): 3-dp
    primary border + cover dim to 0.55 alpha + filled-check badge in
    the top-left. `BackHandler` in selection mode collapses the
    selection rather than popping the screen. Phase F.3 carries this
    into main.md Phase H.

## The four M3E patterns (locked)

(All four together produce the "happy" Settings look. Each is cheap
in isolation; the combination matters.)

1. **Surface tier ladder.** Page background = `colorScheme.surface`;
   grouped cards / row clusters = `colorScheme.surfaceContainer` (light)
   or `colorScheme.surfaceContainerHigh` (AMOLED dark). Do NOT use
   `surface` for both. Don't lean on shadow elevation —
   `defaultElevation = 0.dp` with the surface-tier token does the lift.
2. **`MaterialExpressiveTheme`** in place of plain `MaterialTheme` at
   the app theme entry. Pulls in M3E motion / typography / shapes.
   Requires `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` until
   `material3:1.5.0` ships stable.
3. **Coloured circular row-icon avatars.** `Box(Modifier.size(40.dp)
   .clip(CircleShape).background(accent.container))` wrapping a `24.dp`
   filled `Icon(tint = accent.onContainer)`. `accent` is per-category
   from a `data class CategoryAccent(val container: Color, val onContainer: Color)`
   defined alongside the theme — M3 only ships three container pairs
   (`primary` / `secondary` / `tertiary`) and the system Settings
   spreads ~6 hues. Hand-pick.
4. **Filled glyph icons** at the avatar layer (`Icons.Filled.*`,
   not `Icons.Outlined.*`). Outlined glyphs read weak inside a
   coloured circle.

Two supporting niceties:

- Card shape: `RoundedCornerShape(28.dp)` or
  `MaterialTheme.shapes.extraLarge`.
- Divider-less list rows inside a card: `Column(verticalArrangement
  = Arrangement.spacedBy(2.dp))`. The surface-tier gap between cards
  does the visual separation; in-card rows just stack.

## Constraints (locked)

- **Follow upstream M3 token names.** Use `surfaceContainer*`, not
  invented palette names like `cardBackground`.
- **Hand-picked per-category accents.** Driving the row avatars off
  `dynamicDarkColorScheme(LocalContext.current)` would let the user's
  wallpaper-derived palette steamroll category intent (a green
  wallpaper would drift the Photos-pink accent off-pink). Document the
  decision in `theme/Theme.kt`.
- **Filled icon set scoped narrowly.** Sweep `Icons.Outlined.*` →
  `Icons.Filled.*` at avatar / row-leading-icon sites only. Don't
  churn icons inside the photo viewer chrome / EXIF panel / sort
  sheet — those read filled already.
- **Splash is locked** (commits above). Do not revisit under this plan.
- **Dynamic color stays available.** `Theme.kt` currently picks
  `dynamicDarkColorScheme()` when `dynamicColor = true && API ≥ 31`.
  Keep that path — Phase I.2 surfaces a Settings toggle for it. M3E
  applies to the *static* fallback palette + the surface-tier discipline
  + the avatar pattern; dynamic color users already get expressive
  containers from the Material You system palette. Verify both branches
  in Phase B.4.
- **i18n discipline carries.** Every user-facing string lands in
  `values/strings.xml` with `<surface>_<role>` keys.

## Surface inventory — what exists today (`2026-05-06`)

Authoritative as of HEAD = `31d25d8`. Each surface gets a row in the
Phase B audit + the Phase E sweep below.

**Theme entry:**
- `theme/Theme.kt` — `ShutterboyTheme` wraps `MaterialTheme(colorScheme,
  typography)`. `darkColorScheme()` / `lightColorScheme()` static
  fallbacks; `dynamicDarkColorScheme()` / `dynamicLightColorScheme()`
  for API 31+. **A.3 / A.4 target.**

**Top app bars (6 callers):**
- `ui/photos/PhotosScreen.kt` — title + sort-overflow.
- `ui/collections/CollectionsScreen.kt` — title + reorder-overflow.
- `ui/collections/FolderDetailScreen.kt` — back + title + sort-overflow.
- `ui/collections/SmartAlbumDetailScreen.kt` — back + title.
- `ui/viewer/PhotoViewerScreen.kt` — back (Phase F expands).
- `ui/settings/SettingsAboutScreen.kt` — back + title.

**Settings card / row primitives:**
- `ui/settings/catalog/SettingsCard.kt` — `Card` wrapper. **B.2 target.**
- `ui/settings/catalog/SettingsRow.kt` — row primitive. **D.4 target
  (auto-accent injection point).**

**Custom Surface callers:**
- `ui/photos/grid/StickyHeaderBanner.kt` — translucent banner over the
  grid (already pulls a tinted `surface`, audit semantics in B.1).
- `ui/photos/grid/YearScrubber.kt` — drag-bubble over the right edge
  (year label).
- `ui/collections/SmartAlbumChipRow.kt` — rounded chip + a manual
  `color = MaterialTheme.colorScheme.surface` callsite (line 116).

**Dialogs / sheets:**
- `ui/sort/SortSheet.kt` — `ModalBottomSheet` (defaults flow through).
- `ui/collections/ReorderListDialog.kt` — `AlertDialog` (defaults flow
  through).
- `ui/settings/SettingsAboutScreen.kt` — `AlertDialog` for the MIT
  license body.

**Bottom navigation:**
- `ui/nav/ShutterboyApp.kt` — `NavigationBar` + 3 `NavigationBarItem`
  (Photos / Collections / Settings).

**Empty states + tiles (visual only, no card chrome):**
- `ui/photos/grid/EmptyPhotosState.kt`,
  `ui/collections/EmptyFolderState.kt`,
  `ui/collections/CollectionsScreen.kt::EmptyFoldersBlock`.
- `ui/collections/FolderTile.kt`, `ui/photos/grid/CoverTile.kt`,
  `ui/photos/grid/MonthYearBand.kt`.

## Category-accent design — five hues for shutterboy

Settings sub-pages (Phase I.1) split into five categories. Pick one
accent pair per category, in dark + light variants. The same accents
also drive any non-Settings surface that wants per-category colour
(e.g. an empty-state badge).

| Category       | Hue (HSL hint)             | Phase I row  | Filled icon                   |
|----------------|----------------------------|--------------|-------------------------------|
| Look and Feel  | sky blue (210° / 80 / 65)  | `settings_lookfeel_*` | `Icons.Filled.Palette` |
| Library        | purple (270° / 70 / 60)    | `settings_library_*`  | `Icons.Filled.PhotoLibrary` |
| Photos         | pink (340° / 80 / 65)      | `settings_photos_*`   | `Icons.Filled.PhotoCamera` |
| Albums         | orange (25° / 90 / 60)     | `settings_albums_*`   | `Icons.Filled.Folder` |
| About          | green (140° / 60 / 55)     | `settings_about_*`    | `Icons.Filled.Info` |

Light variants: same hue, lower-saturation container, darker
`onContainer`. Dark variants: brighter container against
`surfaceContainerHigh`. Aim for a delta-E vs background ≥ 25 so the
avatar reads on AMOLED.

Out of scope: deriving accents from individual photo / folder cover
art (would need per-row palette extraction). Revisit if a "tinted
folder card" experiment lands in main.md Phase E.x or later.

## Phase A — dependency + theme entry

**Why:** every later phase compiles against the expressive APIs.

- [ ] **A.1** Override Compose `material3` artifact in
  `gradle/libs.versions.toml`. Add `composeMaterial3 = "1.5.0-alpha18"`
  and pin `androidx-compose-material3 = { ..., version.ref = "composeMaterial3" }`.
  **Removes BOM-based resolution for that one artifact only;** other
  Compose modules keep flowing through the BOM.
- [ ] **A.2** Verify `androidx.compose.material:material-icons-extended`
  is still pinned with an explicit version (no longer transitive in
  `material3:1.4.0+`). It already is — confirm.
- [ ] **A.3** Add `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)`
  to `theme/Theme.kt` so consumers don't carry the annotation.
- [ ] **A.4** Replace `MaterialTheme(colorScheme, typography)` in
  `ShutterboyTheme` with `MaterialExpressiveTheme(colorScheme,
  shapes, typography, motionScheme = MotionScheme.expressive())`.
  Wire `MaterialTheme.shapes` from a small `Shapes(...)` block (see
  D.1).
- [ ] **A.5** Switch the *static-fallback* `lightColorScheme(...)` call
  in `resolveColorScheme` to `expressiveLightColorScheme(...)`. Dark
  stays on `darkColorScheme(...)` (alpha18 doesn't ship the dark
  factory — see Finding 2). Static-fallback only — the dynamic-color
  branches keep using `dynamicDark/LightColorScheme(context)`.
- [ ] **A.6** Run `JAVA_HOME=… ./gradlew :app:assembleDebug` — confirm
  it still builds. Run `./gradlew :app:licenseeAndroidDebug` if the
  Licensee plugin has landed (oss-licenses Phase A); otherwise skip
  this sub-step until that plan ships.
- [ ] **A.7** Ship + tick.

## Phase B — surface-tier discipline

**Why:** Phase A unlocks the M3E ladder; Phase B actually uses it.

- [ ] **B.1** Audit `colorScheme.surface` / `colorScheme.background`
  call sites under `ui/`. Inventory snapshot (above) is the starting
  list. Page-level callers stay on `surface`; card-level callers
  move to `surfaceContainer` (light) / `surfaceContainerHigh` (dark).
  Specifically:
    - `SmartAlbumChipRow.kt:116` (`color = MaterialTheme.colorScheme.surface`)
      → likely `surfaceContainer` (it's the chip bg).
    - `StickyHeaderBanner.kt`'s `Surface` translucent overlay → keep
      on `surface` semantics but verify against the photo grid.
    - `YearScrubber.kt`'s drag-bubble `Surface` → bubble is a card —
      `surfaceContainer`.
    - `SettingsCard.kt`'s `Card` → `containerColor = surfaceContainer`
      (light) / `surfaceContainerHigh` (dark) + `defaultElevation =
      0.dp` + `RoundedCornerShape(28.dp)`.
- [ ] **B.2** Update `theme/Theme.kt`'s static-fallback
  `darkColorScheme(...)` block: stop collapsing
  `surface = ShutterCharcoal` AND `background = ShutterCharcoal`
  onto the same colour — leave `background` undefined (M3E derives
  it from `surface` + the ladder), or pick a slightly darker base so
  `surfaceContainer*` reads as lifted. Same audit for the light
  scheme.
- [ ] **B.3** Confirm `Card` defaults across the app set `defaultElevation
  = 0.dp` + `RoundedCornerShape(28.dp)`. Currently only `SettingsCard`
  uses `Card`; verify it.
- [ ] **B.4** Add
  `app/src/test/java/com/eight87/shutterboy/theme/SurfaceTierContractTest.kt`
  asserting that the static-fallback dark colour scheme's `surface`
  and `surfaceContainer` resolve to two distinguishable RGB values
  (delta-E ≥ 5). Cheap regression guard. Run under Robolectric so
  no AVD is required.
- [ ] **B.5** **Fix the double-inset bug per Finding 8.** In
  `ui/nav/ShutterboyApp.kt`, set the outer `Scaffold(...)` call's
  `contentWindowInsets = WindowInsets(0)` so inner-screen
  `TopAppBar`s own the status bar inset themselves. Pre-fix evidence:
  install + screencap each chrome screen, measure the empty band
  between the status bar bottom and the first content row.
  Post-fix: same screencap, single inset, title sits tight under the
  status bar. **Do NOT reach for `expandedHeight` overrides as a
  workaround** (see Finding 9).
- [ ] **B.6** AVD smoke: install + boot + screenshot Photos, Collections,
  Settings-About; confirm the `SettingsCard` + chip row + scrubber
  bubble visibly lift against the page surface AND the chrome under
  the status bar reads with a single inset.
- [ ] **B.7** Ship + tick.

## Phase C — typography + shapes audit

**Why:** the M3E motion / typography / shape defaults flow through
`MaterialExpressiveTheme`; verify nothing is overriding them with
baseline-M3 tokens.

- [ ] **C.1** Audit the existing `Typography` value used in
  `ShutterboyTheme`. If it's still the Phase 0 default
  `androidx.compose.material3.Typography()`, leave it on the
  defaults — `MaterialExpressiveTheme` overrides them with the
  expressive scale automatically.
- [ ] **C.2** Add an explicit `Shapes(...)` to the theme with
  `extraLarge = RoundedCornerShape(28.dp)` so the M3E group-card
  shape is honoured. Wire into the `MaterialExpressiveTheme(...)`
  call from A.4.
- [ ] **C.3** SettingsRow inner padding = `padding(horizontal = 16.dp,
  vertical = 12.dp)` (system metric). Verify against the current
  `SettingsRow.kt` and patch if it drifts.
- [ ] **C.4** Within-`SettingsCard` row stacking: `Column(verticalArrangement
  = Arrangement.spacedBy(2.dp))`. **Drop** any `HorizontalDivider`
  between settings rows — M3E goes divider-less. Currently
  `SettingsCard` doesn't add dividers between children, but verify
  before Phase I lands and starts adding rows.
- [ ] **C.5** Settings row title style:
  `MaterialTheme.typography.titleMedium`. Subtitle:
  `bodyMedium` + `color = onSurfaceVariant`. Audit `SettingsRow`
  current usage; patch if drifted.
- [ ] **C.6** Ship + tick.

## Phase D — `CategoryAccent` + per-row avatars

**Why:** the actual coloured-circle avatar wiring. Lands ahead of
Phase I (Settings) so the catalog rows pick up avatars from day one.

- [ ] **D.1** Add `theme/CategoryAccent.kt`:
  ```kotlin
  data class CategoryAccent(val container: Color, val onContainer: Color)
  ```
  Co-locate with the theme.
- [ ] **D.2** Define five accent pairs per the table above, dark +
  light variants. Document the "no dynamic-color override" decision
  inline.
- [ ] **D.3** Add `theme/AccentFor.kt`:
  ```kotlin
  internal fun accentFor(id: String, isDark: Boolean): CategoryAccent
  ```
  Maps a settings-row id (e.g. `settings_library_manage_sources`,
  `settings_about_github`) to its category accent by prefix match
  (`settings_library_*` → Library, `settings_about_*` → About,
  etc.). Falls back to a neutral `onSurface`-pair when the prefix
  doesn't match — non-Settings callers (a hand-rolled empty-state
  badge) can still render through the same primitive without a
  category.
- [ ] **D.4** Add `ui/settings/catalog/CategoryAvatar.kt`:
  ```kotlin
  @Composable
  fun CategoryAvatar(
      icon: ImageVector,
      accent: CategoryAccent,
      contentDescription: String?,
      modifier: Modifier = Modifier,
  )
  ```
  `Box(Modifier.size(40.dp).clip(CircleShape).background(accent.container))`
  + centred 24-dp filled icon, tint `accent.onContainer`.
- [ ] **D.5** Add the auto-accent fallback to `SettingsRow.kt` per
  Finding 4: `accent: CategoryAccent? = null` and `id: String? = null`
  parameters; resolve internally as `accent ?: id?.let { accentFor(it,
  isSystemInDarkTheme()) }`. Direct callers stay one-arg; the catalog
  binding pipeline gets the avatar for free.
- [ ] **D.6** Sweep `Icons.Outlined.*` → `Icons.Filled.*` at the
  settings-row leading-icon layer only (about-page rows + future I.2 /
  I.3 / I.4 / I.5 / I.6 catalog rows). Do **not** touch icons inside
  the sort sheet, the reorder dialog, the photo viewer chrome, the
  EXIF panel, or the bottom-nav glyphs.
- [ ] **D.7** AVD smoke: open About; confirm row icons render as
  coloured filled circles, not transparent outlined glyphs.
- [ ] **D.8** Ship + tick.

## Phase E — sweep the rest of the chrome

**Why:** Phases A–D get the theme + Settings right. Phase E carries
M3E into Photos / Collections / Viewer / sheets / dialogs / bottom-nav.
Each sub-step ships its own commit so each surface AVD-smokes
independently.

- [ ] **E.1** **TopAppBar consistency.** All six top app bars use the
  same `TopAppBar(...)` defaults. Verify after A–D that they pull
  `containerColor = MaterialTheme.colorScheme.surface` (page-level)
  and that the title / icon colours read correctly against the new
  static-fallback palette.
- [ ] **E.2** **NavigationBar.** `ShutterboyApp.kt:46` — confirm
  `containerColor` defaults flow to the M3E surface tier. The selected
  item indicator picks up `secondaryContainer` from the expressive
  palette automatically.
- [ ] **E.3** **Photos grid.**
    - `MonthYearBand.kt` — verify it reads on the new surface tier.
    - `StickyHeaderBanner.kt` — translucent banner; B.1 audit handles
      semantics, E.3 just AVD-confirms.
    - `YearScrubber.kt` — drag-bubble `Surface` already moved to
      `surfaceContainer` in B.1; smoke-confirm the right-edge strip
      reads against the photo grid below.
    - `EmptyPhotosState.kt` — typography + spacing pass.
- [ ] **E.4** **Collections root.**
    - `SmartAlbumChipRow.kt` — chip background to `surfaceContainer`
      (B.1); the trailing "+ Manage" chip stays consistent.
    - `FolderTile.kt` — verify the cover photo + label legibility
      against the new page surface.
    - `EmptyFolderState.kt` + `CollectionsScreen::EmptyFoldersBlock` —
      typography + spacing pass.
- [ ] **E.5** **Sheets + dialogs.**
    - `SortSheet.kt` (`ModalBottomSheet`) — defaults pull from M3E
      tokens; confirm the radio + segmented + Apply chrome reads
      against the new container colour.
    - `ReorderListDialog.kt` (`AlertDialog`) — same; the drag-handle
      icon + row label should read against the dialog surface tier.
    - `SettingsAboutScreen.kt`'s license `AlertDialog` — same.
- [ ] **E.6** AVD smoke each surface independently. Per CLAUDE.md, UI
  changes are not done on the strength of unit tests + build alone —
  install + screencap + visual confirm for every E.x sub-step.
- [ ] **E.7** Ship + tick (one commit per E.1–E.5 sub-step).

## Phase F — design unshipped phases with M3E baked in

**Why:** the rest of `main.md` (Phases F / G / H / I / J / K) hasn't
landed yet. Locking M3E patterns into the *plan* for those phases
means we ship them M3E-ready instead of paying a second migration
quarter from now.

- [ ] **F.1** **main.md Phase F (Viewer).** Add a sub-step note: top
  bar + bottom action row use M3E surface tokens against the photo
  background; favorite / share / info / delete icons stay
  `Icons.Outlined.*` since they're chrome glyphs, not avatars.
  EXIF info `ModalBottomSheet` inherits expressive defaults.
- [ ] **F.2** **main.md Phase G (Search).** Pill-shaped search field
  uses M3E `extraLarge` shape + `surfaceContainerHigh` container
  (search field is a "lifted" element above the results list). Filter
  chips: `FilterChip` with M3E selected-container token.
- [ ] **F.3** **main.md Phase H (Multi-select).** Selection chrome top
  bar = `surfaceContainerHigh` (it's the lifted-mode bar). Bottom
  action bar = same. Selection count Badge inherits expressive
  container. **Three concrete patterns absorbed from tonearmboy
  `7876789`** (do these in main.md Phase H):
    - **Active-state colour stays pinned, not theme-derived.** The
      selected-tile accent + selection-bar tint resolve to a stable
      `Color(0xFF…)` defined alongside `CategoryAccent`, NOT
      `primaryContainer` — otherwise a user picking an orange primary
      hijacks the selection colour. (Finding 11.)
    - **Tile-selection visual:** 3-dp `primary` border + cover dim to
      `alpha = 0.55` + `Icons.Filled.CheckCircle` badge in the top-left
      corner. Matches the system Photos / Files multi-select pattern.
      (Finding 12.)
    - **`BackHandler` collapses selection, not pop the screen.** The
      back press exits selection mode and stays on the grid; only a
      second back press leaves the screen. (Finding 12.)
- [ ] **F.4** **main.md Phase I (Settings).** Five sub-pages map to the
  five `CategoryAccent` pairs from Phase D. Catalog row binding flows
  through `SettingsRow` with `id` set so the auto-accent picks up.
  Add a sub-step: "Look and Feel sub-page exposes Theme (System /
  Light / Dark) + Dynamic color toggle + AMOLED Black mode + Grid
  density + Thumbnail quality. Dynamic color toggle does NOT override
  the per-category accents — it only re-seeds the M3 surface palette."
- [ ] **F.5** **main.md Phase J (Slideshow).** Full-screen pager has
  no chrome to migrate, but the timer-pause overlay + the scope-picker
  use M3E surface tier.
- [ ] **F.6** **main.md Phase K (Easter egg).** Triple-tap modal Dialog
  defaults flow through M3E.
- [ ] **F.7** Cross-reference: each main.md phase header gets a
  one-liner "see m3-expressive Phase F.x for M3E touchpoints" inline
  comment so the future implementer doesn't have to re-discover the
  pattern.
- [ ] **F.8** Ship the cross-references as a single commit (no behaviour
  change; just plan-file edits).

## Phase G — opt-in cleanup (deferred)

**Why:** when `material3:1.5.0` ships stable, the
`ExperimentalMaterial3ExpressiveApi` opt-in goes away.

- [ ] **G.1** Track upstream `androidx.compose.material3:material3:1.5.0`
  release. Alpha18 already promoted the API; stable is plausibly a
  quarter or two out from Phase A landing.
- [ ] **G.2** When stable lands: remove the version-catalog override
  (let the BOM drive again), delete the file-level
  `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` from
  `theme/Theme.kt`.
- [ ] **G.3** Re-AVD-smoke (no behaviour change expected). Ship + tick.

## Effort + risk

- **Phase A** S (½ day) — version-catalog override + theme entry +
  one annotation.
- **Phase B** M (1 day) — surface-tier audit is mechanical but
  touches every card / Surface site (small inventory: ~6 callers).
- **Phase C** S (½ day) — Shapes block + spacing verification. Defaults
  do most of the work.
- **Phase D** M (1 day) — five accent pairs + two new files +
  `SettingsRow` patch + outline-to-filled icon sweep.
- **Phase E** M (1 day) — five sweep sub-steps, mostly verification
  + tweaks.
- **Phase F** S (½ day) — plan-file edits only, no code.
- **Phase G** XS — drops one version override + one annotation.

Total: **~4 days** focused work for A–F, plus G whenever 1.5.0 ships.

## Risk / unknowns

- **Light mode pass.** Dark mode is the AVD default + the user's
  primary device. Phase D AVD-smokes light + dark both. If
  `expressiveLightColorScheme()` reads off against the chosen accents,
  patch the light-variant `CategoryAccent` values, not the colour
  scheme.
- **Dynamic color users.** A user who keeps the Phase I.2 dynamic-color
  toggle on gets `dynamicDark/LightColorScheme(context)` instead of
  the static-fallback expressive palette. The surface-tier ladder
  still works (the system palette ships the full ladder), but the
  hand-picked `CategoryAccent` values stay independent of the
  wallpaper — confirmed in the Constraints section above. Document
  in the theme file.
- **Order vs main.md Phase I (Settings).** If main.md Phase I lands
  before this plan's Phase D, the settings catalog ships monochrome
  and we re-touch every catalog binding to add the avatar. **Resolve
  by landing this plan's A–D ahead of main.md Phase I.** Phase E /
  F can interleave or follow.

## References

- `tonearmboy/docs/plans/m3-expressive.md` — full upstream plan that
  triggered this one; cites the official docs and the version-roll
  story.
- developer.android.com/develop/ui/compose/designsystems/material3 —
  M3E + Android 16 visual style; surface roles list.
- developer.android.com/jetpack/androidx/releases/compose-material3 —
  release notes (1.4.0 stable Sept 2025; 1.5.0-alpha18 Apr 2026
  promotes the expressive APIs).
- m3.material.io/develop/android/jetpack-compose — M3 Compose landing.
- github.com/android/androidify — Google's M3E reference app.
- 9to5google.com/2025/09/03/android-16-qpr1-pixel/ — confirms Android
  16 QPR1 shipped the M3E redesign.
