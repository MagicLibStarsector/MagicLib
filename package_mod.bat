@echo off
REM Builds the mod jar and packages it into the release zip.
"%~dp0gradlew.bat" packageMod
pause
