@echo off
rem Compiles and starts SiteGuard. Run this from the "course" folder.
rem Optional: put sqlite-jdbc-<version>.jar in the lib folder to store data in SQLite.
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 goto end
java -cp "out;lib/*" siteguard.Main
:end
del sources.txt
