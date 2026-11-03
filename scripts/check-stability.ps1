param([ValidateSet('vanilla','botania')][string]$Profile = 'botania', [ValidateRange(1200,3600)][int]$Ticks = 1200,
      [switch]$ClientWindow, [ValidateSet('Both','Create','Reload')][string]$Phase = 'Both')
$ErrorActionPreference = 'Stop'
$stRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$stRun = Join-Path $stRoot ('build/stability-' + $Profile)
$stSource = Join-Path $stRoot 'src/main/java/thaumcraft/common/lib/events/DebugStabilityAudit.java'
if (Test-Path -LiteralPath $stSource) { throw 'Temporary stability source already exists.' }
$stManifest = Get-Content (Join-Path $stRoot 'tools/compat/manifest.json') -Raw | ConvertFrom-Json
$stResult = 0
Push-Location $stRoot
try {
    New-Item -ItemType Directory -Force $stRun, (Join-Path $stRun 'mods') | Out-Null
    'eula=true' | Set-Content (Join-Path $stRun 'eula.txt')
    @('server-port=25566','online-mode=false','spawn-protection=0','view-distance=6','simulation-distance=4','level-seed=728361','level-name=world','max-tick-time=60000') | Set-Content (Join-Path $stRun 'server.properties')
    if ($Profile -eq 'botania') {
        foreach ($stArtifact in $stManifest.artifacts) {
            $stJar = Join-Path $stRoot ('build/compat-cache/' + $stArtifact.filename)
            if ((Get-FileHash -LiteralPath $stJar -Algorithm SHA512).Hash -ne $stArtifact.sha512) { throw 'Compatibility jar checksum mismatch.' }
            Copy-Item -LiteralPath $stJar -Destination (Join-Path $stRun 'mods')
        }
    }
    Copy-Item -LiteralPath (Join-Path $stRoot 'tools/perf/DebugStabilityAudit.java') -Destination $stSource
    if ($ClientWindow -and (Test-Path -LiteralPath 'build/client-performance.json')) {
        Move-Item -LiteralPath 'build/client-performance.json' -Destination 'build/client-performance.previous.json' -Force
    }
    $stPhases = if ($Phase -eq 'Both') { @('create','reload') } else { @($Phase.ToLowerInvariant()) }
    foreach ($stPhase in $stPhases) {
        $stSaved = Join-Path $stRun ('saved-' + $stPhase + '.ok')
        if (Test-Path -LiteralPath $stSaved) { Remove-Item -LiteralPath $stSaved }
        $stDuration = if ($ClientWindow -and $stPhase -eq 'reload') { 3600 } else { $Ticks }
        @($stPhase, $stDuration) | Set-Content (Join-Path $stRun 'tc-stability-request')
        $stLog = Join-Path $stRun ('console-' + $stPhase + '.out')
        & .\gradlew.bat runServer "-PtcStabilityAudit=$Profile" --offline --console=plain *> $stLog
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $stSaved)) { throw "Stability $stPhase failed. See $stLog" }
        Get-Content -LiteralPath (Join-Path $stRun ('report-' + $stPhase + '.json'))
    }
} catch { Write-Host $_ -ForegroundColor Red; $stResult = 1 }
finally {
    Remove-Item -LiteralPath $stSource -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath (Join-Path $stRun 'tc-stability-request') -ErrorAction SilentlyContinue
    & .\gradlew.bat build --offline --console=plain
    if ($LASTEXITCODE -ne 0) { $stResult = 1 }
    Pop-Location
}
exit $stResult
