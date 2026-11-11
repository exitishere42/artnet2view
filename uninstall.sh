#!/usr/bin/env bash
# ==============================================================================
# artnet2view - Automated Linux Uninstaller
# Removes artnet2view, ~/artnet2view, symlinks, desktop entry, and icons.
# ==============================================================================

set -e

INSTALL_DIR="${INSTALL_DIR:-$HOME/artnet2view}"
APPS_DIR="$HOME/.local/share/applications"
ICON_DIR_PNG="$HOME/.local/share/icons/hicolor/256x256/apps"
ICON_DIR_SVG="$HOME/.local/share/icons/hicolor/scalable/apps"

echo "=================================================================="
echo "  artnet2view - Linux Uninstaller"
echo "=================================================================="

if [ -L "$HOME/.local/bin/artnet2view" ]; then
    echo "[*] Removing symlink ~/.local/bin/artnet2view..."
    rm -f "$HOME/.local/bin/artnet2view"
fi

if [ -f "$APPS_DIR/artnet2view.desktop" ]; then
    echo "[*] Removing desktop entry..."
    rm -f "$APPS_DIR/artnet2view.desktop"
fi

if [ -f "$ICON_DIR_PNG/artnet2view.png" ]; then
    echo "[*] Removing PNG icon..."
    rm -f "$ICON_DIR_PNG/artnet2view.png"
fi

if [ -f "$ICON_DIR_SVG/artnet2view.svg" ]; then
    echo "[*] Removing SVG icon..."
    rm -f "$ICON_DIR_SVG/artnet2view.svg"
fi

if command -v update-desktop-database &> /dev/null; then
    update-desktop-database "$APPS_DIR" &> /dev/null || true
fi
if command -v gtk-update-icon-cache &> /dev/null; then
    gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" &> /dev/null || true
fi

if [ -d "$INSTALL_DIR" ]; then
    echo "[*] Removing installation folder: $INSTALL_DIR"
    rm -rf "$INSTALL_DIR"
else
    echo "[i] Installation folder $INSTALL_DIR already removed."
fi

echo "=================================================================="
echo "  [OK] artnet2view has been completely uninstalled!"
echo "=================================================================="
