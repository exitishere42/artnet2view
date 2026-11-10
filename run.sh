#!/usr/bin/env bash
set -e
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

JAR_FILE=$(ls -t target/artnet2rig-*-all.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "[!] Keine JAR in target/ gefunden. Starte Maven Build..."
    mvn clean package -DskipTests
    JAR_FILE=$(ls -t target/artnet2rig-*-all.jar 2>/dev/null | head -n 1)
fi

if [ -z "$JAR_FILE" ] || [ ! -f "$JAR_FILE" ]; then
    echo "[FEHLER] Build fehlgeschlagen." >&2
    exit 1
fi

echo "[*] Starte artnet2rig mit $JAR_FILE..."
exec java -jar "$JAR_FILE" "$@"
