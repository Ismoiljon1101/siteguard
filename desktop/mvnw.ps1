# Bootstrap a pinned Maven distribution for Windows when Maven is not installed.
# Uses JDK 21 (decision D1). Set SITEGUARD_JAVA_HOME to pick a specific JDK 21.
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
$version = '3.9.9'
$base = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$version"
$archive = "apache-maven-$version-bin.zip"
$cache = Join-Path $PSScriptRoot ".maven-cache\maven\$version"
$maven = Join-Path $cache "apache-maven-$version\bin\mvn.cmd"
if (-not (Test-Path -LiteralPath $maven)) {
    New-Item -ItemType Directory -Path $cache -Force | Out-Null
    $zip = Join-Path $cache $archive
    Invoke-WebRequest -UseBasicParsing -Uri "$base/$archive" -OutFile $zip
    $checksum = (Invoke-WebRequest -UseBasicParsing -Uri "$base/$archive.sha512").Content.Trim().Split(' ')[0]
    $hasher = [System.Security.Cryptography.SHA512]::Create()
    $stream = [System.IO.File]::OpenRead($zip)
    try { $digest = [System.BitConverter]::ToString($hasher.ComputeHash($stream)).Replace('-', '') }
    finally { $stream.Dispose(); $hasher.Dispose() }
    if ($digest -ne $checksum) { throw 'Maven archive checksum mismatch.' }
    Expand-Archive -LiteralPath $zip -DestinationPath $cache -Force
}
$repository = Join-Path $PSScriptRoot '.maven-cache\repository'
& $maven "-Dmaven.repo.local=$repository" @args
exit $LASTEXITCODE
