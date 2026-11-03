# Сверка численных параметров рецептов с оригиналом. Minecraft-клиент не запускается.
# Временный серверный класс и триггер удаляются в finally; после проверки собирается jar без проверочного класса.
param([ValidateRange(30, 600)][int]$TimeoutSeconds = 180)
$ErrorActionPreference = 'Stop'
$recipeAuditRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$recipeAuditTarget = Join-Path $recipeAuditRoot 'src/main/java/thaumcraft/common/lib/events/DebugRecipeDump.java'
$recipeAuditTrigger = Join-Path $recipeAuditRoot 'run-server/tc-recipe-dump'
if ((Test-Path -LiteralPath $recipeAuditTarget) -or (Test-Path -LiteralPath $recipeAuditTrigger)) {
    throw 'Recipe audit source or trigger already exists; refusing to overwrite it.'
}
$recipeAuditGit = (Get-Command git -ErrorAction Stop).Source
$recipeAuditBash = [System.IO.Path]::GetFullPath((Join-Path (Split-Path $recipeAuditGit -Parent) '../bin/bash.exe'))
if (-not (Test-Path -LiteralPath $recipeAuditBash)) { throw 'Git Bash was not found next to Git.' }
$recipeAuditExit = 0
$recipeAuditOldLinger = $env:CHECK_SERVER_LINGER
Push-Location $recipeAuditRoot
try {
    Copy-Item -LiteralPath (Join-Path $recipeAuditRoot 'tools/recipes/DebugRecipeDump.java') -Destination $recipeAuditTarget
    New-Item -ItemType File -Path $recipeAuditTrigger | Out-Null
    # Старый дамп не должен подменить результат неудачного запуска сервера.
    $recipeAuditDump = Join-Path $recipeAuditRoot 'build/recipe-audit-actual.json'
    if (Test-Path -LiteralPath $recipeAuditDump) { Remove-Item -LiteralPath $recipeAuditDump }
    & node tools/recipes/convert-arcane.js
    if ($LASTEXITCODE -ne 0) { throw 'Arcane recipe conversion failed.' }
    & node tools/aspects/check-aspects.js
    if ($LASTEXITCODE -ne 0) { throw 'Aspect declaration comparison failed.' }
    $env:CHECK_SERVER_LINGER = '5'
    & $recipeAuditBash scripts/check-server.sh $TimeoutSeconds
    if ($LASTEXITCODE -ne 0) { throw 'Server recipe dump failed.' }
    if (-not (Select-String -LiteralPath 'build/check-server.out' -SimpleMatch '[TC-RECIPE-DUMP] PASS: all functional audits' -Quiet)) {
        throw 'Functional server audits did not finish successfully; see build/check-server.out.'
    }
    & node tools/recipes/check-recipes.js build/recipe-audit-actual.json
    if ($LASTEXITCODE -ne 0) { throw 'Recipe comparison failed.' }
} catch {
    Write-Host $_ -ForegroundColor Red
    $recipeAuditExit = 1
} finally {
    $env:CHECK_SERVER_LINGER = $recipeAuditOldLinger
    Remove-Item -LiteralPath $recipeAuditTarget -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $recipeAuditTrigger -ErrorAction SilentlyContinue
    & .\gradlew.bat build --offline --console=plain
    if ($LASTEXITCODE -ne 0) { $recipeAuditExit = 1 }
    Pop-Location
}
exit $recipeAuditExit
