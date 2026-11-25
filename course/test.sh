#!/bin/sh
# Compiles SiteGuard and runs the automatic tests (no JUnit needed).
set -e
rm -rf out && mkdir out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out siteguard.test.TestRunner
