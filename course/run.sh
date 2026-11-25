#!/bin/sh
# Compiles and starts SiteGuard. Run this from the "course" folder.
set -e
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp "out:lib/*" siteguard.Main
