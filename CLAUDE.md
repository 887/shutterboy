# shutterboy — Claude instructions

Modern Android photo gallery. Kotlin + Jetpack Compose + Coil 3 + Room. Built entirely from the CLI, no Android Studio required, no QEMU emulator. Sibling app to [`tonearmboy`](https://github.com/887/tonearmboy) — same architectural shape, same release pipeline, same Obtainium distribution path.

## Architectural decisions (locked)

- **Language:** Kotlin only. No Java.
- **UI:** Jetpack Compose. No Android Views.
- **Image loading:** [Coil 3](https://coil-kt.github.io/coil/) (Compose-native; backs all thumbnail / fullscreen / shared-element image loads). No Glide, no Picasso.
- **EXIF / metadata:** `androidx.exifinterface` for orientation, capture date, lens, GPS. Read-only in v1; rotation / metadata edits are out of scope.
- **Photo discovery:** `MediaStore.Images.Media` for the device's image collection; SAF (`Intent.ACTION_OPEN_DOCUMENT_TREE`) for additional folders / SD card / USB OTG sources. Same multi-source pattern tonearmboy uses for music sources.
- **Data:** Room for cached MediaStore metadata (rows = thumbnail uri, capture date, dimensions, EXIF subset, album / folder key). FTS for search across filename + EXIF user-tags.
- **i18n discipline (locked from Phase 0):** every user-facing string goes through `stringResource(R.string.…)` or `LocalContext.current.getString(R.string.…)`. No inline `Text("…")` literals on shipped UI. `app/src/main/res/values/strings.xml` is the canonical source of truth; locale variants live at `app/src/main/res/values-<locale>/strings.xml`. Naming scheme: `<surface>_<role>` lowercase snake (`photos_tab_title`, `viewer_share_cd`, `settings_library_rescan_button`). Surfaces: `photos_`, `collections_`, `viewer_`, `search_`, `settings_`, `dialog_`, `error_`, `cd_` (content descriptions). Translation workflow: see the **Translations** section below.
- **Build front-end:** [Google's Android CLI](https://developer.android.com/tools/agents/android-cli) (`android` command, launched April 2026). Wraps project creation, SDK management, build, install, and run. **Do not introduce Android Studio project files** (`.idea/`, `*.iml`).
- **Build back-end:** Gradle (driven by the Android CLI; the wrapper is committed to the repo).
- **Tests, unit:** Robolectric. JVM-only. No device required.
- **Tests, UI:** [mobile-mcp](https://github.com/mobile-next/mobile-mcp), Claude-driven over ADB. **Current target: headless AVD `medium_phone`** (Android 16 / API 36, RSS ~3.2 GB), started without window/audio/snapshot. Phone via wifi-adb is the long-term home once SAF + share-sheet behaviour matters; Waydroid was declined (would need root).

## Required CLIs and MCP servers

These are user-machine prerequisites. The plan tracks each in Phase 0.

### Android CLI

The new (April 2026) `android` command from Google wraps everything we need.

Install (userspace, this user's setup):

```bash
curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/android -o ~/.local/bin/android
chmod +x ~/.local/bin/android
android --version  # self-bootstraps the runtime on first call
```

The CLI bundles its own JDK 21 at `~/.android/cli/bundles/<hash>/jre/`. **Caveat:** the bundled JRE is *minimized* — it's missing modules including `java.rmi`, which Gradle 9.1's Kotlin DSL classpath fingerprinter loads. Direct `./gradlew` invocations against the bundled JRE will fail at configuration time with `java.lang.NoClassDefFoundError: java/rmi/Remote`. For direct Gradle calls, export `JAVA_HOME` to a full system JDK 17+ instead — for example `/usr/lib/jvm/java-26-openjdk` on this user's machine. Going through `android run` / other `android` subcommands is fine and uses the bundled toolchain internally.

Practical rule of thumb:
- `android run --apks=…` → just works.
- `./gradlew assembleDebug` → `JAVA_HOME=/usr/lib/jvm/java-26-openjdk ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug` (or equivalent system JDK 17+ path).

**Worktree caveat:** Gradle reads the SDK path from `local.properties`, which is gitignored. Worktrees created off `main` for subagents start without a `local.properties`, so direct `./gradlew` calls there will fail with `SDK location not found` unless `ANDROID_HOME` is exported (or `local.properties` is generated locally in the worktree). Always export `ANDROID_HOME=$HOME/Android/Sdk` alongside `JAVA_HOME` when invoking Gradle directly in an agent worktree.

Useful subcommands:

```bash
android create list                                     # browse project templates
android create --name=shutterboy --output=. <template>  # scaffold a new project
android sdk install platforms/android-34 build-tools/34.0.0
android run --apks=app/build/outputs/apk/debug/app-debug.apk
android docs search <query>                             # query the Android Knowledge Base
android docs fetch <kb-url>                             # fetch a specific KB doc
android skills list --long                              # browse official Android skills
android info                                            # show detected SDK + version
```

`android docs search` is **the first place to look** when uncertain about Android APIs. It returns up-to-date guidance from the official Android Knowledge Base — beats grepping web search results, and is on-machine.

### `mobile` MCP server (UI driving)

Registered at **project scope** in `.mcp.json` (committed to the repo) and allowed in `.claude/settings.json` (also committed). When a Claude Code session starts in this repo with `enableAllProjectMcpServers: true` (set in the project settings), the `mcp__mobile__*` tools become available automatically.

To re-register on a fresh checkout if for any reason the project config drops the entry:

```bash
claude mcp add mobile --scope project -- npx -y @mobilenext/mobile-mcp@latest
```

What it gives you: list connected ADB targets, install APKs, launch the app, read the accessibility tree (the screen state, the way Playwright reads the DOM), tap by label / coordinates, assert UI state.

### `android-skills` MCP server (official Android skills)

Registered at **project scope** in `.mcp.json` and allowed in `.claude/settings.json`. Surfaces Google's official Android Skills (Compose migration, Navigation 3, Edge-to-Edge, AGP 9, R8 config, Media3 patterns, etc.) as MCP tools inside Claude Code. **Consult these before hand-rolling any Android-specific pattern** that could be load-bearing on platform conventions.

To re-register on a fresh checkout:

```bash
claude mcp add android-skills --scope project -- npx -y android-skills-mcp
```

### Test target

One of:

- **wifi-adb to the user's phone** (preferred — zero machine RAM cost):
  ```bash
  adb pair <ip>:<pair-port>      # pair once
  adb connect <ip>:<connect-port>
  adb devices                    # confirm
  ```
- **Waydroid** on Linux (LXC container, ~1-2 GB resident, shares host kernel):
  ```bash
  waydroid session start
  adb connect 192.168.240.112:5555  # default Waydroid IP
  ```

## Test loop

```bash
./gradlew assembleDebug
android run --apks=app/build/outputs/apk/debug/app-debug.apk
# mobile-mcp tools take over for UI interaction
```

### UI changes are verified on the running AVD

Any change that touches Compose UI (layout, composable structure, navigation, theming, anything visible) MUST be verified by installing the rebuilt debug APK on the running headless AVD (`emulator-5554`) and inspecting the result — Robolectric unit tests do not catch real-device layout bugs (overflow, clipping, off-screen widgets, scroll behaviour under the bottom bar, image-aspect-ratio surprises, etc.).

Canonical loop:

```bash
JAVA_HOME=/usr/lib/jvm/java-26-openjdk ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:assembleDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 shell am start -n com.eight87.shutterboy/.MainActivity
adb -s emulator-5554 exec-out screencap -p | magick - -resize 50% /tmp/shutterboy.png   # then Read the PNG
```

The AVD is 1080x2400 native, which is too big to read comfortably — pipe screencaps through `magick - -resize 50%` to land at 540x1200 (quarter the pixels, easier to inspect, tap coords are still computed against the device's native 1080x2400, just multiply scaled image coords by 2). Skip the resize only when you genuinely need pixel-accurate detail.

Also: clean up `/tmp/*.png` periodically — these accumulate fast across sessions and a few hundred stale screenshots makes file listings noisy.

Prefer `mobile-mcp` tools when they're loaded in the session (they give the accessibility tree + tap-by-label, much more precise than coordinate input). When mobile-mcp isn't available, fall back to `adb exec-out screencap -p` + visual inspection of the PNG via the Read tool — it's lower-resolution evidence than the a11y tree but enough to confirm widget presence, position, and overflow behaviour.

Do not report a UI task as done on the strength of unit tests + a successful build alone.

For raw ADB inspection during dev:

```bash
adb logcat -s shutterboy:* MediaStore:* Coil:*
adb shell am start -n com.eight87.shutterboy/.MainActivity
```

## File conventions

- Single-module to start. Split into `:core` / `:data` / `:ui` only when the single-module size warrants it; do not premature-modularize.
- Package root: `com.eight87.shutterboy`.
- Composable functions: PascalCase, no `@Composable` on private helpers unless they take a Modifier.
- ViewModels: one per screen, talk to the data layer via repository interfaces.
- No DI framework in v1 (Hilt/Koin) — pass dependencies as constructor params. Add DI later if/when the manual wiring hurts.
- No reflection-based JSON. Use `kotlinx.serialization` if any serialization is needed.

## Design principles — SOLID, applied to Kotlin + Compose

The codebase follows SOLID where it earns its keep. Kotlin + Compose change *how* the principles cash out (top-level functions instead of `interface ServiceImpl`, sealed types instead of Visitor, `Flow<T>` instead of Observer wiring), but the underlying tests still apply. **When introducing a new file or refactoring an existing one, sanity-check it against these five questions.** When in doubt, prefer the principle over the shortcut.

- **S — Single Responsibility.** A type / file / composable should have *one reason to change*. If you can describe what a class does without "and", "also", or "plus", you're probably fine. If a single file is editable for three independent reasons (e.g. *grid rendering* + *folder filter persistence* + *navigation routing*), split it. Soft heuristic: anything past ~500 LOC of non-trivial Kotlin deserves a second look; past ~800 LOC almost always needs splitting.
- **O — Open/Closed.** Prefer adding a new sealed-class case / new strategy implementation over modifying an existing `when`/`if` chain that already covers the abstraction. Sealed types + exhaustive `when` are the Kotlin-native way to express "open for extension". *Caveat:* don't pre-build extension points for cases that don't exist yet — closed-by-default, opened only when a second variant arrives.
- **L — Liskov Substitution.** Subtypes (or sealed-type variants) must honour the contract of the parent. A `FilterCondition.HasGps` that throws on `matches()` for a malformed photo would break this — every variant must satisfy `matches: Photo -> Boolean` totally. In Compose, this also means: if a composable promises to render in a `Modifier.fillMaxWidth()` parent, every implementation should.
- **I — Interface Segregation.** Don't pass a fat type when a narrow one would do. If a screen needs only `observePhotos()` and `photosMatching()`, take a `PhotoSource` (two methods) — not the whole `GalleryRepository` (40+ methods). In Compose this often manifests as: don't pass a god-state object down five levels; pass the three fields the leaf actually reads.
- **D — Dependency Inversion.** High-level modules (UI, gallery orchestration) depend on abstractions, not concrete classes. Concretely: ViewModels / composables take repository *interfaces* or function-typed parameters; concrete Room DAOs / SAF wrappers / Coil image loaders live behind those interfaces. The `AppGraph` is the composition root — it's the *only* place that knows the concrete types.

These are evaluation criteria, not religion — small ad-hoc helpers don't need their own interface, and one-off composables don't need to be split for principle's sake. But anything load-bearing (repositories, the gallery scanner, navigation, settings storage) should pass all five.

SOLID refactor + standing-discipline plan: see [`docs/plans/refactor-solid.md`](docs/plans/refactor-solid.md). It tracks the cross-cutting rules every phase enforces, the tonearmboy-audit lessons absorbed pre-emptively into main.md's phase definitions (R.A → R.E), the standing R.F polish backlog, and the per-phase + end-of-major-phase audit cadence. Self-check the cross-cutting rules against the diff before ticking any phase header.

## Open-source licenses

The app ships an open-source acknowledgements sub-page driven by the [`app.cash.licensee`](https://github.com/cashapp/licensee) Gradle plugin (build-time only, nothing added to the APK at runtime). The plugin walks the `releaseRuntimeClasspath` and writes `app/src/main/assets/licenses/artifacts.json`, which `LicensesScreen` renders. Allowlist: `Apache-2.0`, `MIT`, `BSD-2-Clause`, `BSD-3-Clause`. License bodies ship as raw text assets at `app/src/main/assets/licenses/<spdx>.txt` sourced from `https://spdx.org/licenses/<spdx>.txt`.

When adding a new `implementation` dep:

1. Run `JAVA_HOME=/usr/lib/jvm/java-26-openjdk ANDROID_HOME=$HOME/Android/Sdk ./gradlew :app:licenseeAndroidRelease` and confirm the dep's SPDX is in the allowlist above.
2. If it isn't, prefer adding it via `licensee { allow("<SPDX>") }` in `app/build.gradle.kts`; only fall back to `allowDependency(group, artifact, version) { because("…") }` for one-off exemptions (e.g. test-only deps like JUnit's EPL-1.0).
3. If the SPDX is genuinely new, ship the matching `app/src/main/assets/licenses/<spdx>.txt` from spdx.org in the same commit. `LicensesCatalogTest` (Robolectric, JVM-only) will fail loud if the asset is missing or the SPDX falls outside the allowlist.

Full design + phase log: [`docs/plans/oss-licenses.md`](docs/plans/oss-licenses.md).

## Plan file

The phased build plan lives at [`docs/plans/main.md`](docs/plans/main.md), per the user's global CLAUDE.md rule (numbered phases, sub-step checkboxes).

When working on a phase:

- Tick its sub-steps (`- [x]`) in the same commit that lands the work.
- Add `shipped in commit <id>` to the phase header when *all* its sub-steps are ticked.
- Mark the whole plan `## Status: ✅ DONE` once every phase is ticked.
- If a phase header has no sub-step checkboxes, *write them first*. No vibes-based progress.

## Editorial — user-facing copy

The user follows Paul Graham's *Keep Your Identity Small*. App copy (settings descriptions, error messages, About text) should be plain, factual, useful. No "vibes" copy, no personal opinions, no humor that pins identity.

## Translations

**Translations are produced by the user + Claude, per-language, in dedicated sessions.** That's the canonical workflow, not a fallback. Community PRs are accepted if they show up but nothing in the pipeline assumes or requires them — no contributor onboarding doc, no PR template addendum, no welcome-mat infrastructure. (Mirrors the [`tonearmboy` translations plan](https://github.com/887/tonearmboy/blob/main/docs/plans/translations.md) — same constraints, same workflow.)

Per-language session shape:
1. User picks a target locale.
2. Claude reads `app/src/main/res/values/strings.xml` and the editorial brief (plain / factual / useful — no vibes copy).
3. Claude drafts `app/src/main/res/values-<locale>/strings.xml` with every translatable key. Same key order as `values/strings.xml` for diff-friendly review.
4. Per-entry user review. Anything off → user redirects → Claude revises in place.
5. Commit signed-off entries; leave anything unconfirmed missing — English-fallback is correct behaviour, not a placeholder.
6. Run `scripts/translation-progress.sh` to refresh the README progress table.
7. AVD smoke under the new locale: switch device locale (`adb shell setprop persist.sys.locale de-DE && adb shell stop && adb shell start`), walk every screen, watch for layout overflow on long compound words (German specifically — `flowRow` / `wrapContentWidth` may need targeted patches).

Locked constraints:
- **No third-party translation service.** No Crowdin, Lokalise, Weblate (hosted or self-hosted).
- **No new build dependency.** Just Android's built-in `values-<locale>/strings.xml` + a small POSIX shell script for the README progress table.
- **Zero CI minutes.** `scripts/translation-progress.sh` runs locally inside `scripts/build-release-apk.sh` before the `git tag` step.
- **English is canonical.** Locale variants are partial overrides; missing keys fall back to English at runtime.

What this deliberately does NOT include: a `CONTRIBUTING-TRANSLATIONS.md`, a PR template addendum for translation contributions, "we welcome contributions" copy in README, a `<!-- needs-translation -->` placeholder convention, or a reviewer-pair rule. The layout migrates cleanly to Weblate/Crowdin if the user ever opens community translations later — but this CLAUDE.md doesn't pre-build for that path.

## Easter egg + character art

Sibling pattern to tonearmboy's `R.drawable.easter_egg_fox`: triple-tapping the build-version row on the About sub-page reveals a full-screen modal showing `R.drawable.easter_egg_tiger` — an anthro tiger character in a "STRIPE A POSE" hoodie wearing a black collar with brass nametag stamped "TIGER". Source artwork is committed at [`docs/artwork/easter_egg_tiger.png`](docs/artwork/easter_egg_tiger.png) and is moved into `app/src/main/res/drawable-nodpi/easter_egg_tiger.png` during Phase 0 scaffolding. The tiger is the app's mascot the same way the fox is tonearmboy's.

App icon: tiger cutout + a Pentax SLR sitting on the right of the canvas, on the same warm-charcoal / burnt-orange / cream / brass palette as the tonearmboy fox-vinyl icon, so the two read as litter-mate apps on a launcher.

## Release workflow

The user's intended pattern: **vibing from their phone with the Claude app**,
they tell Claude "ship a new build of shutterboy." Claude opens a session against
this repo on the dev machine and runs the local build. The user then pulls the
APK via [Obtainium](https://github.com/ImranR98/Obtainium) on their phone,
which auto-detects the new GitHub Release.

**Local build is the primary path. Zero CI minutes by default.**

Canonical commands:

```bash
# Full one-shot: build + push to GH Releases + install on connected device
scripts/build-release-apk.sh --gh-release --install

# Just publish to GH Releases (Obtainium pulls from there)
scripts/build-release-apk.sh --gh-release

# Local APK only, no upload, no install
scripts/build-release-apk.sh
```

What `--gh-release` does:

1. Builds `release/shutterboy-<version>-<sha7>.apk` (debug-signed by default).
2. Generates release notes from `git log <prev-tag>..HEAD` plus a
   "Verify build" table containing the commit hash and APK SHA-256.
3. Creates the GitHub Release `v<version>-<sha7>` with the APK attached.
4. Pushes the local annotated tag to `origin`.

The `.github/workflows/release.yml` fallback is **tag-only and self-disabling**:
it triggers when a `v*` tag is pushed, then queries the matching release; if
an APK is already attached (which is true after the local script ran), it
exits 0 without rebuilding. Saves CI minutes by default; only runs when a tag
shows up without a matching APK (e.g. tag pushed from the GH web UI).

When a phase asks for a release, the happy path is `--gh-release --install`
against the connected AVD / wifi-adb phone.

## Subagent dispatching

Subagents working on this repo run in worktrees. Each agent prompt must:

- name the phase + sub-steps it owns
- be told to tick checkboxes and add the commit ID to the phase header as it lands work
- be told to keep the work scoped to its phase (no opportunistic refactors of unrelated code)
- be told to never modify `~/.claude/` files (those are not under this repo)
- be told to consult `android docs search <query>` before hitting general web search for Android API questions
- be told to consult the `android-skills` MCP for any pattern Google has codified (Compose migration, Navigation 3, edge-to-edge, etc.)
