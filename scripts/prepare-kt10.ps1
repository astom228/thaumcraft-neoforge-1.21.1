# Фиксированный проверочный набор. Не меняет зависимости мода и не запускает Minecraft.
param([ValidateSet('Base', 'Botania', 'Off')][string]$Profile = 'Base')
$ErrorActionPreference = 'Stop'
$ktRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$ktManifest = Get-Content -LiteralPath (Join-Path $ktRoot 'tools/compat/manifest.json') -Raw | ConvertFrom-Json
$ktCache = Join-Path $ktRoot 'build/compat-cache'
$ktDirs = @('run/mods', 'run-server/mods', 'run-client2/mods') | ForEach-Object { Join-Path $ktRoot $_ }
$ktArtifacts = @($ktManifest.artifacts)
$ktSelected = @($ktArtifacts | Where-Object { $Profile -ne 'Off' -and ($_.profile -eq 'base' -or $Profile -eq 'Botania') })
function Test-KtHash($Path, $Artifact) {
    return (Test-Path -LiteralPath $Path) -and ((Get-FileHash -LiteralPath $Path -Algorithm SHA512).Hash -eq $Artifact.sha512)
}
# Проверяем все назначения до изменений. Чужие/изменённые файлы не перезаписываются.
foreach ($ktDir in $ktDirs) {
    foreach ($ktArtifact in $ktArtifacts) {
        $ktTarget = Join-Path $ktDir $ktArtifact.filename
        if ((Test-Path -LiteralPath $ktTarget) -and -not (Test-KtHash $ktTarget $ktArtifact)) {
            throw "Unexpected content in managed file: $ktTarget"
        }
    }
}
New-Item -ItemType Directory -Force -Path $ktCache | Out-Null
foreach ($ktArtifact in $ktSelected) {
    if ([IO.Path]::GetFileName($ktArtifact.filename) -ne $ktArtifact.filename) { throw 'Invalid manifest filename.' }
    $ktSource = Join-Path $ktCache $ktArtifact.filename
    if (-not (Test-KtHash $ktSource $ktArtifact)) {
        $ktDownload = $ktSource + '.download'
        Invoke-WebRequest -Uri $ktArtifact.url -OutFile $ktDownload
        if (-not (Test-KtHash $ktDownload $ktArtifact)) { throw "Checksum mismatch: $($ktArtifact.filename)" }
        Move-Item -LiteralPath $ktDownload -Destination $ktSource -Force
    }
}
foreach ($ktDir in $ktDirs) {
    New-Item -ItemType Directory -Force -Path $ktDir | Out-Null
    foreach ($ktArtifact in $ktArtifacts) {
        $ktTarget = Join-Path $ktDir $ktArtifact.filename
        if ($ktArtifact -in $ktSelected) {
            Copy-Item -LiteralPath (Join-Path $ktCache $ktArtifact.filename) -Destination $ktTarget
        } elseif (Test-Path -LiteralPath $ktTarget) {
            Remove-Item -LiteralPath $ktTarget
        }
    }
}
Write-Host "KT10 profile: $Profile (Minecraft $($ktManifest.minecraft), NeoForge $($ktManifest.neoforge))"
$ktSelected | Select-Object project, version, channel | Format-Table
if ($Profile -eq 'Botania') { Write-Host 'Botania is an official development snapshot; use a separate test world.' }
