#!/usr/bin/env bash
set -euo pipefail

echo "[PRE-INSTALL] PWD=$(pwd)"
echo "[PRE-INSTALL] === sources/assets/images (from archive) ==="
ls sources/assets/images/ 2>&1 | head -20 || echo "[PRE-INSTALL] DIRECTORY MISSING - will download"

IMAGES_DIR="sources/assets/images"
BASE_URL="https://raw.githubusercontent.com/nastech-ai/NasTech-Agent/main/NasTech/sources/assets/images"

mkdir -p "$IMAGES_DIR"

for img in icon.png icon-adaptive.png icon-monochrome.png icon-notification.png \
           favicon.png splash-android-light.png splash-android-dark.png \
           icon-openclaw.png logotype.png transparent.png; do
  if [ ! -f "$IMAGES_DIR/$img" ]; then
    echo "[PRE-INSTALL] Downloading missing: $img"
    curl -fsSL -o "$IMAGES_DIR/$img" "$BASE_URL/$img" && \
      echo "[PRE-INSTALL] Downloaded: $img ($(wc -c < "$IMAGES_DIR/$img") bytes)" || \
      echo "[PRE-INSTALL] WARNING: Failed to download $img"
  else
    echo "[PRE-INSTALL] Present: $img ($(wc -c < "$IMAGES_DIR/$img") bytes)"
  fi
done

echo "[PRE-INSTALL] === Final images dir ==="
ls -la "$IMAGES_DIR"/*.png 2>&1 | head -20 || echo "[PRE-INSTALL] No PNGs found"
