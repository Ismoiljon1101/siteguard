@echo off
rem Compiles SiteGuard and runs the automatic tests (no JUnit needed).
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 goto end
java -cp out siteguard.test.TestRunner
:end
del sources.txt
