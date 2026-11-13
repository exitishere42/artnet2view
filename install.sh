#!/usr/bin/env bash
# ==============================================================================
# artnet2view - Automated Linux Installer
# Installs artnet2view into ~/artnet2view, checks/installs Java 21 LTS,
# and configures desktop menu entry and application icons.
# ==============================================================================

set -e

REPO="exitishere42/artnet2view"
INSTALL_DIR="${INSTALL_DIR:-$HOME/artnet2view}"
APP_NAME="artnet2view"
JAR_NAME="artnet2view-1.2.2-all.jar"
DETECTED_JAR=$(curl -fsSL "https://api.github.com/repos/$REPO/releases/latest" 2>/dev/null | grep -o '"name": *"artnet2view-[^"]*-all\.jar"' | head -n 1 | cut -d '"' -f 4 || true)
if [ -n "$DETECTED_JAR" ]; then
    JAR_NAME="$DETECTED_JAR"
fi
APPIMAGE_NAME="artnet2view-x86_64.AppImage"
RELEASE_BASE_URL="https://github.com/$REPO/releases/latest/download"
RAW_BASE_URL="https://raw.githubusercontent.com/$REPO/main"

echo "=================================================================="
echo "  artnet2view - Linux Installer"
echo "  Target directory: $INSTALL_DIR"
echo "=================================================================="

# ------------------------------------------------------------------------------
# 1. Check Java 21+ installation
# ------------------------------------------------------------------------------
check_java_version() {
    if command -v java &> /dev/null; then
        local ver
        ver=$(java -version 2>&1 | head -n 1 | sed -E 's/.*"([0-9]+).*/\1/')
        if [[ "$ver" =~ ^[0-9]+$ ]] && [ "$ver" -ge 21 ]; then
            return 0
        fi
    fi
    return 1
}

if check_java_version; then
    JAVA_VER_STR=$(java -version 2>&1 | head -n 1)
    echo "[OK] Compatible Java detected ($JAVA_VER_STR) -> skipping Java setup."
else
    echo "[*] Java 21+ not found. Installing OpenJDK 21 LTS..."
    if command -v apt-get &> /dev/null; then
        sudo apt-get update -y
        if apt-cache show openjdk-21-jre &> /dev/null; then
            sudo apt-get install -y openjdk-21-jre
        else
            echo "[*] Adding Eclipse Adoptium repository for OpenJDK 21..."
            sudo apt-get install -y wget apt-transport-https gpg
            wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
            echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list > /dev/null
            sudo apt-get update -y
            sudo apt-get install -y temurin-21-jre
        fi
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y java-21-openjdk
    elif command -v pacman &> /dev/null; then
        sudo pacman -Sy --noconfirm jre-openjdk
    elif command -v zypper &> /dev/null; then
        sudo zypper install -y java-21-openjdk
    else
        echo "[!] Unsupported package manager. Please install OpenJDK 21+ manually."
        exit 1
    fi
    echo "[OK] Java 21+ installed successfully."
fi

# ------------------------------------------------------------------------------
# 2. Download artifacts into target directory
# ------------------------------------------------------------------------------
echo "[*] Creating directory: $INSTALL_DIR"
mkdir -p "$INSTALL_DIR"

download_file() {
    local url="$1"
    local dest="$2"
    if command -v curl &> /dev/null; then
        curl -fL --progress-bar -o "$dest" "$url"
    elif command -v wget &> /dev/null; then
        wget -q --show-progress -O "$dest" "$url"
    else
        echo "[!] Neither curl nor wget is installed."
        exit 1
    fi
}

echo "[*] Downloading application files and icons..."
download_file "$RELEASE_BASE_URL/$JAR_NAME" "$INSTALL_DIR/$JAR_NAME"
download_file "$RELEASE_BASE_URL/$APPIMAGE_NAME" "$INSTALL_DIR/$APPIMAGE_NAME" || true
download_file "$RAW_BASE_URL/artnet2view.png" "$INSTALL_DIR/artnet2view.png"
download_file "$RAW_BASE_URL/artnet2view.svg" "$INSTALL_DIR/artnet2view.svg"
download_file "$RAW_BASE_URL/uninstall.sh" "$INSTALL_DIR/uninstall.sh" || true

if [ -f "$INSTALL_DIR/$APPIMAGE_NAME" ]; then
    chmod +x "$INSTALL_DIR/$APPIMAGE_NAME"
fi
if [ -f "$INSTALL_DIR/uninstall.sh" ]; then
    chmod +x "$INSTALL_DIR/uninstall.sh"
fi

# Generate launch script run.sh
cat > "$INSTALL_DIR/run.sh" << 'EOF'
#!/usr/bin/env bash
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
JAR_FILE=$(ls -t "$SCRIPT_DIR"/artnet2view-*-all.jar "$SCRIPT_DIR"/artnet2view.jar 2>/dev/null | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "[!] No artnet2view JAR found in $SCRIPT_DIR" >&2
    exit 1
fi
exec java -jar "$JAR_FILE" "$@"
EOF
chmod +x "$INSTALL_DIR/run.sh"

# Create ~/.local/bin symlink
mkdir -p "$HOME/.local/bin"
ln -sf "$INSTALL_DIR/run.sh" "$HOME/.local/bin/artnet2view"

# ------------------------------------------------------------------------------
# 3. Desktop Entry & Icons
# ------------------------------------------------------------------------------
echo "[*] Configuring Desktop Entry and Application Icons..."
ICON_DIR_PNG="$HOME/.local/share/icons/hicolor/256x256/apps"
ICON_DIR_SVG="$HOME/.local/share/icons/hicolor/scalable/apps"
APPS_DIR="$HOME/.local/share/applications"

mkdir -p "$ICON_DIR_PNG" "$ICON_DIR_SVG" "$APPS_DIR"

cp "$INSTALL_DIR/artnet2view.png" "$ICON_DIR_PNG/artnet2view.png"
cp "$INSTALL_DIR/artnet2view.svg" "$ICON_DIR_SVG/artnet2view.svg"

DESKTOP_FILE="$APPS_DIR/artnet2view.desktop"
cat > "$DESKTOP_FILE" << EOF
[Desktop Entry]
Version=1.0
Type=Application
Name=artnet2view
Comment=artnet2view - Interactive 3D Lighting Rig & Stage Visualizer
Exec=$INSTALL_DIR/run.sh
Icon=$INSTALL_DIR/artnet2view.png
Path=$INSTALL_DIR
Terminal=false
Categories=AudioVideo;Utility;
StartupNotify=true
StartupWMClass=de.exit.artnet2view.Artnet2viewApplication
EOF
chmod +x "$DESKTOP_FILE"
cp "$DESKTOP_FILE" "$INSTALL_DIR/artnet2view.desktop"

if command -v update-desktop-database &> /dev/null; then
    update-desktop-database "$APPS_DIR" &> /dev/null || true
fi
if command -v gtk-update-icon-cache &> /dev/null; then
    gtk-update-icon-cache -f -t "$HOME/.local/share/icons/hicolor" &> /dev/null || true
fi

echo "=================================================================="
echo "  [OK] Installation complete!"
echo "  Directory:     $INSTALL_DIR"
echo "  Run command:   artnet2view (or $INSTALL_DIR/run.sh)"
echo "  AppImage:      $INSTALL_DIR/$APPIMAGE_NAME"
echo "  Desktop Menu:  artnet2view is now available in your app launcher."
echo "=================================================================="
