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

## Phase A — generation-token plumbing

- [ ] **A.1** Add a `data/scan/MediaStoreGeneration.kt` thin wrapper: `fun current(context: Context, volume: String = MediaStore.VOLUME_EXTERNAL_PRIMARY): Long = MediaStore.getGeneration(context, volume)`. Single-purpose, no logic — keeps the API-26-vs-30 surface in one place. `MediaStore.getGeneration` requires API 29+; on API 26-28 the wrapper returns `-1L` (sentinel: always rescan).
- [ ] **A.2** Persist the last-observed generation token in DataStore as `media_store_generation_<volumeName>`. Live alongside the existing settings prefs (`shutterboy_settings`) — a separate prefs file isn't worth it.
- [ ] **A.3** Persist the last-observed SAF tree fingerprint set: `Map<safTreeUri, lastChildCount>`. SAF doesn't have a generation API, so a coarse "fingerprint = recursive image-leaf count" is the cheapest reliable cache invalidator. (Hash of sorted (uri, lastModified) pairs is more accurate but blows the budget; child count drift is good enough — any add / remove flips it.)

## Phase B — wire it into `LibraryRepository`

- [ ] **B.1** `RoomGalleryRepository.scanIfChanged()` — entry point that replaces the unconditional `scan()` call on first `observePhotos` collection. Compares `MediaStoreGeneration.current()` against persisted token + SAF fingerprints against persisted map. If both match, return without touching the scanner. If either differs, run the full scan, then persist the fresh tokens at the end (NOT at the start — a crashed scan must re-run next boot).
- [ ] **B.2** `ContentObserver` on `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` (already exists) keeps its existing role — it triggers an in-session rescan when the user adds a photo via another app. It doesn't replace the cold-start gate; it complements it.
- [ ] **B.3** **Hard rule: never scan twice on cold start.** The bug `50f1d6b` fixed in tonearmboy was the repository firing a scan on first DAO subscription *and* the activity firing one on `onCreate`. Audit for double-entry before B.1 lands — there should be exactly one `scanIfChanged()` call site, owned by the repository's lazy first-collect path.
- [ ] **B.4** Manual override: Settings → Library → "Rescan photos" (already specced in main.md Phase I.3) bypasses the generation gate. The action calls `rescanNow()` which clears the persisted tokens *then* invokes the scanner — so a force-rescan also re-seeds the gate.

## Phase C — verification

- [ ] **C.1** Robolectric: `MediaStoreGenerationStoreTest` — read / write / round-trip per volume name; `-1L` sentinel preserved through encode/decode.
- [ ] **C.2** Robolectric: `RoomGalleryRepositoryScanGateTest` — fake `MediaStoreGeneration` returns matching token → scanner is *not* invoked; differing token → scanner *is* invoked and the new token is persisted; scanner-throws-mid-scan → token is NOT persisted (the next boot re-attempts).
- [ ] **C.3** AVD smoke: cold-boot the app twice in a row with no library changes between boots. Second boot's `logcat -s shutterboy:*` should show "skipped scan, generation unchanged"; cold-start time should drop by 100–400 ms depending on library size.
- [ ] **C.4** AVD smoke: add a photo via the camera app between boots; second boot should NOT skip — scanner runs, new photo lands in the grid.

## Phase D — standing rule (maintenance contract)

- [ ] **D.1** **The cold-start path runs `scanIfChanged()`, not `scan()`.** Any new entry point that bypasses the gate is a regression. Add a comment to `scan()` documenting it as the force-rescan / test-only entrypoint.
- [ ] **D.2** **Token persistence happens after success, not before.** A scan that throws halfway must re-run next boot.
- [ ] **D.3** **The `ContentObserver` and the cold-start gate are independent.** Don't try to clever-merge them — they serve different windows (in-session vs cross-boot).

## Status

Pending — no sub-steps shipped. Targeted to land before Phase F so the
viewer's cold-deeplink path benefits from the gate.
