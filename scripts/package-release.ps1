param([switch]$IncludeSource)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$properties = Get-Content (Join-Path $repoRoot 'gradle.properties')
$version = (($properties | Where-Object { $_ -match '^mod_version=' }) -replace '^mod_version=', '').Trim()
if ($version -notmatch '^[A-Za-z0-9.+_-]+$') { throw 'Invalid release version' }
$jarName = "thaumcraft-$version.jar"
$jarPath = Join-Path $repoRoot "build/libs/$jarName"
if (!(Test-Path -LiteralPath $jarPath)) { throw 'Build the ordinary production JAR first' }
# Independently reject development output before preparing distributable files.
$zip = [IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $bad = $zip.Entries | Where-Object {
        $_.FullName -match '(^|/)\.cache/|/DebugAcceptance|^thaumcraft/common/lib/events/Kt|/DebugStabilityAudit\.class$'
    }
    if ($bad) { throw 'Acceptance code or generator cache is present in JAR' }
    foreach ($name in @('META-INF/neoforge.mods.toml','LICENSE.md','NOTICE.md')) {
        if (!$zip.GetEntry($name)) { throw "Missing JAR entry: $name" }
    }
} finally { $zip.Dispose() }
$output = Join-Path $repoRoot "build/publication/$version"
New-Item -ItemType Directory -Force $output | Out-Null
Copy-Item -LiteralPath $jarPath -Destination $output
$documents = @('README.md','CHANGELOG.md','KNOWN_ISSUES.md','LICENSE.md','NOTICE.md',
    'TEMPLATE_LICENSE.txt','RELEASE_NOTES.md','RELEASE_VALIDATION.md','PUBLICATION_CHECKLIST.md')
foreach ($name in $documents) { Copy-Item -LiteralPath (Join-Path $repoRoot $name) -Destination $output }
$commit = (& git -C $repoRoot rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0) { throw 'Cannot determine source commit' }
$manifest = [ordered]@{
    version = $version
    commit = $commit
    minecraft = '1.21.1'
    neoforge = '21.1.252'
    java = 21
    required = @('curios 9.5.1+1.21.1','terrablender 4.1.0.8')
    bundledDependencies = $false
    permissionToPublishConfirmedByScript = $false
    jar = $jarName
    sha256 = (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.ToLowerInvariant()
}
$utf8 = [Text.UTF8Encoding]::new($false)
[IO.File]::WriteAllText((Join-Path $output 'RELEASE_MANIFEST.json'), ($manifest | ConvertTo-Json -Depth 4) + "`n", $utf8)
$bundleName = "thaumcraft-$version-release.zip"
$bundleFiles = @($jarName) + $documents + @('RELEASE_MANIFEST.json') | ForEach-Object { Join-Path $output $_ }
Compress-Archive -LiteralPath $bundleFiles -DestinationPath (Join-Path $output $bundleName) -Force
if ($IncludeSource) {
    $dirty = & git -C $repoRoot status --porcelain --untracked-files=normal
    if ($dirty) { throw 'Commit the intended source tree before creating the source archive' }
    $sourcePath = Join-Path $output "thaumcraft-$version-source.zip"
    & git -C $repoRoot archive --format=zip "--output=$sourcePath" HEAD
    if ($LASTEXITCODE -ne 0) { throw 'Source archive creation failed' }
}
$lines = Get-ChildItem -LiteralPath $output -File | Where-Object Name -ne 'SHA256SUMS.txt' |
    Sort-Object Name | ForEach-Object {
        (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() + '  ' + $_.Name
    }
[IO.File]::WriteAllText((Join-Path $output 'SHA256SUMS.txt'), ($lines -join "`n") + "`n", $utf8)
Write-Output $output
Write-Output "Prepared $jarName and release archives; nothing uploaded or published."
