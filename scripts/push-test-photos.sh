#!/usr/bin/env bash
#
# L.2 — push the fetched test photos onto a connected AVD/device + kick
# MediaStore so they appear in the scan. Pass --serial <id> to target a
# specific device when multiple are connected.
set -euo pipefail

SERIAL=""
if [[ "${1:-}" == "--serial" ]]; then
    SERIAL="-s $2"
    shift 2
fi

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="${REPO_ROOT}/test-photos"

if [[ ! -d "$SRC" ]] || [[ -z "$(ls -A "$SRC"/*.jpg 2>/dev/null)" ]]; then
    echo "no test photos at $SRC — run scripts/fetch-test-photos.sh first" >&2
    exit 1
fi

# shellcheck disable=SC2086
adb $SERIAL shell mkdir -p /sdcard/DCIM/shutterboy-test/

for f in "$SRC"/*.jpg; do
    bn="$(basename "$f")"
    # shellcheck disable=SC2086
    adb $SERIAL push "$f" "/sdcard/DCIM/shutterboy-test/$bn" >/dev/null
    echo "pushed $bn"
done

echo "kicking MediaStore scan..."
# shellcheck disable=SC2086
adb $SERIAL shell content call --uri content://media --method scan_volume \
    --arg external_primary >/dev/null

# Best-effort permission grant for the shutterboy package (no-op if not
# installed yet). Manifest declares READ_MEDIA_IMAGES so this lands.
# shellcheck disable=SC2086
adb $SERIAL shell pm grant com.eight87.shutterboy \
    android.permission.READ_MEDIA_IMAGES 2>/dev/null || true

# shellcheck disable=SC2086
count=$(adb $SERIAL shell content query \
    --uri content://media/external/images/media --projection _id \
    | grep -c '^Row' || true)
echo "MediaStore now sees $count image rows"
