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

## Phase G.1 — swipe-down to dismiss

- [ ] **G.1.1** Add a `NestedScrollConnection` at the `PhotoViewerScreen` scaffold's body modifier.
  - `onPostScroll`: accumulate downward `available.y` into a state variable. Consume so it doesn't bubble further.
  - `onPreScroll`: drain accumulated over-scroll first when the user starts dragging back up.
  - `onPreFling`: if accumulated over-scroll exceeds 128 dp, call `onBack`. Reset accumulator.
- [ ] **G.1.2** Threshold = 128 dp, converted via `with(LocalDensity.current) { 128.dp.toPx() }`. Captured into a local val so the gesture handler doesn't recompute per-event.
- [ ] **G.1.3** Verify mid-pinch is untouched: when `Modifier.transformable` claims a pointer for pinch, the nested-scroll connection sees zero `available.y` and the accumulator stays at 0.
- [ ] **G.1.4** Verify horizontal pager is untouched: an unambiguous horizontal swipe routes to the pager; the nested-scroll accumulator only grows on dominantly-vertical drags (the pager's gesture detector wins horizontal ones first).
- [ ] **G.1.5** AVD smoke: swipe down on a photo from the viewer → pops back to the grid; swipe down mid-pinch → pinch continues, no pop; quick horizontal swipe → page change, no pop.

## Phase G.2 — swipe-up for info panel

- [ ] **G.2.1** Add `Modifier.pointerInput { detectVerticalDragGestures }` to the viewer page content (NOT the chrome layers). Track accumulated vertical delta in a `mutableStateOf<Float>(0f)`. On `onDragEnd`:
  - `delta < -threshold` (swiped up) → open the info `ModalBottomSheet` (same handle the Info icon uses)
  - `delta > threshold` (swiped down) → fall through to the G.1 nested-scroll dismiss path (so a long down-drag still dismisses even if it's faster than the pre-fling threshold expects)
  - else → no-op
  - reset delta to 0 in all cases.
- [ ] **G.2.2** Threshold = 64 dp, same density-px conversion as G.1.
- [ ] **G.2.3** Verify the existing single-tap (chrome toggle) and double-tap (zoom toggle) still fire — `detectVerticalDragGestures` only claims events with actual movement.
- [ ] **G.2.4** Verify the info-sheet's own swipe-to-dismiss still works once it's open — the sheet manages its own gestures; G.2 only handles the *opening* swipe on the photo behind it.
- [ ] **G.2.5** AVD smoke: swipe up on a photo → info sheet opens; tap Info icon → same sheet opens; swipe up while sheet is already open → no-op.

## Phase G.3 — verify pinch / double-tap / single-tap paths still work

- [ ] **G.3.1** AVD smoke: pinch-to-zoom on a photo → zoom works; release → no dismiss, no info-open.
- [ ] **G.3.2** AVD smoke: double-tap → toggles 1× ↔ 2×.
- [ ] **G.3.3** AVD smoke: single tap → toggles chrome.
- [ ] **G.3.4** AVD smoke: horizontal swipe → page change.

## Phase G.4 — ship + tick

- [ ] **G.4.1** Tick every G.1 / G.2 / G.3 sub-step.
- [ ] **G.4.2** Mark the phase header with the commit range.
- [ ] **G.4.3** Cross-tick: confirm main.md Phase F is shipped (F.1 + F.2 + F.3 at minimum — the pager, the chrome, the info sheet — before G ships).

**Effort:** S (1–2 hours). **Risk:** low — gesture additions on top of the existing pager + transformable + clickable; existing tap + back-arrow paths stay intact.

## Status

Pending — depends on main.md Phase F.1 + F.2 + F.3 landing first. Can
ship in the same commit as F.3 if convenient, or immediately after as
its own commit.
