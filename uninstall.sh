#!/usr/bin/env bash
set -e

echo "=== artnet2view Linux Uninstaller ==="

rm -rf "$HOME/.local/share/artnet2view"
rm -f "$HOME/.local/bin/artnet2view"
rm -f "$HOME/.local/share/applications/artnet2view.desktop"
rm -f "$HOME/.local/share/icons/hicolor/256x256/apps/artnet2view.png"

echo "[OK] artnet2view wurde erfolgreich entfernt."
