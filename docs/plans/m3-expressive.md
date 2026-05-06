# shutterboy — Material 3 Expressive (M3E) starter plan

## Status: PLANNED — but read the tonearmboy findings before starting

## Findings shipped in tonearmboy (apply here too)

tonearmboy ran ahead and shipped Phases A–C; capture the gotchas
before shutterboy repeats the same mistakes. Source-of-truth plan:
`tonearmboy/docs/plans/m3-expressive.md`. Commits: `54eaa85`
(opt-in + initial splash), `4bae805` (Phase B+C + first splash
shrink), `2bb042e` (auto-accent + retune splash to 60 %).

1. **`material3:1.4.0` keeps `MaterialExpressiveTheme` /
   `expressive*ColorScheme` `internal`.** The Compose BOM
   `2026.03.01` resolves there but you cannot call the expressive
   APIs from 1.4.0 stable — Kotlin metadata marks them `internal`
   even though the JVM bytecode is public. Override in
   `gradle/libs.versions.toml` with `composeMaterial3 = "1.5.0-alpha18"`
   (the alpha that promoted them). Note: `expressiveDarkColorScheme()`
   does NOT exist in 1.5.0-alpha18 — only the light factory ships;
   dark stays on `darkColorScheme(...)` and inherits the surface
   ladder.
2. **`surfaceContainer` is too quiet on AMOLED-leaning dark
   palettes.** Use `surfaceContainerHigh` for the card container
   colour. Light mode at `surfaceContainer` is fine; revisit if/when
   shutterboy gets a light-mode polish pass.
3. **Auto-derive accent from row `id` at the row composable.** Don't
   pass `accent = ...` from every catalog binding + screen — the
   moment a hand-rolled screen (About / Licenses) bypasses the
   binding system, it goes monochrome. Default `accent` to null at
   the row signature and resolve `accent ?: id?.let { accentFor(it) }`
   inside the body. Every caller wins for free.
4. **Android 12+ splash icon is hard circle-clipped, period.** The
   layer-list `android:windowBackground` workaround does NOT work —
   `windowSplashScreenBackground` paints over the whole window.
   Working approach: ship a dedicated `mipmap-<d>/ic_launcher_splash.png`
   per density, where the design is shrunk to **60 %** with
   transparent padding back to the source canvas size. tonearmboy's
   first attempt at 70.7 % left corners grazing the mask; 60 % gives
   proper headroom. Wire as
   `windowSplashScreenAnimatedIcon = @mipmap/ic_launcher_splash` +
   `windowSplashScreenIconBackgroundColor = @color/launcher_background`
   (the bigger 240-dp icon area).
5. **Album-art tint blending in the theme** (if shutterboy adopts
   tonearmboy's art-driven tint pattern): blend `surface` /
   `surfaceVariant` / `background` AND the `surfaceContainer*`
   ladder, otherwise tint will drift between page and cards.
6. **Auto-derive accent already covers `SettingsRow` callers.**
   Custom Compose surfaces (e.g. a Photos-tab grid header)
   that want the same coloured accent should call `accentFor(id)`
   directly — keep the function exported.

## Scope: "all the views"

User feedback after tonearmboy shipped Settings + About:
> "we want all the views updated/modernized"

Phase D below sweeps the WHOLE app, not just Settings. The
auto-accent fix takes care of any settings-shaped screen for free;
the rest (Photos tab, Albums tab, Viewer, sheets, dialogs) needs
explicit work — surface-tier discipline + `MaterialExpressiveTheme`
opt-in cascade through the same way.

## Why this exists

The user's sister project `tonearmboy` did the leg-work: a side-by-side
of the Android 16 system Settings UI vs the app's own settings showed
the system-side has been redesigned around **Material 3 Expressive
(M3E)** — vibrant per-row coloured circular icon avatars, a clearly
elevated card surface tier vs the page background, divider-less stacks,
the new motion / shape / typography defaults. tonearmboy was on
baseline M3 and reading flat / monochrome by comparison.

**Same architectural shape applies here.** Shutterboy was scaffolded
from the same toolchain and design conventions; the moment the
Settings / Photos / Albums / Viewer chrome lands on a screen-shaped
list of cards, you'll get the same flatness from the same root cause
unless we adopt M3E up front.

This is a **starter plan**: it captures the M3E patterns to follow.
It does not prescribe specific shutterboy UI changes — the actual
sweep should follow whatever screens shutterboy ends up shipping.

## The four M3E patterns to follow

(All four together produce the "happy" Settings look. Each is cheap
in isolation; the combination matters.)

1. **Surface tier ladder.** Page background reads from
   `MaterialTheme.colorScheme.surface`; grouped cards / row clusters
   read from `MaterialTheme.colorScheme.surfaceContainer` (or
   `surfaceContainerHigh` if AMOLED dark needs more separation). Do
   NOT use `surface` for both. Don't lean on shadow elevation —
   `defaultElevation = 0.dp` with the surface-tier token does the
   lift.
2. **`MaterialExpressiveTheme`** in place of plain `MaterialTheme` at
   the app theme entry. Pulls in expressive motion / typography /
   shapes. Requires
   `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` until
   `material3:1.5.0` ships stable.
3. **Coloured circular row-icon avatars.** A `Box` with
   `Modifier.size(40.dp).clip(CircleShape).background(accent.container)`
   wrapping a `24.dp` filled `Icon(tint = accent.onContainer)`.
   `accent` is per-category and comes from a small `data class
   CategoryAccent(val container: Color, val onContainer: Color)`
   defined alongside the theme — M3 only ships three container
   pairs (`primary` / `secondary` / `tertiary`) and the system
   Settings spreads ~6 hues. Hand-pick.
4. **Filled glyph icons** at the avatar layer (`Icons.Filled.*`,
   not `Icons.Outlined.*`). Outlined glyphs read weak inside a
   coloured circle — they're meant for transparent-background row
   leads, not avatars.

Two supporting niceties:

- Card shape: `RoundedCornerShape(28.dp)` or
  `MaterialTheme.shapes.extraLarge` — matches the M3E "extra-large"
  default the system Settings uses for groups.
- Divider-less list rows inside a card: `Column(verticalArrangement
  = Arrangement.spacedBy(2.dp))`. The surface-tier gap between
  cards does the visual separation; in-card rows just stack.

## Phase A — dependency + theme entry

- [ ] **A.1** Bump `androidx.compose.material3:material3` to `1.4.0`
  (or later). Verify Licensee allowlist still passes — see
  `CLAUDE.md` license workflow.
- [ ] **A.2** Add explicit
  `androidx.compose.material:material-icons-extended` dep — no
  longer transitive in `material3:1.4.0+`.
- [ ] **A.3** At the app theme entry (whatever the equivalent of
  `TonearmboyTheme.kt` ends up being here), wrap the root in
  `MaterialExpressiveTheme(...)` instead of `MaterialTheme(...)`.
  Add the `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` once
  at the file level so consumers don't need it.
- [ ] **A.4** Switch `darkColorScheme()` /  `lightColorScheme()`
  calls to `expressiveDarkColorScheme()` / `expressiveLightColorScheme()`.

## Phase B — surface-tier discipline

- [ ] **B.1** Audit every `colorScheme.surface` /
  `colorScheme.background` call site. Page-level callers stay on
  `surface`; card-level callers move to `surfaceContainer`.
- [ ] **B.2** Audit every `Card` / `Surface` / equivalent grouped
  list-row container. Each must use `containerColor =
  surfaceContainer`, `defaultElevation = 0.dp`,
  `RoundedCornerShape(28.dp)`.
- [ ] **B.3** Add a unit test asserting
  `darkColorScheme.surface != darkColorScheme.surfaceContainer` —
  cheap regression guard.

## Phase C — `CategoryAccent` + per-row avatars

- [ ] **C.1** Add `data class CategoryAccent(val container: Color,
  val onContainer: Color)` next to the theme.
- [ ] **C.2** Define ~5–6 hand-picked accent pairs (sky blue /
  purple / pink / magenta / orange / green or similar) for each
  surface that needs them. shutterboy's split is its own — pick
  natural categories from whatever the settings catalog ends up
  shaped like (e.g. Appearance / Library sources / Viewer behaviour
  / Sharing / About). Give each light + dark variants.
- [ ] **C.3** New composable
  `SettingsCategoryIcon(icon: ImageVector, accent: CategoryAccent,
  contentDescription: String?)` per the pattern in §3 above.
- [ ] **C.4** Wire it into the settings catalog row renderer.

## Phase D — the rest of the chrome

- [ ] **D.1** Photos / Albums grid headers, FAB, top app bar — pull
  through `surfaceContainer`-on-`surface` consistently.
- [ ] **D.2** Viewer top + bottom action bars — verify they respect
  the new container surface against the photo background.
- [ ] **D.3** Bottom sheets (share, sort, filter) — `ModalBottomSheet`
  defaults already pull from M3 tokens; confirm the new expressive
  containers flow through.

## References

- `tonearmboy/docs/plans/m3-expressive.md` — the full plan that
  triggered this one; cites all the upstream docs and the
  `material3` artifact version-roll story.
- developer.android.com/develop/ui/compose/designsystems/material3
- developer.android.com/jetpack/androidx/releases/compose-material3
- m3.material.io/develop/android/jetpack-compose
- github.com/android/androidify — Google's M3E reference app.

## Risk / unknowns

- **Wallpaper-driven Material You vs hand-picked accents:** the
  per-category `CategoryAccent` plan is intentionally NOT driven
  off `dynamicDarkColorScheme(LocalContext.current)` — letting the
  user's wallpaper palette override the per-category intent defeats
  the colour-coding. Document the decision in the theme file.
- **Light mode pass:** verify after Phase C that
  `expressiveLightColorScheme()` + the same accent class reads OK
  against the lighter background.
- **Scope creep:** keep `Icons.Outlined.*` → `Icons.Filled.*` swap
  at the settings-row leading-icon layer only. Don't churn icons
  inside the photo viewer / share sheet / EXIF panel.
