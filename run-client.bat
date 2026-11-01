@echo off
cd /d "%~dp0"
if not exist build\police-thief-client.jar call build.bat
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -jar build/police-thief-client.jar %*
