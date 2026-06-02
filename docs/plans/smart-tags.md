# Smart Tags — on-device AI photo tagging

On-device image tagging that runs as a **downstream background pass after
each scan** (never inline in the scan loop — the gallery must keep filling
incrementally at full speed). ML Kit Image Labeling (GMS-backed, keyword
labels) is the baseline; ML Kit GenAI Image Description (AICore / Gemini
Nano, natural-language captions) is an opt-in bonus on capable devices.
Everything is on-device. Nothing is bundled into the APK — the models live
in Play Services / AICore, so the feature is simply absent on non-Google
devices and the app keeps working without it.

New Settings → **Smart Tags** sub-page: master toggle, captions toggle,
tagged-count + approximate storage size, re-tag-all, and clear-tag-data.

## Decisions (locked from intent)

- **Labels default ON, captions default OFF.** User said "do it during
  scanning, let me toggle it off" → labels on. Captions are heavier +
  device-limited → opt-in.
- **GMS-backed labeling** (`play-services-mlkit-image-labeling`), not the
  bundled model — no APK bloat; absent gracefully off-GMS.
- **Decoupled from the scan loop.** Trigger the pass on `ScanProgress.Done`
  + once at graph init. Scan code is untouched.
- **Storage size is an estimate** (`SUM(LENGTH(label))` + row overhead) —
  tags live in the shared SQLite file, can't be isolated exactly. Shown
  with a "≈".
- **Status table** so a photo is processed once even if it yields zero
  tags; re-tag = wipe status + re-run.

## Phase A — Build wiring & deps ✅
- [x] **A.1** Add `play-services-mlkit-image-labeling` + `genai-image-description` to `libs.versions.toml`
- [x] **A.2** Add both `implementation`s to `app/build.gradle.kts`
- [x] **A.3** Resolve licensee: `allowUrl` the two proprietary Google SDK license URLs; filter no-SPDX artifacts out of the OSS list in `parseLicensesCatalog`; parse test updated
- [x] **A.4** Add ML Kit `meta-data` (`com.google.mlkit.vision.DEPENDENCIES = ica`) to AndroidManifest

## Phase B — Data layer (Room) ✅
- [x] **B.1** `PhotoTagEntity` (`photo_tags`: id, photo_id FK→photos CASCADE, label, confidence, source) + unique index (photo_id,label,source)
- [x] **B.2** `PhotoTagStatusEntity` (`photo_tag_status`: photo_id PK, status, tagged_at_ms, engine) — no FK (survives transient scans, favorites pattern)
- [x] **B.3** `PhotoTagDao` (upsertTags, markStatus, untaggedPhotoIds, observeProcessedCount, observeTotalPhotos, observeTagCount, observeLabelByteSum, tagsForPhoto, wipeTags, wipeStatus)
- [x] **B.4** Bump DB version 2→3, `MIGRATION_2_3`, register entities + `photoTags()` DAO; schema 3.json exported (DDL matches migration)
- [x] **B.5** Room migration round-trip test (`Migration2to3Test`) — tables created, rows accepted, FK cascade

## Phase C — Tagger abstraction + ML Kit impl ✅
- [x] **C.1** `ImageTagger` interface + `TagCandidate`/`TagSource`/`TaggerAvailability` (test seam)
- [x] **C.2** `MlKitImageTagger` — labeling (GMS `Task`) + GenAI description (Guava `ListenableFuture`), both awaited without extra deps; never throws; GMS/AICore availability probes
- [x] **C.3** `TaggingBitmapSource` interface + `ContentResolverBitmapSource` (loadThumbnail API≥29, BitmapFactory fallback)

## Phase D — Preferences ✅
- [x] **D.1** `TaggingPreferences` interface (enabled default true, captionsEnabled default false)
- [x] **D.2** `DataStoreTaggingPreferences` impl (shared `shutterboy_settings` store)
- [x] **D.3** Codec round-trip test (`DataStoreTaggingPreferencesTest`)

## Phase E — Service + wiring ✅
- [x] **E.1** `AiTaggingService` — background pass (untagged→decode→analyze→upsert+status), single-flight guard, cancellable, `observeStats()`, `start()`, `retagAll()`, `wipe()`, `runState`
- [x] **E.2** Wire into `AppGraph`: construct service, expose facet, trigger on `scanProgress()==Done`; react to enabled toggle (collector covers cold start)
- [x] **E.3** Add `taggingPreferences` + `aiTagging` to `RouteScope` + factory
- [x] **E.4** Service test with fakes + in-memory Room (tags written/status marked, disabled+unsupported no-ops, wipe, retag, caption-flag propagation)

## Phase F — Settings UI ✅
- [x] **F.1** `Destination.SettingsSmartTags` + route registration (`SettingsRoutes` + `ShutterboyApp` entry)
- [x] **F.2** `SettingsSmartTagsScreen` (availability banner, 2 switch rows, stats card + progress, re-tag + clear buttons w/ confirm dialogs)
- [x] **F.3** Root card "Smart Tags" row in `SettingsScreen`
- [x] **F.4** `SmartTagsSectionCatalog` + register in `SettingsCatalog` + update `SettingsCatalogTest`
- [x] **F.5** All strings in `values/strings.xml` (plain/factual per editorial brief)

## Phase G — Verify & ship
- [x] **G.1** `:app:testDebugUnitTest` + `:app:assembleDebug` green (migration, service-with-fakes, prefs codec, catalog pin-down, license-parse all pass)
- [ ] **G.2** AVD smoke — SKIPPED this run: emulator-5554 shell wedged (device lists but `adb shell` hangs); not a code issue. Phone-via-Obtainium is the real test loop.
- [ ] **G.3** Commit + `scripts/build-release-apk.sh --gh-release`

## Status: shipping — labeling baseline + GenAI captions, on-device only, decoupled from scan loop
