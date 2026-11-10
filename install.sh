#!/usr/bin/env bash
set -e

REPO="exitishere42/artnet2rig"
INSTALL_DIR="$HOME/.local/share/artnet2rig"
BIN_DIR="$HOME/.local/bin"
DESKTOP_DIR="$HOME/.local/share/applications"
ICON_DIR="$HOME/.local/share/icons/hicolor/256x256/apps"
APPIMAGE_NAME="artnet2rig-x86_64.AppImage"
RELEASE_URL="https://github.com/$REPO/releases/latest/download"
RAW_URL="https://raw.githubusercontent.com/$REPO/main"

echo "=== artnet2rig Linux Installer ==="

if ! command -v java &> /dev/null; then
    echo "[!] Java 21+ nicht gefunden. Bitte installiere openjdk-21-jre."
fi

mkdir -p "$INSTALL_DIR" "$BIN_DIR" "$DESKTOP_DIR" "$ICON_DIR"

echo "[*] Lade artnet2rig herunter..."
curl -sSL -o "$INSTALL_DIR/$APPIMAGE_NAME" "$RELEASE_URL/$APPIMAGE_NAME"
chmod +x "$INSTALL_DIR/$APPIMAGE_NAME"

curl -sSL -o "$ICON_DIR/artnet2rig.png" "$RAW_URL/artnet2rig.png"
curl -sSL -o "$INSTALL_DIR/uninstall.sh" "$RAW_URL/uninstall.sh"
chmod +x "$INSTALL_DIR/uninstall.sh"

ln -sf "$INSTALL_DIR/$APPIMAGE_NAME" "$BIN_DIR/artnet2rig"

cat > "$DESKTOP_DIR/artnet2rig.desktop" << EOF
[Desktop Entry]
Type=Application
Name=artnet2rig
Comment=artnet2rig - Interactive 3D Lighting Rig & Stage Visualizer
Exec=$INSTALL_DIR/$APPIMAGE_NAME
Icon=artnet2rig
Categories=AudioVideo;Utility;
Terminal=false
StartupNotify=true
StartupWMClass=de.exit.artnet2rig.Artnet2rigApplication
EOF

echo "[OK] artnet2rig erfolgreich installiert!"
