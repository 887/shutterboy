#!/usr/bin/env bash
#
# L.4 — UI smoke. Tab through Photos / Collections / Settings + cap each.
# Assumes the gallery-smoke-test has already pushed photos + granted the
# permission, or that the app already has state.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${SERIAL:-emulator-5556}"
if [[ "${1:-}" == "--serial" ]]; then
    SERIAL="$2"; shift 2
fi
adb_s() { adb -s "$SERIAL" "$@"; }

adb_s shell am start -n com.eight87.shutterboy/.MainActivity >/dev/null
sleep 3

# Bottom-nav tap targets in device coords (1080-wide screen).
# Photos: x≈180  Collections: x≈540  Settings: x≈900
# Nav-bar Y ≈ 2300 on a 2400-tall screen.
declare -a TABS=( "Photos:180:2300" "Collections:540:2300" "Settings:900:2300" )

for spec in "${TABS[@]}"; do
    IFS=":" read -r name x y <<<"$spec"
    echo "[step] $name tab"
    adb_s shell input tap "$x" "$y"
    sleep 2
    adb_s exec-out screencap -p > "/tmp/sb-smoke-$(echo "$name" | tr '[:upper:]' '[:lower:]').png"
done

echo "ui smoke test: OK"
