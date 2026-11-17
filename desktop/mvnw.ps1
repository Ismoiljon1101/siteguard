# Bootstrap a pinned Maven distribution for Windows when Maven is not installed.
$ErrorActionPreference = 'Stop'
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
