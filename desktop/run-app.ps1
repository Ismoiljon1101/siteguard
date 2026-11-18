# Runs the SiteGuard shaded jar with JDK 21 (same JDK lookup as mvnw.ps1). Extra arguments go to the JVM.
# Example: .\run-app.ps1 -Dsiteguard.dark=true
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

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
$jar = Join-Path $PSScriptRoot 'target\siteguard-0.1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw "Jar not found. Run: .\mvnw.cmd clean verify  (looked for $jar)" }
& (Join-Path $jdk 'bin\java.exe') @args -jar $jar
exit $LASTEXITCODE
