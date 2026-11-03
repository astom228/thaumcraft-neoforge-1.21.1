# Запускается только с разрешения пользователя, одновременно с check-stability -ClientWindow.
$ErrorActionPreference = 'Stop'
$cpRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$cpRun = Join-Path $cpRoot 'build/stability-client'
$cpSource = Join-Path $cpRoot 'src/main/java/thaumcraft/client/fx/DebugClientPerformance.java'
if (Test-Path -LiteralPath $cpSource) { throw 'Client performance source already exists.' }
$cpResult = 0
Push-Location $cpRoot
try {
    New-Item -ItemType Directory -Force $cpRun, (Join-Path $cpRun 'mods') | Out-Null
    'enabled' | Set-Content (Join-Path $cpRun 'tc-client-performance')
    @('lang:ru_ru','renderDistance:8','simulationDistance:5','maxFps:120','enableVsync:false','fullscreen:false','guiScale:2','particles:0','gamma:1.0') | Set-Content (Join-Path $cpRun 'options.txt')
    $cpManifest = Get-Content tools/compat/manifest.json -Raw | ConvertFrom-Json
    foreach ($cpFile in $cpManifest.artifacts) {
        $cpJar = Join-Path $cpRoot ('build/compat-cache/' + $cpFile.filename)
        if ((Get-FileHash -LiteralPath $cpJar -Algorithm SHA512).Hash -ne $cpFile.sha512) { throw 'Client jar checksum mismatch.' }
        Copy-Item -LiteralPath $cpJar -Destination (Join-Path $cpRun 'mods')
    }
    if (Test-Path -LiteralPath build/client-performance.json) {
        Move-Item -LiteralPath build/client-performance.json -Destination build/client-performance.previous.json -Force
    }
    Copy-Item -LiteralPath tools/perf/DebugClientPerformance.java -Destination $cpSource
    & .\gradlew.bat runClient -PtcStabilityClient --offline --console=plain *> build/client-perf-console.out
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath build/client-performance.json)) { throw 'Client performance check failed; see build/client-perf-console.out.' }
    Get-Content -LiteralPath build/client-performance.json
} catch { Write-Host $_ -ForegroundColor Red; $cpResult = 1 }
finally {
    Remove-Item -LiteralPath $cpSource -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath (Join-Path $cpRun 'tc-client-performance') -ErrorAction SilentlyContinue
    & .\gradlew.bat build --offline --console=plain
    if ($LASTEXITCODE -ne 0) { $cpResult = 1 }
    Pop-Location
}
exit $cpResult
