# shutterboy — open-source licenses plan

## Status: ✅ DONE — Phases A + B + C shipped. A + B in commit `bda7da5` (merged to main via `0263cbb`); C.1 / C.4 / C.5 in commit `d9e5482`. Licensee plugin generates the inventory, LicensesScreen renders the catalog, About row links to it, LicensesCatalogTest gates the inventory + asset shape on every JVM test run.

## Why

The app ships under MIT (`LICENSE` in repo root). Every dependency that ships in the APK is **Apache License 2.0** — every `androidx.*` module, the kotlinx ecosystem, Coil 3, `androidx.exifinterface`. Apache 2.0 §4 requires that downstream binary distributions preserve copyright + NOTICE entries from upstream artifacts. The Android-conventional way to satisfy this is an "Open-source licenses" sub-page: a list of every shipping dep with name, version, license SPDX, and license body.

The licenses surface is also user-friendly: people who pick up the app from F-Droid / Obtainium and look at the About screen want to know what's underneath.

`docs/plans/main.md` already schedules an About sub-page in Phase I.6 with an explicit "Open-source acknowledgments" bullet. **This plan is the implementation of that bullet** — it does not redo the About-screen design, it adds the licenses surface and wires it in. The Licensee plugin work (Phase A here) can land *before* I.6 since it produces a generated artifact independent of the About UI.

## Approach (locked)

- **Build-time inventory, zero runtime deps.** Use the [`app.cash.licensee`](https://github.com/cashapp/licensee) Gradle plugin. It walks the resolved `releaseRuntimeClasspath` at configuration time and writes a JSON inventory; nothing is added to the APK at runtime. Licensee is Apache 2.0 itself, build-time only, and is the same pattern adopted in [`tonearmboy/docs/plans/oss-licenses.md`](../../../tonearmboy/docs/plans/oss-licenses.md) — keeping the two sibling apps consistent.
- **Compose-rendered sub-screen.** A new `LicensesScreen.kt` reads the generated `artifacts.json` from `assets/licenses/` and renders a `LazyColumn` of cards. Tapping a row reveals the license body. License bodies (Apache-2.0, MIT, EPL-1.0) ship as raw text assets — finite set, three at most.
- **Robolectric-driven catalog test.** Parses the generated JSON, asserts non-empty, asserts every entry has a known SPDX and a backing license-text asset, asserts a known sample of shipping deps is present.
- **Report-only allowlist in v1.** Licensee can fail the build on disallowed licenses. We declare the allowlist (`Apache-2.0`, `MIT`, `BSD-2-Clause`, `BSD-3-Clause`) but do not enforce in v1.
- **i18n discipline carries over.** Per CLAUDE.md, every user-facing string lands in `app/src/main/res/values/strings.xml` in the same commit that introduces the surface. Keys follow the `<surface>_<role>` snake-case scheme. New surface prefix: `licenses_`.

## Inventory snapshot (informational — confirmed `2026-05-04` against `gradle/libs.versions.toml` + `app/build.gradle.kts`)

Apache 2.0 unless otherwise marked. **Authoritative source is the Licensee-generated `artifacts.json` once Phase A lands.**

**Ships in APK (`implementation`):**
- `androidx.core:core-ktx`, `androidx.core:core-splashscreen`
- `androidx.activity:activity-compose`
- `androidx.lifecycle:lifecycle-{runtime-ktx,runtime-compose,viewmodel-compose,viewmodel-navigation3}`
- `androidx.compose.{ui,material3,material:material-icons-extended,ui-tooling-preview}` via the Compose BOM
- `androidx.navigation3:navigation3-{runtime,ui}`
- `androidx.room:room-{runtime,ktx}`
- `androidx.datastore:datastore-preferences`
- `androidx.exifinterface:exifinterface`
- `org.jetbrains.kotlinx:kotlinx-serialization-json`
- `io.coil-kt.coil3:coil-compose` (transitively pulls okio — Apache 2.0)

**Test-only (`testImplementation` / `androidTestImplementation`, excluded from the APK):**
- `junit:junit:4.13.2` — **EPL-1.0** (Eclipse Public License 1.0). Test-scope only; never shipped.
- `org.robolectric:robolectric` — MIT
- `androidx.test.*`, `androidx.compose.ui.test.*`, `androidx.arch.core:core-testing`, `androidx.room:room-testing`, `org.jetbrains.kotlinx:kotlinx-coroutines-test` — Apache 2.0

**Build-only / KSP / Gradle plugins (never shipped):** AGP, KSP, Kotlin Compose / Serialization plugins, Room Gradle plugin, foojay-resolver — Apache 2.0.

Conclusion: **MIT app license is correct. No GPL anywhere. No dep prevents MIT.**

## Phase A — Licensee plugin + generated inventory — shipped in commit `0abaac5`

**Why:** every later phase reads the JSON this phase generates. Independent of the About-screen UI; can land before main.md Phase I.

- [x] **A.1** Add Licensee version to `gradle/libs.versions.toml` and a `[plugins]` entry: `licensee = { id = "app.cash.licensee", version.ref = "licensee" }`. Pinned to 1.14.1 (latest stable on Maven Central).
- [x] **A.2** Apply `alias(libs.plugins.licensee)` in `app/build.gradle.kts`.
- [x] **A.3** Configure the plugin block: `licensee { allow("Apache-2.0"); allow("MIT"); allow("BSD-2-Clause"); allow("BSD-3-Clause"); allowDependency("junit", "junit", "4.13.2") { because("EPL-1.0; test-scope only, not shipped") } }`. Reporting only in v1.
- [x] **A.4** Wire a Gradle task (`copyLicenseeInventory`) to copy `app/build/reports/licensee/androidRelease/artifacts.json` to `app/src/main/assets/licenses/artifacts.json`. Hooked as a dependency of `mergeReleaseAssets` and `mergeDebugAssets`. (Licensee's per-flavour report path is `androidRelease/`, not `release/`.)
- [x] **A.5** Added `app/src/main/assets/licenses/Apache-2.0.txt`, `MIT.txt`, `EPL-1.0.txt`, `BSD-2-Clause.txt`, `BSD-3-Clause.txt`. Sourced from `https://spdx.org/licenses/<SPDX>.txt`; top-of-file comment carries the SPDX id + source URL.
- [x] **A.6** `:app:assembleDebug` runs clean; generated `artifacts.json` has 182 entries spanning Apache-2.0 and BSD-3-Clause SPDX ids.
- [x] **A.7** Ship + tick.

## Phase B — `LicensesScreen` Compose UI — shipped in commit `0abaac5`

**Why:** the user-facing surface that fulfils the Apache 2.0 NOTICE requirement and the main.md I.6 "Open-source acknowledgments" bullet.

**Sequencing note:** depends on the M3-Expressive `SettingsCard` / `SettingsRow` chrome introduced earlier in main.md Phase A.5 (already shipped) and on the AboutScreen scaffold from main.md I.6. If I.6 has not yet landed when this plan is worked, scaffold a minimal AboutScreen here (build-version + GitHub link + license link + Licenses entry) and let I.6 expand it later — same shape, smaller initial surface.

- [x] **B.1** Added `app/src/main/java/com/eight87/shutterboy/ui/settings/LicensesScreen.kt`. Reuses `SettingsCard` / `SettingsRow` for visual consistency with the rest of settings.
- [x] **B.2** Added `LicensesViewModel`. Reads `assets/licenses/artifacts.json` once at init via `AssetManager.open(...)` + `kotlinx.serialization.json`. Exposes `StateFlow<List<LicenseEntry>>` (entries pre-sorted by `groupId:artifactId`).
- [x] **B.3** Defined `LicenseEntry { groupId, artifactId, version, spdxId, licenseText: String? }` — `licenseText` resolved at construction by reading `assets/licenses/<spdx>.txt` with a per-SPDX cache. Unknown SPDX → `licenseText = null` and the row's subtitle renders the `licenses_unknown_spdx` warning string.
- [x] **B.4** UI: `LazyColumn` of `SettingsRow`s. Row title `<artifactId> <version>`, supporting text `<groupId> • <spdxId>`. Tap opens a Material3 `AlertDialog` with the license body in monospaced scrollable text.
- [x] **B.5** Wired navigation. Added a new `Licenses` `Destination`, a `Licenses.Register` route, and a new "Open-source licenses" row in `SettingsAboutScreen` between the GitHub row and the existing OSS-acknowledgments placeholder, using `Icons.AutoMirrored.Outlined.Article`. Tap pushes `Licenses` onto the back stack.
- [x] **B.6** Strings landed in `app/src/main/res/values/strings.xml` under the `licenses_` / `cd_licenses_` prefix per the i18n convention.
- [x] **B.7** AVD smoke — walked Photos → Settings tab → About card → Open-source licenses on `emulator-5556` (API 36); LicensesScreen renders the live ~183-entry catalog and the per-row Apache-2.0 dialog opens cleanly. Screencaps under `/tmp/sb-licenses-{0..4}*.png`. (See C.3 ship note.)
- [x] **B.8** Ship + tick.

## Phase C — Tests + audit discipline — shipped in commit `d9e5482` (C.1 / C.4 / C.5 / C.6); C.2 + C.3 closed out in this worktree (see ticks below).

**Why:** keep the inventory honest as deps churn.

- [x] **C.1** `LicensesCatalogTest` (Robolectric, JVM-only): parses `assets/licenses/artifacts.json` via `RuntimeEnvironment.getApplication().assets`, asserts non-empty, asserts every entry's SPDX is in the `{Apache-2.0, MIT, BSD-2-Clause, BSD-3-Clause}` allowlist, asserts a backing license-text asset exists for each SPDX (verified via `assets.list("licenses")`), and asserts the catalog contains `io.coil-kt.coil3:coil-compose`, `androidx.exifinterface:exifinterface`, and `androidx.room:room-runtime`.
- [x] **C.2** `LicensesScreenTest` (Compose UI test under Robolectric, `ui-test-junit4`): mounts `LicensesScreenContent` (split out from `LicensesScreen` for testability — narrow `(entries, onBack)` shape, same as `FavoritesScreenContent` / `PhotoViewerContent`) over a 60-entry synthetic catalog and asserts (a) the title + first row render, (b) `performScrollToNode` reveals a row beyond the first viewport, (c) tapping a row opens an `AlertDialog` whose body contains the Apache-2.0 header. Lives at `app/src/test/java/com/eight87/shutterboy/ui/settings/LicensesScreenTest.kt`.
- [x] **C.3** AVD smoke per CLAUDE.md — Photos → Settings tab (1080×2400 @ ~724,143) → About row (~540,1053) → Open-source licenses row (~540,1280) → tap row → Apache-2.0 dialog. Screencaps `/tmp/sb-licenses-{0-photos,1-settings,2-about,3-list,4-dialog}.png`. The LicensesScreen renders the live Licensee-generated catalog and the dialog scrolls through the full Apache-2.0 body.
- [x] **C.4** `CLAUDE.md` gained an "Open-source licenses" subhead above the Plan-file section: workflow for adding a new `implementation` dep (run `:app:licenseeAndroidRelease`, confirm SPDX in allowlist, prefer `licensee.allow(...)` over `allowDependency(...) { because("...") }`, ship new `assets/licenses/<spdx>.txt` from spdx.org if the SPDX is new), with a pointer to this plan.
- [x] **C.5** main.md I.6 "Open-source acknowledgments" sub-step now cross-links to this plan. The I.6 checkbox itself remains unticked since the About-page row is a Phase I.6 deliverable; this plan only ships the Licenses sub-page it links to.
- [x] **C.6** Ship + tick.

## Out of scope (revisit if pain emerges)

- Failing the build on a disallowed SPDX (`failOnDisallowed = true`) — defer until the v1 inventory has been reviewed once.
- A separate "Acknowledgements" screen for non-binary attributions (icon sets, font samples). The current dep tree has none; revisit if one lands.
- Multi-locale license text. SPDX bodies are English-only by upstream convention.
- A DEPENDENCIES.md / NOTICE file in repo root duplicating the runtime list.
