# Compile and launch without Maven. SQLite persistence is unavailable unless its driver is on the classpath.
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
$root = $PSScriptRoot
$source = Join-Path $root 'src\main\java'
$output = Join-Path $root 'target\offline-classes'
$files = @(Get-ChildItem -LiteralPath $source -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
if ($files.Count -eq 0) { throw 'No Java source files found.' }
New-Item -ItemType Directory -Path $output -Force | Out-Null
& javac -encoding UTF-8 --release 21 -d $output $files
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $output com.siteguard.bootstrap.SiteGuardApp
exit $LASTEXITCODE
