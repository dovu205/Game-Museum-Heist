@echo off
setlocal
cd /d "%~dp0"
where javaw >nul 2>nul
if errorlevel 1 (
 echo Can cai Java 17 hoac 21 va them Java vao PATH.
 pause
 exit /b 1
)
if not exist MuseumHeist.jar (
 echo Khong tim thay MuseumHeist.jar. Hay giai nen toan bo bo game.
 pause
 exit /b 1
)
call "%~dp0May2-ThamGia.bat"

