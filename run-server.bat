@echo off
cd /d "%~dp0"
if not exist build\police-thief-server.jar call build.bat
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "build/police-thief-server.jar;lib/*" vn.edu.policethief.server.ServerMain %*
