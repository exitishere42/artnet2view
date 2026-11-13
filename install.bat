@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

set "REPO=exitishere42/artnet2view"
set "INSTALL_DIR=%USERPROFILE%\artnet2view"
set "JAR_NAME=artnet2view-1.3.0-all.jar"
set "RELEASE_URL=https://github.com/%REPO%/releases/latest/download"
set "RAW_URL=https://raw.githubusercontent.com/%REPO%/main"

echo ==================================================================
echo   artnet2view - Windows Installer
echo   Target directory: %INSTALL_DIR%
echo ==================================================================

:: 1. Check Java 21+
set "JAVA_OK=0"
where java >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%v in ('powershell -NoProfile -Command "$v = (java -version 2>&1 | Select-Object -First 1); if ($v -match '\"(\d+)') { [int]$Matches[1] } else { 0 }"') do (
        if %%v GEQ 21 set "JAVA_OK=1"
    )
)

if "%JAVA_OK%"=="1" (
    echo [OK] Java 21+ is already installed.
) else (
    echo [*] Java 21+ not found. Installing OpenJDK 21 LTS...
    where winget >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        winget install --id EclipseAdoptium.Temurin.21.JRE -e --accept-source-agreements --accept-package-agreements
    ) else (
        echo [*] Downloading OpenJDK 21 MSI from Adoptium...
        powershell -NoProfile -Command "Invoke-WebRequest -Uri 'https://api.adoptium.net/v3/installer/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse' -OutFile '$env:TEMP\temurin21.msi'; Start-Process msiexec.exe -ArgumentList '/i', '$env:TEMP\temurin21.msi', '/passive', '/norestart' -Wait"
    )
    echo [OK] Java 21+ installation finished.
)

:: 2. Download files into %USERPROFILE%\artnet2view
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

echo [*] Downloading artnet2view and assets...
powershell -NoProfile -Command ^
    "$ProgressPreference = 'SilentlyContinue'; " ^
    "Invoke-WebRequest -Uri '%RELEASE_URL%/%JAR_NAME%' -OutFile '%INSTALL_DIR%\%JAR_NAME%'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/artnet2view.ico' -OutFile '%INSTALL_DIR%\artnet2view.ico'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/artnet2view.png' -OutFile '%INSTALL_DIR%\artnet2view.png'; " ^
    "Invoke-WebRequest -Uri '%RAW_URL%/uninstall.bat' -OutFile '%INSTALL_DIR%\uninstall.bat'"

if not exist "%INSTALL_DIR%\%JAR_NAME%" (
    echo [!] Failed to download %JAR_NAME%.
    pause
    exit /b 1
)

:: Create launch script
(
echo @echo off
echo cd /d "%%~dp0"
echo for /f "delims=" %%%%f in ^('dir /b /o:-d "%%~dp0artnet2view-*-all.jar" "%%~dp0artnet2view.jar" 2^^^>nul'^) do ^(
echo     start "" javaw -jar "%%~dp0%%%%f" %%*
echo     exit /b 0
echo ^)
echo start "" javaw -jar "%%~dp0%JAR_NAME%" %%*
) > "%INSTALL_DIR%\run.bat"

:: 3. Create Start Menu and Desktop shortcuts
echo [*] Creating Start Menu and Desktop shortcuts...
powershell -NoProfile -Command ^
    "$ws = New-Object -ComObject WScript.Shell; " ^
    "$javaw = (Get-Command javaw -ErrorAction SilentlyContinue).Source; " ^
    "if (-not $javaw) { $javaw = 'javaw.exe' }; " ^
    "$smPath = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\artnet2view.lnk'; " ^
    "$dtPath = Join-Path ([Environment]::GetFolderPath('Desktop')) 'artnet2view.lnk'; " ^
    "foreach ($lnkPath in @($smPath, $dtPath)) { " ^
    "  $s = $ws.CreateShortcut($lnkPath); " ^
    "  $s.TargetPath = $javaw; " ^
    "  $s.Arguments = '-jar \"' + '%INSTALL_DIR%\%JAR_NAME%' + '\"'; " ^
    "  $s.WorkingDirectory = '%INSTALL_DIR%'; " ^
    "  $s.IconLocation = '%INSTALL_DIR%\artnet2view.ico,0'; " ^
    "  $s.Description = 'artnet2view - 3D Lighting Rig & Stage Visualizer'; " ^
    "  $s.Save(); " ^
    "}"

echo ==================================================================
echo   [OK] Installation complete!
echo   Directory:   %INSTALL_DIR%
echo   Shortcuts:   artnet2view (Start Menu & Desktop)
echo ==================================================================
pause
