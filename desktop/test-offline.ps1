$ErrorActionPreference = 'Stop'
$sourceRoot = Join-Path $PSScriptRoot 'src\main\java'
$mainOutput = Join-Path $PSScriptRoot 'target\offline-classes'
$testOutput = Join-Path $PSScriptRoot 'target\offline-tests'
$testSource = Join-Path $PSScriptRoot 'src\test\java\com\siteguard\bootstrap\SimulationSmoke.java'
$sourceFiles = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
New-Item -ItemType Directory -Path $mainOutput, $testOutput -Force | Out-Null
& javac -encoding UTF-8 --release 17 -d $mainOutput $sourceFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& javac -encoding UTF-8 --release 17 -cp $mainOutput -d $testOutput $testSource
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java '-Djava.awt.headless=true' -cp "$mainOutput;$testOutput" com.siteguard.bootstrap.SimulationSmoke
exit $LASTEXITCODE
