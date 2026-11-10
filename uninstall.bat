@echo off
setlocal
echo ==================================================================
echo   artnet2view - Windows Uninstaller
echo ==================================================================

set "INSTALL_DIR=%USERPROFILE%\artnet2view"
set "DESKTOP_LNK=%USERPROFILE%\Desktop\artnet2view.lnk"

if exist "%DESKTOP_LNK%" del /f /q "%DESKTOP_LNK%"
if exist "%INSTALL_DIR%" rmdir /s /q "%INSTALL_DIR%"

echo [OK] artnet2view wurde erfolgreich deinstalliert.
pause
