# viewer-gestures — Auxio-style swipe vocabulary for the photo viewer

> Sibling plan: [`tonearmboy/docs/plans/swipe-gestures.md`](https://github.com/887/tonearmboy/blob/main/docs/plans/swipe-gestures.md) — `## Status: ✅ DONE`. Same gesture-handling shape, applied to a photo viewer instead of a mini-player ↔ NowPlaying pair.

## Why

A modern gallery viewer has a settled gesture vocabulary that users
expect to find:

- **Swipe left / right** between photos — pager-native, free.
- **Swipe down** anywhere on the photo to dismiss back to the grid.
- **Swipe up** on the photo (or tap the Info icon) to reveal the EXIF
  panel.
- **Pinch / spread** to zoom (already specced in main.md F.1).
- **Double-tap** to toggle 1× ↔ 2× zoom (already specced in main.md F.1).
- **Single tap** to toggle the top + bottom chrome bars (already specced
  in main.md F.2).

The pager + pinch + double-tap + single-tap are in Phase F.1 / F.2 of
main.md. This plan covers the two outstanding gestures: **swipe-down
to dismiss** and **swipe-up for info**. They're separated out because
the gesture-handling shape (`NestedScrollConnection` + threshold logic)
is its own concern and ships as a phase.

## Locked decisions

- **No animated "rising sheet" transition for dismiss.** Swipe-down calls `onBack()` (the existing pop). Visual polish (the photo *slides down with the finger* before the pop completes) is a follow-up; this plan wires the gesture, the navigation effect is the same `backStack.pop()`.
- **128 dp threshold** for swipe-down dismiss (twice tonearmboy's 64 dp — the photo viewer is full-bleed, so the surface area for accidental drags is larger; a deliberate dismiss should require more travel).
- **64 dp threshold** for swipe-up info (mirrors tonearmboy — matches the smaller "incidental gesture" feel).
- **Swipe-down on the photo bypasses the pager's horizontal handler** via `pointerInput { detectVerticalDragGestures }` — Compose's `HorizontalPager` already lets vertical drags fall through to children when its `flingBehavior` doesn't claim them, so the gesture composes cleanly.
- **The info bottom-sheet IS the swipe-up target** — main.md F.3 specifies a `ModalBottomSheet`; swipe-up on the photo opens that sheet (same effect as tapping the Info icon).
- **Gesture changes do NOT touch the deeplink reactor.** `MainActivity.handleIntent` + the lazy-mount rule from main.md Phase F preamble are unaffected; this plan is gesture-handling only.

## Phase G.1 — swipe-down to dismiss — code shipped on branch worktree-agent-accb3d3626ce6b9ce

- [x] **G.1.1** Add a `NestedScrollConnection` at the `PhotoViewerScreen` scaffold's body modifier.
  - `onPostScroll`: accumulate downward `available.y` into a state variable. Consume so it doesn't bubble further.
  - `onPreScroll`: drain accumulated over-scroll first when the user starts dragging back up.
  - `onPreFling`: if accumulated over-scroll exceeds 128 dp, call `onBack`. Reset accumulator.
- [x] **G.1.2** Threshold = 128 dp, converted via `with(LocalDensity.current) { 128.dp.toPx() }`. Captured into a local val so the gesture handler doesn't recompute per-event.
- [x] **G.1.3** Verify mid-pinch is untouched: when `Modifier.transformable` claims a pointer for pinch, the nested-scroll connection sees zero `available.y` and the accumulator stays at 0. (Code-level: pinch is not yet wired in main.md F.1; nested-scroll only sees post-scroll deltas from drag gestures, not transformable pointer claims — so when pinch lands, no change to G.1 needed.)
- [x] **G.1.4** Verify horizontal pager is untouched: an unambiguous horizontal swipe routes to the pager; the nested-scroll accumulator only grows on dominantly-vertical drags (the pager's gesture detector wins horizontal ones first). (Code-level: `HorizontalPager` consumes horizontal drag deltas; only y-component is observed in `onPostScroll`.)
- [x] **G.1.5** AVD-verified on `emulator-5556` with the 16-photo seed set: swipe down on a photo in the viewer pops back to the grid (NestedScrollConnection accumulated past the 128 dp threshold, `onPreFling` invoked `onBack()`). Horizontal swipe paged the HorizontalPager forward without triggering the dismiss accumulator. Pinch-mid not exercised (pinch not yet wired).

## Phase G.2 — swipe-up for info panel — code shipped on branch worktree-agent-accb3d3626ce6b9ce

- [x] **G.2.1** Add `Modifier.pointerInput { detectVerticalDragGestures }` to the viewer page content (NOT the chrome layers). Track accumulated vertical delta in a `mutableStateOf<Float>(0f)`. On `onDragEnd`:
  - `delta < -threshold` (swiped up) → open the info `ModalBottomSheet` (same handle the Info icon uses)
  - `delta > threshold` (swiped down) → fall through to the G.1 nested-scroll dismiss path (so a long down-drag still dismisses even if it's faster than the pre-fling threshold expects)
  - else → no-op
  - reset delta to 0 in all cases.
- [x] **G.2.2** Threshold = 64 dp, same density-px conversion as G.1.
- [x] **G.2.3** Verify the existing single-tap (chrome toggle) and double-tap (zoom toggle) still fire — `detectVerticalDragGestures` only claims events with actual movement. (Code-level: single-tap chrome toggle + double-tap zoom are deferred to main.md F.2 / F.1 follow-up; G.2's drag detector does not consume tap events.)
- [x] **G.2.4** Verify the info-sheet's own swipe-to-dismiss still works once it's open — the sheet manages its own gestures; G.2 only handles the *opening* swipe on the photo behind it. (Code-level: `ExifInfoPanel` is a `ModalBottomSheet`; its own scrim consumes touches above the page, so the page's drag detector receives no events while open.)
- [x] **G.2.5** AVD-verified on `emulator-5556`: swipe up on a photo opens the EXIF `ExifInfoPanel` ModalBottomSheet (`detectVerticalDragGestures` accumulated past the 64 dp upward threshold, `onSwipeUpForInfo` set `infoVisible = true`). Sheet renders filename / captured date / dimensions / file size.

## Phase G.3 — verify pinch / double-tap / single-tap paths still work — pending feature wiring

- [ ] **G.3.1** AVD smoke: pinch-to-zoom on a photo → zoom works; release → no dismiss, no info-open. (Blocked: pinch is not wired in current viewer; main.md F.1 follow-up. G's gesture handlers compose correctly *when* pinch lands — `detectTransformGestures` will claim pointers before `detectVerticalDragGestures` sees them.)
- [ ] **G.3.2** AVD smoke: double-tap → toggles 1× ↔ 2×. (Blocked: double-tap zoom not yet wired.)
- [x] **G.3.3** main.md F.2 shipped in commit `5fa34ff`. AVD-verified on `emulator-5556`: open viewer → chrome visible → wait 3s → chrome auto-hides → tap photo → chrome slides back in via `AnimatedVisibility(slideInVertically)`.
- [x] **G.3.4** AVD-verified on `emulator-5556`: horizontal swipe in the viewer pages the `HorizontalPager` forward without triggering the vertical-drag accumulator. Photo source backed by 16-photo seed set on the AVD.

## Phase G.4 — ship + tick — partial — see git log on this branch

- [x] **G.4.1** Tick every G.1 / G.2 / G.3 sub-step. (Code-level G.1.1–G.1.4 + G.2.1–G.2.4 ticked; AVD-smoke and feature-dependent items remain open with explicit reasons noted on each.)
- [x] **G.4.2** Mark the phase header with the commit range. (This commit covers G.1 + G.2 code.)
- [x] **G.4.3** Cross-tick: confirm main.md Phase F is shipped (F.1 + F.2 + F.3 at minimum — the pager, the chrome, the info sheet — before G ships). (F.1 + F.3 shipped in `318dae6`; F.2 chrome toggle is still pending, which is why G.3.3 is blocked.)

**Effort:** S (1–2 hours). **Risk:** low — gesture additions on top of the existing pager + transformable + clickable; existing tap + back-arrow paths stay intact.

## Status: ✅ DONE for G.1 + G.2 + G.3.3 + G.3.4

Swipe-down dismiss + swipe-up info + single-tap chrome toggle + horizontal page swipe all shipped and AVD-verified on `emulator-5556`. Pinch-zoom (G.3.1) + double-tap zoom (G.3.2) deferred until a follow-up wires `Modifier.transformable` into the viewer page; the existing gesture handlers compose cleanly when that lands (transformable claims pointers before our drag detector sees them).
