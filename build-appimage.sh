#!/usr/bin/env bash
set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="/tmp/artnet2view-appimage"
JAR_SRC=$(ls -t "$PROJECT_DIR"/target/artnet2view-*-all.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_SRC" ] || [ ! -f "$JAR_SRC" ]; then
    echo "[!] Fat JAR nicht gefunden in target/. Bitte zuerst 'mvn package' ausführen."
    exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/AppDir/usr/lib/artnet2view"
mkdir -p "$BUILD_DIR/AppDir/usr/share/icons/hicolor/256x256/apps"
mkdir -p "$BUILD_DIR/AppDir/usr/share/applications"

cp "$JAR_SRC" "$BUILD_DIR/AppDir/usr/lib/artnet2view/"
cp "$PROJECT_DIR/artnet2view.png" "$BUILD_DIR/AppDir/artnet2view.png"
cp "$PROJECT_DIR/artnet2view.png" "$BUILD_DIR/AppDir/.DirIcon"
cp "$PROJECT_DIR/artnet2view.png" "$BUILD_DIR/AppDir/usr/share/icons/hicolor/256x256/apps/artnet2view.png"
cp "$PROJECT_DIR/artnet2view.svg" "$BUILD_DIR/AppDir/artnet2view.svg"

cat > "$BUILD_DIR/AppDir/artnet2view.desktop" << 'EOF'
[Desktop Entry]
Type=Application
Name=artnet2view
Comment=artnet2view - Interactive 3D Lighting Rig & Stage Visualizer
Exec=AppRun
Icon=artnet2view
Categories=AudioVideo;Utility;
Terminal=false
StartupNotify=true
StartupWMClass=de.exit.artnet2view.Artnet2viewApplication
EOF
cp "$BUILD_DIR/AppDir/artnet2view.desktop" "$BUILD_DIR/AppDir/usr/share/applications/artnet2view.desktop"

cat > "$BUILD_DIR/AppDir/AppRun" << 'EOF'
#!/usr/bin/env bash
HERE="$(dirname "$(readlink -f "${0}")")"
JAR_FILE=$(ls -t "$HERE"/usr/lib/artnet2view/artnet2view-*-all.jar 2>/dev/null | head -n 1)
if ! command -v java &> /dev/null; then
    echo "[!] Fehler: Java 21+ (java) wurde nicht gefunden. Bitte openjdk-21-jre installieren." >&2
    exit 1
fi
exec java -jar "$JAR_FILE" "$@"
EOF
chmod +x "$BUILD_DIR/AppDir/AppRun"

APPIMAGETOOL="$PROJECT_DIR/appimagetool-x86_64.AppImage"
if [ ! -f "$APPIMAGETOOL" ]; then
    echo "[*] Lade appimagetool herunter..."
    curl -sSL -o "$APPIMAGETOOL" "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-x86_64.AppImage"
    chmod +x "$APPIMAGETOOL"
fi

OUTPUT_APPIMAGE="$PROJECT_DIR/artnet2view-x86_64.AppImage"
ARCH=x86_64 "$APPIMAGETOOL" "$BUILD_DIR/AppDir" "$OUTPUT_APPIMAGE"
echo "[+] AppImage erfolgreich erstellt: $OUTPUT_APPIMAGE"
