@echo off
setlocal
cd /d "%~dp0"

echo.
echo MemoBrain Signed Release APK Builder v1.0.0
echo.

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Build-MemoBrainRelease.ps1"
set ERR=%ERRORLEVEL%

echo.
if not "%ERR%"=="0" (
  echo Release build failed. ErrorLevel=%ERR%
) else (
  echo Release build completed.
)
echo.
pause
exit /b %ERR%
