# Smoke-test entrypoints

shutterboy's CLI smoke tests live as POSIX shell scripts under
[`scripts/`](../scripts/), not as Maestro YAML flows. Adopting Maestro
is deferred to v1.x — see main.md L.10 for the rationale. This
directory exists so future Maestro flows have an obvious home; today
it's documentation only.

## Running the existing smoke tests

Prerequisites:
- A connected ADB target (real device via wifi-adb, or the headless
  AVD `medium_phone` booted via
  `scripts/start-avd.sh`).
- `JAVA_HOME=/usr/lib/jvm/java-26-openjdk` +
  `ANDROID_HOME=$HOME/Android/Sdk` exported for direct Gradle calls
  (see [`CLAUDE.md`](../CLAUDE.md) for the JDK quirk).
- `magick` (ImageMagick) on `$PATH` for inline screencap downscaling.

### `scripts/gallery-smoke-test.sh`

End-to-end gallery walk: build the debug APK → install → push a small
set of test photos to `/sdcard/DCIM/shutterboy-test/` → cold-boot the
app → confirm the timeline populates → tap into the viewer → swipe to
next photo → back out → switch to Collections → open a folder → back.
Stops at any failure with a non-zero exit code.

```
SERIAL=emulator-5556 scripts/gallery-smoke-test.sh
```

### `scripts/ui-smoke-test.sh`

Faster check: tab through Photos / Collections / Settings and
screencap each. Assumes either `gallery-smoke-test.sh` has already
seeded photos + granted media permissions, or the app already has
state.

```
SERIAL=emulator-5556 scripts/ui-smoke-test.sh
```

## Why no Maestro yaml today

- The bash flows are good-enough for the v0.1 / v1.0 surface area.
- Maestro adds a runtime dependency (the `maestro` CLI) to the
  dev-machine prerequisite list. The shell flows just need `adb` +
  `magick`, both already on this user's machine.
- When v1.x grows surface area where Maestro's snapshot-diffing or
  CI-friendly reporting pays off, we'll port the bash flows here.
  Track that decision in main.md before the port lands.
