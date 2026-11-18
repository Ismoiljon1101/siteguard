$ErrorActionPreference = 'Stop'
function Test-SiteGuardJdk21([string]$jdkHome) {
    if (-not $jdkHome) { return $false }
    $jdkHome = $jdkHome.Trim().TrimEnd('\', '/')
    if (-not (Test-Path -LiteralPath (Join-Path $jdkHome 'bin\java.exe'))) { return $false }
    $release = Join-Path $jdkHome 'release'
    if (-not (Test-Path -LiteralPath $release)) { return $false }
    return [bool](Select-String -LiteralPath $release -Pattern '^JAVA_VERSION="21[."]' -Quiet)
}

function Resolve-SiteGuardJdk21 {
    $candidates = @($env:SITEGUARD_JAVA_HOME, $env:JAVA_HOME)
    foreach ($base in @('C:\Program Files\Microsoft', 'C:\Program Files\Eclipse Adoptium', 'C:\Program Files\Java')) {
        if (Test-Path -LiteralPath $base) {
            $candidates += @(Get-ChildItem -LiteralPath $base -Directory -Filter 'jdk-21*' |
                Sort-Object Name -Descending | ForEach-Object { $_.FullName })
        }
    }
    foreach ($candidate in $candidates) {
        if (Test-SiteGuardJdk21 $candidate) { return $candidate.Trim().TrimEnd('\', '/') }
    }
    throw 'JDK 21 not found. Install JDK 21 or set SITEGUARD_JAVA_HOME to its folder.'
}
$jdk = Resolve-SiteGuardJdk21
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
$sourceRoot = Join-Path $PSScriptRoot 'src\main\java'
$mainOutput = Join-Path $PSScriptRoot 'target\offline-classes'
$testOutput = Join-Path $PSScriptRoot 'target\offline-tests'
$testSource = Join-Path $PSScriptRoot 'src\test\java\com\siteguard\bootstrap\SimulationSmoke.java'
$sourceFiles = @(Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
New-Item -ItemType Directory -Path $mainOutput, $testOutput -Force | Out-Null
& javac -encoding UTF-8 --release 21 -d $mainOutput $sourceFiles
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& javac -encoding UTF-8 --release 21 -cp $mainOutput -d $testOutput $testSource
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java '-Djava.awt.headless=true' -cp "$mainOutput;$testOutput" com.siteguard.bootstrap.SimulationSmoke
exit $LASTEXITCODE
