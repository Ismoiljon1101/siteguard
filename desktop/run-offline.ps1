# Compile and launch without Maven. SQLite persistence is unavailable unless its driver is on the classpath.
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$source = Join-Path $root 'src\main\java'
$output = Join-Path $root 'target\offline-classes'
$files = @(Get-ChildItem -LiteralPath $source -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
if ($files.Count -eq 0) { throw 'No Java source files found.' }
New-Item -ItemType Directory -Path $output -Force | Out-Null
& javac -encoding UTF-8 --release 17 -d $output $files
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $output com.siteguard.bootstrap.SiteGuardApp
exit $LASTEXITCODE
