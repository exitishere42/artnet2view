@echo off
setlocal
cd /d "%~dp0"

for /f "delims=" %%i in ('dir /b /o-d target\artnet2view-*-all.jar 2^>nul') do (
    set "JAR_FILE=target\%%i"
    goto :found
)

echo [!] Keine JAR in target/ gefunden. Starte Maven Build...
call mvn clean package -DskipTests
for /f "delims=" %%i in ('dir /b /o-d target\artnet2view-*-all.jar 2^>nul') do (
    set "JAR_FILE=target\%%i"
    goto :found
)

echo [FEHLER] Build fehlgeschlagen.
pause
exit /b 1

:found
echo [*] Starte artnet2view mit %JAR_FILE%...
start javaw -jar "%JAR_FILE%" %*
exit /b 0
