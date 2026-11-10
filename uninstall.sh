#!/usr/bin/env bash
set -e

echo "=== artnet2rig Linux Uninstaller ==="

rm -rf "$HOME/.local/share/artnet2rig"
rm -f "$HOME/.local/bin/artnet2rig"
rm -f "$HOME/.local/share/applications/artnet2rig.desktop"
rm -f "$HOME/.local/share/icons/hicolor/256x256/apps/artnet2rig.png"

echo "[OK] artnet2rig wurde erfolgreich entfernt."
