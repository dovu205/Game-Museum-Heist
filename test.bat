@echo off
setlocal
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
if not exist build\tests mkdir build\tests
javac -encoding UTF-8 --release 17 -cp MuseumHeist.jar -d build\tests tests\*.java
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "MuseumHeist.jar;build\tests" nightchase.StoreTest
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "MuseumHeist.jar;build\tests" nightchase.EngineTest
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "MuseumHeist.jar;build\tests" nightchase.NetworkTest
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "MuseumHeist.jar;build\tests" nightchase.MultiClientTest
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "MuseumHeist.jar;build\tests" nightchase.ClientConfigTest
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "MuseumHeist.jar;build\tests" nightchase.PaginationTest
if errorlevel 1 exit /b 1
java -Dfile.encoding=UTF-8 -cp "MuseumHeist.jar;build\tests" nightchase.UiTest
if errorlevel 1 exit /b 1
echo ALL TESTS PASSED
