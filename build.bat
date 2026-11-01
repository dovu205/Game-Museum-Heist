@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>nul
if errorlevel 1 (
 echo Can JDK 17 tro len. Cai JDK va them thu muc bin vao PATH.
 pause
 exit /b 1
)
if not exist build\app mkdir build\app
if not exist lib\sqlite-jdbc-3.53.4.0.jar (
 echo Thieu lib\sqlite-jdbc-3.53.4.0.jar. Hay giai nen day du bo game.
 exit /b 1
)
javac -encoding UTF-8 --release 17 -d build\app src\nightchase\*.java
if errorlevel 1 (
 pause
 exit /b 1
)
>build\MANIFEST.MF echo Class-Path: lib/sqlite-jdbc-3.53.4.0.jar
jar "-J-Djava.io.tmpdir=%cd%\build" --create --file MuseumHeist.jar --main-class nightchase.Launcher --manifest build\MANIFEST.MF -C build\app nightchase -C assets .
if errorlevel 1 (
 pause
 exit /b 1
)
echo Build thanh cong: MuseumHeist.jar


