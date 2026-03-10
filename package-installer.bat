@echo off
setlocal

cd /d "%~dp0"

call ".\build-release.bat"
if errorlevel 1 goto :fail

echo.
echo [OK] Release packaging finished.
echo [OK] Output folder: dist\release
echo [OK] Public exe: dist\release\*.exe
echo [OK] Portable zip: dist\release\*-portable.zip
goto :eof

:fail
echo.
echo [ERROR] Packaging failed.
exit /b 1
