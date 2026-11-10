@echo off
setlocal
echo ==================================================================
echo   artnet2rig - Windows Uninstaller
echo ==================================================================

set "INSTALL_DIR=%USERPROFILE%\artnet2rig"
set "DESKTOP_LNK=%USERPROFILE%\Desktop\artnet2rig.lnk"

if exist "%DESKTOP_LNK%" del /f /q "%DESKTOP_LNK%"
if exist "%INSTALL_DIR%" rmdir /s /q "%INSTALL_DIR%"

echo [OK] artnet2rig wurde erfolgreich deinstalliert.
pause
