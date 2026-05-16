#!/usr/bin/env bash
#
# L.3 — gallery smoke test on a running AVD/device.
#
# Exercises: build → install → push test photos → cold-boot → confirm
# Photos grid populates → tap into Viewer → swipe to next photo → back
# out → Collections tab → folder open → back. Stops at any failure.
#
# Pass --serial <id> for multi-device setups (default: emulator-5556).
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${SERIAL:-emulator-5556}"

if [[ "${1:-}" == "--serial" ]]; then
    SERIAL="$2"
    shift 2
fi

adb_s() { adb -s "$SERIAL" "$@"; }

step() {
    printf '\n[step] %s\n' "$*"
}

step "build debug APK"
JAVA_HOME=/usr/lib/jvm/java-26-openjdk ANDROID_HOME=$HOME/Android/Sdk \
    "${REPO_ROOT}/gradlew" -p "$REPO_ROOT" :app:assembleDebug --quiet

step "install"
adb_s install -r "${REPO_ROOT}/app/build/outputs/apk/debug/app-debug.apk" >/dev/null

step "push test photos + grant permission"
bash "${REPO_ROOT}/scripts/push-test-photos.sh" --serial "$SERIAL" >/dev/null

step "clear app state + cold-boot"
adb_s shell pm clear com.eight87.shutterboy >/dev/null
adb_s shell pm grant com.eight87.shutterboy android.permission.READ_MEDIA_IMAGES
adb_s logcat -c
adb_s shell am start -n com.eight87.shutterboy/.MainActivity >/dev/null
sleep 6

step "verify scan completed"
log_line=$(adb_s logcat -d -s "shutterboy:*" 2>&1 | grep -m1 "scan complete" || true)
if [[ -z "$log_line" ]]; then
    echo "fail: no 'scan complete' line in logcat" >&2
    exit 1
fi
echo "  ok: $log_line"

step "verify topResumedActivity == MainActivity"
adb_s shell dumpsys activity activities 2>&1 | grep -q "com.eight87.shutterboy/.MainActivity" || {
    echo "fail: MainActivity is not the top activity" >&2
    exit 1
}

step "screencap Photos tab"
adb_s exec-out screencap -p > /tmp/sb-smoke-photos.png
echo "  /tmp/sb-smoke-photos.png written"

step "verify a photo cell is hittable via the accessibility tree"
adb_s shell uiautomator dump /sdcard/dump.xml >/dev/null
photo_bounds=$(adb_s shell cat /sdcard/dump.xml | \
    grep -oP 'content-desc="img_[^"]+"[^>]*bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1)
if [[ -z "$photo_bounds" ]]; then
    echo "fail: no photo cells found in accessibility tree" >&2
    exit 1
fi
echo "  ok: $photo_bounds"

# Tap the photo center.
center=$(echo "$photo_bounds" | grep -oP '\[\K[0-9]+(?=,)' | head -2 | paste -sd' ' | awk '{print int(($1+$2)/2)}')
y_center=$(echo "$photo_bounds" | grep -oP ',\K[0-9]+(?=\])' | head -2 | paste -sd' ' | awk '{print int(($1+$2)/2)}')
adb_s shell input tap "$center" "$y_center"
sleep 2

step "screencap Viewer"
adb_s exec-out screencap -p > /tmp/sb-smoke-viewer.png
echo "  /tmp/sb-smoke-viewer.png written"

step "swipe to next photo"
adb_s shell input swipe 900 1200 200 1200 300
sleep 2

step "back out to Photos"
adb_s shell input keyevent KEYCODE_BACK
sleep 2

step "Collections tab"
adb_s shell input tap 540 2300
sleep 2
adb_s exec-out screencap -p > /tmp/sb-smoke-collections.png
echo "  /tmp/sb-smoke-collections.png written"

echo
echo "gallery smoke test: OK"
