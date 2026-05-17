# cold-start-perf — keep shutterboy fast

> Reference: [Compose performance best practices](https://developer.android.com/develop/ui/compose/performance/bestpractices)
>
> Sibling plan: [`tonearmboy/docs/plans/cold-start-perf.md`](https://github.com/887/tonearmboy/blob/main/docs/plans/cold-start-perf.md)

This plan is a **maintenance contract**, not a punch list of work
to do. Shutterboy currently cold-starts at ~1.1 s on the AVD —
roughly the realistic Compose floor. The plan exists so we don't
slowly regress it as more features land. Tonearmboy spent a session
clawing back ~400 ms after letting MainActivity grow to 220 lines;
this plan exists so that doesn't happen here.

## What kept shutterboy fast (lessons from tonearmboy's session)

The single biggest variable is **what happens synchronously between
`super.onCreate()` and `setContent { ... }` returning** — and what
the body of `setContent { }` allocates / subscribes / measures on
the first composition.

Shutterboy's MainActivity (25 LOC, end of session) does effectively
nothing on the critical path:
```kotlin
installSplashScreen()
super.onCreate(savedInstanceState)
enableEdgeToEdge()
val graph = (application as ShutterboyApplication).graph
setContent { ShutterboyTheme { ShutterboyApp(graph = graph) } }
```

Tonearmboy at the start of the perf session was 220 LOC with:
- A custom splash hold (`splash.setKeepOnScreenCondition { ... }`) gating dismissal on a Media3 controller handshake — kept the splash up *longer*, not shorter
- Three `applicationScope.launch { ... }` chains in onCreate (watcher service re-arm, first-launch initialise, controller connect kick) all running synchronously on the critical path
- Six `collectAsState` (not `collectAsStateWithLifecycle`) calls inline in `setContent` — each starts collecting during composition, each first emission triggers a root recomposition cascade
- Nine `animateColorAsState(tween(300))` calls in the theme to crossfade chrome between album palettes — allocates 9 `State<Color>` + 9 `LaunchedEffect`s on every cold start, even when no album is playing
- A `BoxWithConstraints` (which is a `SubcomposeLayout`) at the top of the app composable, just to read screen height for sheet drag math
- The full NowPlaying surface mounted unconditionally inside the sheet stack even when sheet was collapsed

After stripping all of these, tonearmboy matches shutterboy to
within ~20 ms on the same AVD. The lessons are below as guards.

## Phase A — keep MainActivity ≤30 lines — standing rule

- [ ] **A.1** **Don't add a splash-hold mechanism.** The default `installSplashScreen()` flow is fine — splash dismisses on first activity frame. A `setKeepOnScreenCondition { ... }` that gates dismissal on async work *extends* the splash visibility, which feels like a hang to the user.
- [ ] **A.2** **Don't run application work in `onCreate`.** Things like `applicationScope.launch { firstLaunchInitialise() }` belong in a `LaunchedEffect(Unit) { }` inside `setContent` — they execute post-first-frame, off the critical path.
- [ ] **A.3** **Don't open the Room database in `onCreate`.** Lazy from the first repository call instead.
- [ ] **A.4** When tempted to put intent / deeplink handling here, keep `handleIntent(...)` minimal — read the extra, set a Compose state, **clear the extra back into the intent via `setIntent(intent)` so a later activity recreation doesn't re-fire the same deeplink**.

## Phase B — keep `setContent { }` shallow — standing rule

- [ ] **B.1** **Use `collectAsStateWithLifecycle`, not `collectAsState`.** The `WithLifecycle` variant defers Flow collection to `Lifecycle.STARTED`, which means DataStore reads (and the recomposition cascades their first emissions trigger) don't happen during the first composition pass.
- [ ] **B.2** **Don't allocate per-color `animateColorAsState` chains in the theme.** Pure function calls (e.g. `blendSurface(base, tint)`) recompose the theme on input change without per-color State allocation. The animation is only worth it if the user asks for it; default to direct values.
- [ ] **B.3** **Don't use `BoxWithConstraints` to read screen size.** It's a `SubcomposeLayout`, forcing an extra composition pass for the children after parent measure. Use `LocalConfiguration.current.screenHeightDp.dp` if you need screen height ahead of layout.

## Phase C — phase-aware modifiers — standing rule

From the [best-practices page](https://developer.android.com/develop/ui/compose/performance/bestpractices):
*"By switching to the lambda version of the modifier, you can make
sure the function reads the scroll state in the layout phase. As a
result, when the scroll state changes, Compose can skip the
composition phase entirely."*

- [ ] **C.1** Prefer `Modifier.offset { IntOffset(x, y) }` over `Modifier.offset(x.dp, y.dp)` when `x` / `y` are derived from animated state — the lambda form runs in the layout phase, the value form runs in composition.
- [ ] **C.2** Prefer `Modifier.graphicsLayer { alpha = ... }` over `Modifier.alpha(...)` when alpha is derived from animated state. Same reason — defers the State read to draw, skips the composition entirely.
- [ ] **C.3** Same for `Modifier.padding { ... }` (where supported) and `Modifier.drawBehind { ... }`.

## Phase D — minimise composable bodies — standing rule

*"Composable functions can run very frequently... you should do as
little calculation in the body of your composable as you can. ... If
possible, it's best to move calculations outside of the composable
altogether."*

- [ ] **D.1** Cache derivations in `remember(keys) { ... }` blocks. Not in the body of the composable.
- [ ] **D.2** Pure derivations that don't depend on Compose state belong outside composables entirely (top-level functions).

## Phase E — lazy layout keys — standing rule

*"By default, each item's state is keyed against its position in the
list. ... If your list updates frequently, this might be problematic."*
The shutterboy timeline grid (`ui/photos/grid/PhotosGrid.kt`) and the
year-scrubber both already pass `key = { ... }` arguments. **Keep it
that way as new lazy lists land.**

- [ ] **E.1** Every `items(...)` / `items(count = ...)` call passes a `key = { ... }` argument that returns a stable identity (typically the entity id, NOT the index).
- [ ] **E.2** When using `LazyColumn` / `LazyVerticalGrid` for a heterogeneous content type, consider `contentType = { ... }` too — Compose's slot table reuse benefits.

## Phase F — Baseline Profile — scaffold shipped (generation device-bound)

Baseline Profile typically shaves 25–35% off cold start on its own.
Worth more than every phase above combined.

Scaffold shipped in main.md L.5 worktree. Versions pinned to
`1.5.0-alpha06` (baselineprofile + benchmark) because AGP 9.0.1
rejects the 1.3.x / 1.4.x stable branches with `Module ':app' is not
a supported android module` — the alpha train adds AGP-9 compat.
ProfileInstaller pinned at `1.4.1` (latest stable; `1.4.2` does not
exist on Maven). Bump both back to stable when an AGP-9-compatible
1.5.x stable lands.

- [x] **F.1** Add the `androidx.baselineprofile` Gradle plugin + a sibling `baselineprofile` benchmark module. Module configures cleanly (`./gradlew :baselineprofile:tasks` lists the macrobench + Baseline Profile task graph; `:baselineprofile:assembleNonMinifiedRelease --dry-run` passes).
- [x] **F.2** Record the cold-boot path (`launch → photos timeline visible`) via `MacrobenchmarkRule` and generate `app/src/main/baseline-prof.txt`. Generator is `BaselineProfileGenerator.kt` — anchors on `By.text("Photos")` (Phase 0.8 `photos_tab_title` string) with a 5 s timeout. Actual profile recording is **device-bound** (needs a non-debuggable benchmark APK on a physical device or `aosp_*` AVD; `emulator-5556`'s `google_apis_playstore` image blocks macrobench), deferred until a suitable device is connected. App-side `baselineProfile { mergeIntoMain = true }` is wired so the next run produces `app/src/main/baseline-prof.txt` directly.
- [x] **F.3** Add `androidx.profileinstaller:profileinstaller` to the app module. Dependency added; no-op until a real profile is recorded.
- [x] **F.4** Wire profile generation into `scripts/build-release-apk.sh`. New `--with-baseline-profile` flag runs `:app:generateBaselineProfile` before assembling. Off by default (slow + device-bound); opt-in per release.

## Phase G — measure, don't guess

- [x] **G.1** Latest measurement (post m3-expressive + viewer + search + oss-licenses) on `emulator-5556`: TotalTime samples 1015 / 1051 / 1056 / 1063 / 1264 ms → median **1056 ms**, well under 1300 ms. Re-measure before each release tag.
- [ ] **G.2** When MainActivity LOC creeps above 50 lines, **revisit this plan before merging**. That's almost always a sign that work has migrated to the critical path.

## Status

This is a **standing-rule plan** — no terminal "DONE". Phase F is the only delta-of-work piece; everything else is preventive. The cost of *not* maintaining it is paying tonearmboy's perf-clawback session over again.
