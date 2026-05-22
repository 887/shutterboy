# shutterboy

Modern Android photo gallery. Built on Jetpack Compose + Coil 3 + Room. Sibling app to [`tonearmboy`](https://github.com/887/tonearmboy) — same architectural shape, same release pipeline, same Obtainium distribution.

## Status

Phase 0 shipped. Android project scaffolded, `./gradlew :app:assembleDebug` clean, hello-world cold-boot verified on `emulator-5554`, tiger artwork in place as the launcher icon and triple-tap easter egg, splash screen wired, release script working. See [`docs/plans/main.md`](docs/plans/main.md) for the phased build plan and the next phase up.

## Translations

Translations are produced per-language by the user + Claude; English is canonical, missing keys fall back to English at runtime.

<!-- TRANSLATIONS-START -->

| Language | Coverage | Status |
| --- | --- | --- |
| [German](app/src/main/res/values-de/) | 225/244 (92%) | in progress |

<!-- TRANSLATIONS-END -->

## Goals

- All the basics: device photos browsed by folder / album / date, fullscreen viewer with pinch-zoom, slideshow, share-sheet, EXIF inspector.
- **Delete photos directly from the gallery**, with the system consent dialog and gallery cache invalidation. Single and bulk.
- **Search** across filename + EXIF (capture date, lens, location).
- Multi-source library: device MediaStore + user-picked SAF folders (SD card, USB OTG).
- Modern stack: Kotlin + Compose + Coil 3 + Room. No legacy Android Views, no Java.
- **Built entirely from the CLI**, no Android Studio required.

## Non-goals

- Cloud sync / cloud backup.
- Photo editing (crop, filter, retouch). Read-only viewer in v1.
- Video playback (still images only in v1; videos shown as thumbnails that hand off to a system viewer).
- Face / object recognition. ML on-device is out of scope for v1.
- Tablet-specific layouts (works on phone, scales to tablet later).

## Install on Android via Obtainium

[Obtainium](https://github.com/ImranR98/Obtainium) is an open-source Android
app store that pulls APKs directly from GitHub Releases. No Play Store, no
sideload dance, auto-update on every new release. Works on de-Googled Androids
(GrapheneOS / CalyxOS / LineageOS).

### One-tap install (if Obtainium is already on your phone)

Tap this link on your phone:

[`obtainium://add/https%3A%2F%2Fgithub.com%2F887%2Fshutterboy`](obtainium://add/https%3A%2F%2Fgithub.com%2F887%2Fshutterboy)

Obtainium opens, prefills the source, and shows **Add**.

### Manual install

1. Install Obtainium from [F-Droid](https://f-droid.org/en/packages/dev.imranr.obtainium.fdroid/) or [its GitHub releases](https://github.com/ImranR98/Obtainium/releases/latest).

2. In Obtainium, tap **Add App** → paste this **Source URL**:

   ```
   https://github.com/887/shutterboy
   ```

3. The other fields auto-detect, but if you need to set them by hand:

   | Field            | Value                     |
   | ---------------- | ------------------------- |
   | Source type      | GitHub                    |
   | APK filter regex | `^shutterboy-.*\.apk$`    |
   | Update channel   | Releases                  |

4. Tap **Add**. Obtainium fetches `shutterboy-<version>-<sha7>.apk` from the latest release and offers Install. Future releases trigger an auto-update notification.

### Verifying a build

Each release ships a "Verify build" table in its notes with the APK SHA-256.
After installing, confirm what you got matches:

```bash
adb shell pm path com.eight87.shutterboy        # find the installed APK on your device
adb pull <path-from-above> /tmp/installed.apk   # pull it back
sha256sum /tmp/installed.apk                    # compare to the release notes
```

---

The rest of this README is for **developers** — building locally, running the
AVD, running smoke tests, shipping a release. Skip if you just want the app.

## Prerequisites (one-time, Linux)

```bash
# Android CLI 0.7+
curl -fsSL https://dl.google.com/android/cli/latest/linux_x86_64/android \
  -o ~/.local/bin/android && chmod +x ~/.local/bin/android
android --version          # self-bootstraps the runtime + bundled JDK 21

# SDK packages
android sdk install platforms/android-34 build-tools/34.0.0

# Test target — headless AVD
android emulator create --profile=medium_phone

# Mirror utility (optional but recommended for visual QA)
sudo pacman -S scrcpy      # Arch / Manjaro
# (or your distro's equivalent — Debian/Ubuntu: `sudo apt install scrcpy`)
```

The Android CLI also fetches the emulator binary and a system image on first
`android emulator create`. Expect ~600 MB of downloads on a fresh box.

## Run the AVD + scrcpy

```bash
scripts/start-avd.sh             # boot AVD if not running, then attach scrcpy
scripts/start-avd.sh --no-mirror # AVD only, no scrcpy window
scripts/start-avd.sh --kill      # stop both
```

The AVD boots headless (`-no-window -no-audio -no-snapshot`) for ~3 GB resident
RAM. `scrcpy` then mirrors the display to a host window (Wayland / X11) without
restarting the emulator. Once running, `adb devices` shows `emulator-5554`.

## Build + install

```bash
# Direct gradlew calls need both env vars (see CLAUDE.md for the why):
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk
export ANDROID_HOME=$HOME/Android/Sdk

./gradlew assembleDebug
android run --apks=app/build/outputs/apk/debug/app-debug.apk --device=emulator-5554
```

Or via the Android CLI directly (which handles the toolchain internally):

```bash
android run --apks=app/build/outputs/apk/debug/app-debug.apk
```

## Build a release APK

The canonical happy path is **"phone-vibing"**: you're on your phone, you tell
Claude (in the Claude app) to ship a new build of shutterboy. Claude opens a
session against this repo on your dev machine, runs:

```bash
scripts/build-release-apk.sh --gh-release
```

…and the new APK shows up on `https://github.com/887/shutterboy/releases/latest`.
You then pull it to your phone via [Obtainium](#install-on-android-via-obtainium), which
auto-detects the new release and offers an in-place update. No Play Store, no
Android Studio, no manual `adb`.

The script supports three flags, individually or combined:

```bash
# 1. Build only — APK lands at release/shutterboy-<version>-<sha7>.apk
scripts/build-release-apk.sh

# 2. Build + upload to GitHub Releases (uses gh CLI; creates a vN.N.N-<sha7> tag)
scripts/build-release-apk.sh --gh-release

# 3. Build + adb install onto the connected device (AVD or wifi-adb phone)
scripts/build-release-apk.sh --install

# Combine flags — the full local one-shot:
scripts/build-release-apk.sh --gh-release --install
```

By default the APK is signed with Gradle's debug keystore (good enough for
personal sideload). For production-signed releases set the
`SHUTTERBOY_RELEASE_KEYSTORE`, `SHUTTERBOY_RELEASE_KEY_ALIAS`, and
`SHUTTERBOY_RELEASE_KEY_PASSWORD` environment variables before running, and the
script switches to `assembleRelease`.

The `release/` directory is gitignored. Each build also writes a
`release/latest.apk` symlink for convenience.

## GitHub Actions fallback

`.github/workflows/release.yml` is a **fallback** for when the local build
isn't available — for example, if you're shipping from a phone via the GitHub
web UI. It triggers **only** on `push: tags: [v*]`; it never runs on regular
pushes, PRs, or schedule, so the default cost is zero CI minutes.

The workflow is **self-disabling**: at the start of the job it queries the
matching release; if any asset already matches `shutterboy-*.apk` (which is what
`scripts/build-release-apk.sh --gh-release` uploaded), it exits 0 without
rebuilding. So for the normal local-build flow, even though the tag push
triggers the workflow, no work happens.

If you've configured a release-signing keystore, set repo secrets
`RELEASE_KEYSTORE_BASE64`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`, and the
fallback build will use `assembleRelease` instead of `assembleDebug`.

## Populate test photos + smoke-test

```bash
scripts/fetch-test-photos.sh          # downloads CC-licensed sample photos into test-photos/ (gitignored)
scripts/push-test-photos.sh           # pushes to the running AVD, triggers MediaStore scan
scripts/fetch-test-photos.sh --push   # fetch + push in one shot
scripts/gallery-smoke-test.sh         # exercises Phase C scan path
scripts/ui-smoke-test.sh              # exercises Phase D (folder browse, fullscreen viewer, share, delete)
```

Photos land at `/sdcard/DCIM/shutterboy-test/` on the device. After pushing, open
shutterboy and hit **Settings → Rescan photos** if the gallery doesn't pick them up
automatically.

`test-photos/` is gitignored — re-fetch on a fresh checkout via the script.

## Test

See [`CLAUDE.md`](CLAUDE.md) for the full Claude-driven test loop.

- **Unit / data layer** — Robolectric, JVM-only, zero device.
- **UI / integration** — [`mobile-mcp`](https://github.com/mobile-next/mobile-mcp) over ADB driving the headless AVD (or a real phone via wifi-adb).

## License

MIT. See [`LICENSE`](LICENSE).
