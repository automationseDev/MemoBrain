@echo off
setlocal
cd /d "%~dp0"

echo.
echo MemoBrain APK Builder v1.0.0
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Build-MemoBrainApk.ps1"
set ERR=%ERRORLEVEL%

echo.
if not "%ERR%"=="0" (
  echo Build failed. ErrorLevel=%ERR%
) else (
  echo Build completed.
)
echo.
pause
exit /b %ERR%
