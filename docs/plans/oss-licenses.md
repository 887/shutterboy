# shutterboy — open-source licenses plan

## Status: 🟡 PLANNED

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
- [ ] **B.7** AVD smoke — deferred. Agent worktree has no live AVD; UI smoke happens when the parent merges and walks the surface.
- [x] **B.8** Ship + tick.

## Phase C — Tests + audit discipline

**Why:** keep the inventory honest as deps churn.

- [ ] **C.1** `LicensesCatalogTest` (Robolectric, JVM-only): parses `assets/licenses/artifacts.json`; asserts non-empty; asserts every entry has a recognized SPDX from the allowlist and a backing license-text asset; asserts the catalog contains a known shipping sample (`io.coil-kt.coil3:coil-compose`, `androidx.exifinterface:exifinterface`, `androidx.room:room-runtime`).
- [ ] **C.2** `LicensesScreenTest` (Compose UI test under Robolectric, `ui-test-junit4`): renders, scrolls, expanding a row reveals license text.
- [ ] **C.3** AVD smoke per CLAUDE.md.
- [ ] **C.4** Add a one-paragraph "Licenses" subhead to `CLAUDE.md`: when adding a new `implementation` dep, run `:app:licenseeReport` and confirm the SPDX is in the allowlist; if not, either add it to `licensee.allow(...)` (preferred) or document the exemption with a `because("...")`. Include a pointer to this plan.
- [ ] **C.5** Cross-link from main.md I.6 to this plan when I.6 ships, and tick the I.6 "Open-source acknowledgments" sub-step.
- [ ] **C.6** Ship + tick.

## Out of scope (revisit if pain emerges)

- Failing the build on a disallowed SPDX (`failOnDisallowed = true`) — defer until the v1 inventory has been reviewed once.
- A separate "Acknowledgements" screen for non-binary attributions (icon sets, font samples). The current dep tree has none; revisit if one lands.
- Multi-locale license text. SPDX bodies are English-only by upstream convention.
- A DEPENDENCIES.md / NOTICE file in repo root duplicating the runtime list.
