@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "REPO=exitishere42/artnet2view"
set "INSTALL_DIR=%USERPROFILE%\artnet2view"
set "JAR_NAME=artnet2view-1.0.0-all.jar"
set "RELEASE_URL=https://github.com/%REPO%/releases/latest/download"
set "RAW_URL=https://raw.githubusercontent.com/%REPO%/main"

echo ==================================================================
echo   artnet2view - Windows Installer
echo   Zielordner: %INSTALL_DIR%
echo ==================================================================

set "JAVA_OK=0"
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%v in ('powershell -NoProfile -Command "$v = (java -version 2>&1 | Select-Object -First 1); if ($v -match '\"(\d+)') { [int]$Matches[1] } else { 0 }"') do (
        if %%v GEQ 21 set "JAVA_OK=1"
    )
)

if "%JAVA_OK%"=="1" (
    echo [OK] Java 21+ ist bereits installiert.
) else (
    echo [*] Java 21+ nicht gefunden. Installiere OpenJDK 21 LTS...
    where winget >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        winget install --id EclipseAdoptium.Temurin.21.JRE -e --accept-source-agreements --accept-package-agreements
    ) else (
        powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse' -OutFile '$env:TEMP\temurin21.msi'; Start-Process msiexec.exe -ArgumentList '/i', '$env:TEMP\temurin21.msi', '/passive', '/norestart' -Wait"
    )
    echo [OK] Java 21+ Installation abgeschlossen.
)

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

echo [*] Lade Programmdateien herunter...
powershell -NoProfile -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "Invoke-WebRequest -Uri '%RELEASE_URL%/%JAR_NAME%' -OutFile '%INSTALL_DIR%\%JAR_NAME%'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/artnet2view.ico' -OutFile '%INSTALL_DIR%\artnet2view.ico'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/uninstall.bat' -OutFile '%INSTALL_DIR%\uninstall.bat'"

(
    echo @echo off
    echo cd /d "%INSTALL_DIR%"
    echo start javaw -jar "%INSTALL_DIR%\%JAR_NAME%" %%*
) > "%INSTALL_DIR%\artnet2view.bat"

powershell -NoProfile -Command ^
    "$ws = New-Object -ComObject WScript.Shell; " ^
    "$s = $ws.CreateShortcut([Environment]::GetFolderPath('Desktop') + '\artnet2view.lnk'); " ^
    "$s.TargetPath = '%INSTALL_DIR%\artnet2view.bat'; " ^
    "$s.WorkingDirectory = '%INSTALL_DIR%'; " ^
    "$s.IconLocation = '%INSTALL_DIR%\artnet2view.ico,0'; " ^
    "$s.Description = 'artnet2view - 3D Lighting Rig & Stage Visualizer'; " ^
    "$s.Save()"

echo [OK] Installation erfolgreich abgeschlossen!
pause
