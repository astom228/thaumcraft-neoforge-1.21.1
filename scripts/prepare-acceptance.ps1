$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$manifest = Get-Content "$root/tools/compat/manifest.json" -Raw | ConvertFrom-Json
# Только закреплённые версии; в кеше также лежат несовместимые экспериментальные версии.
foreach ($artifact in $manifest.artifacts) {
    $source = Join-Path "$root/build/compat-cache" $artifact.filename
    if (!(Test-Path -LiteralPath $source)) { throw "Нет закреплённого файла: $source" }
    if ((Get-FileHash -LiteralPath $source -Algorithm SHA512).Hash.ToLowerInvariant() -ne $artifact.sha512) {
        throw "SHA512 не совпал: $source"
    }
}
foreach ($name in 'kt-server','kt-client1','kt-client2') {
    $dir = "$root/build/$name"
    New-Item -ItemType Directory -Force "$dir/mods" | Out-Null
    foreach ($artifact in $manifest.artifacts) {
        Copy-Item -LiteralPath "$root/build/compat-cache/$($artifact.filename)" -Destination "$dir/mods/$($artifact.filename)"
    }
    if ($name -ne 'kt-server' -and !(Test-Path "$dir/options.txt")) {
        @('lang:ru_ru','renderDistance:8','simulationDistance:5','maxFps:90','enableVsync:false','fullscreen:false','guiScale:2','particles:0','gamma:1.0','pauseOnLostFocus:false') | Set-Content "$dir/options.txt"
    }
}
Set-Content "$root/build/kt-server/eula.txt" 'eula=true'
Set-Content "$root/build/kt-server/tc-acceptance" 'isolated acceptance world'
if (!(Test-Path "$root/build/kt-server/server.properties")) {
    @('server-port=25567','online-mode=false','spawn-protection=0','view-distance=6','simulation-distance=4','level-seed=728361','level-name=world','max-tick-time=60000','gamemode=creative','motd=Thaumcraft isolated KT') | Set-Content "$root/build/kt-server/server.properties"
}
Write-Output 'Изолированный стенд build/kt-* подготовлен; обычные run* не затронуты.'
