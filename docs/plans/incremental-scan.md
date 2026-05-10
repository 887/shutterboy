# incremental-scan — skip the MediaStore rescan when nothing changed

> Sibling plan: [`tonearmboy/docs/plans/...`](https://github.com/887/tonearmboy) — commits `d0a566d` ("fast cold start: skip MediaStore rescan when nothing changed + tighter splash hold") and `50f1d6b` ("fix: stop scanning the library twice on cold start").

The fastest scan is the one you don't run. Shutterboy currently re-walks
`MediaStore.Images.Media` on every cold boot, computes a full diff, then
applies it to Room. On a clean device library that delta is empty and
the work is wasted. `MediaStore.getGeneration(volumeName)` returns a
monotonic token that bumps **only** when the volume's contents change —
comparing it to a persisted token tells us whether the scan is needed at
all, in O(1).

This plan is paired with [`cold-start-perf.md`](cold-start-perf.md). The
cold-start plan is preventive standing rules; this plan is a one-shot
delta of work plus a standing maintenance rule.

## Phase A — generation-token plumbing — shipped in commit `962ebdb`

- [x] **A.1** Add a `data/scan/MediaStoreGeneration.kt` thin wrapper: `fun current(context: Context, volume: String = MediaStore.VOLUME_EXTERNAL_PRIMARY): Long = MediaStore.getGeneration(context, volume)`. Single-purpose, no logic — keeps the API-26-vs-30 surface in one place. `MediaStore.getGeneration` requires API 29+; on API 26-28 the wrapper returns `-1L` (sentinel: always rescan).
- [x] **A.2** Persist the last-observed generation token in DataStore as `media_store_generation_<volumeName>`. Live alongside the existing settings prefs (`shutterboy_settings`) — a separate prefs file isn't worth it. — `ScanGatePreferences` interface + `DataStoreScanGatePreferences` impl behind `Context.shutterboyPrefs`.
- [x] **A.3** Persist the last-observed SAF tree fingerprint set: `Map<safTreeUri, lastChildCount>`. SAF doesn't have a generation API, so a coarse "fingerprint = recursive image-leaf count" is the cheapest reliable cache invalidator. — `SafSourceManager.fingerprint(treeUris)` does a count-only recursive walk (no dimension reads, no `ScannedPhoto` allocation); persisted as `saf_fingerprint` string under `uri=count;uri=count` wire format. Malformed entries dropped silently on decode.

## Phase B — wire it into `LibraryRepository` — shipped in commit `962ebdb`

- [x] **B.1** `RoomGalleryRepository.scanIfChanged()` — entry point that replaces the unconditional `scan()` call on first `observePhotos` collection. Compares `MediaStoreGeneration.current()` against persisted token + SAF fingerprints against persisted map. If both match, return without touching the scanner. If either differs, run the full scan, then persist the fresh tokens at the end (NOT at the start — a crashed scan must re-run next boot). — `LibraryScanner` interface gains `scanIfChanged()` + `forceRescan()` alongside the existing `runScan()`. Implementation extracts the scan body into a private `executeScan()` that throws, so the gate path can persist tokens only on success (not via `runCatching.getOrElse`-swallowed failures).
- [ ] **B.2** `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` (already exists) keeps its existing role — it triggers an in-session rescan when the user adds a photo via another app. It doesn't replace the cold-start gate; it complements it.
- [x] **B.3** **Hard rule: never scan twice on cold start.** The bug `50f1d6b` fixed in tonearmboy was the repository firing a scan on first DAO subscription *and* the activity firing one on `onCreate`. Audit for double-entry before B.1 lands — there should be exactly one `scanIfChanged()` call site, owned by the repository's lazy first-collect path. — first-collect hook landed: `LaunchedEffect(Unit) { graph.libraryScanner.scanIfChanged() }` at the root of `ShutterboyApp`. Single call site, runs post-first-frame off the cold-start critical path (cold-start-perf A.2). No other scan triggers exist; future Settings → Rescan UI (Phase I.3) MUST route through `forceRescan()`, never `runScan()` directly.

**Known limitation:** when `READ_MEDIA_IMAGES` is unset the scan completes successfully with 0 rows, then persists the generation token — so the gate matches on subsequent boots and no rescan fires when the user grants the permission later. Permission-state should clear the gate when it flips. Tracked separately; lands with the Phase I.3 permission UX.
- [x] **B.4** Manual override: Settings → Library → "Rescan photos" (already specced in main.md Phase I.3) bypasses the generation gate. The action calls `rescanNow()` which clears the persisted tokens *then* invokes the scanner — so a force-rescan also re-seeds the gate. — `LibraryScanner.forceRescan()` clears `scanGate` then delegates to `scanIfChanged()`, which sees a fresh-empty gate and runs the full scan. Phase I.3 UI is the consumer-to-be.

## Phase C — verification

- [x] **C.1** Robolectric: `DataStoreScanGatePreferencesTest` — 9 cases covering generation-token round-trip + per-volume independence, SAF fingerprint round-trip + empty-map clearing, `clear()` wiping both surfaces, malformed-encoded-string decode, null/empty decode. — passed locally (122 tests total, 0 failures).
- [ ] **C.2** Robolectric: `RoomGalleryRepositoryScanGateTest` — fake `MediaStoreGeneration` returns matching token → scanner is *not* invoked; differing token → scanner *is* invoked and the new token is persisted; scanner-throws-mid-scan → token is NOT persisted (the next boot re-attempts). **Requires a small refactor** — `MediaStoreGeneration` is a static `object`; tests need a seam (either inject a `(Context, String) -> Long` lambda on the repo, or extract a `MediaStoreGenerationSource` interface). Tracked as follow-up; gate logic itself is exercised end-to-end on the AVD by C.3 / C.4.
- [ ] **C.3** AVD smoke: cold-boot the app twice in a row with no library changes between boots. Second boot's `logcat -s shutterboy:*` should show "skipped scan, generation unchanged"; cold-start time should drop by 100–400 ms depending on library size.
- [ ] **C.4** AVD smoke: add a photo via the camera app between boots; second boot should NOT skip — scanner runs, new photo lands in the grid.

## Phase D — standing rule (maintenance contract)

- [ ] **D.1** **The cold-start path runs `scanIfChanged()`, not `scan()`.** Any new entry point that bypasses the gate is a regression. Add a comment to `scan()` documenting it as the force-rescan / test-only entrypoint.
- [ ] **D.2** **Token persistence happens after success, not before.** A scan that throws halfway must re-run next boot.
- [ ] **D.3** **The `ContentObserver` and the cold-start gate are independent.** Don't try to clever-merge them — they serve different windows (in-session vs cross-boot).

## Status

Pending — no sub-steps shipped. Targeted to land before Phase F so the
viewer's cold-deeplink path benefits from the gate.
