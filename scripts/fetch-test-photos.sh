#!/usr/bin/env bash
#
# L.1 — fetch a small set of CC-licensed sample photos for AVD verification.
#
# Source: picsum.photos (Lorem Picsum) — public-domain placeholder photos.
# Output: ./test-photos/img_<seed>.jpg, EXIF-backdated across 4 years so
# the year scrubber + month sections exercise.
#
# Re-runnable: existing files are re-tagged but not re-downloaded unless
# `--force` is passed.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${REPO_ROOT}/test-photos"
SEEDS=(10 27 42 99 137 200 256 318 405 477 555 612 700 821 900)
FORCE=0
[[ "${1:-}" == "--force" ]] && FORCE=1

mkdir -p "$OUT"
cd "$OUT"

for i in "${!SEEDS[@]}"; do
    seed="${SEEDS[$i]}"
    f="img_${seed}.jpg"
    if [[ "$FORCE" == "1" || ! -f "$f" ]]; then
        echo "fetching $f (seed=$seed)..."
        curl -sL "https://picsum.photos/seed/${seed}/1200/800" -o "$f"
    fi
done

# Backdate EXIF across 2023..2026 so the year-scrubber + month bands
# exercise. 5 photos per year, distributed across months.
echo "backdating EXIF..."
i=0
for seed in "${SEEDS[@]}"; do
    f="img_${seed}.jpg"
    years_ago=$((i / 5))
    month=$(( (i % 12) + 1 ))
    day=$(( ((i * 7) % 28) + 1 ))
    year=$((2026 - years_ago))
    stamp="${year}:$(printf %02d $month):$(printf %02d $day) 12:00:00"
    exiftool -overwrite_original \
        -DateTimeOriginal="$stamp" \
        -CreateDate="$stamp" \
        "$f" >/dev/null
    i=$((i + 1))
done

echo
echo "fetched ${#SEEDS[@]} photos into $OUT"
echo "year spread:"
exiftool -DateTimeOriginal -T -fileOrder filename *.jpg | awk '{print $1}' | cut -d: -f1 | sort | uniq -c
