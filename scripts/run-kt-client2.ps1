$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
# Стенд должен быть подготовлен, первый клиент запущен с -PtcAcceptance.
$first = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
    $_.CommandLine -match 'clientRunProgramArgs.txt' -and $_.CommandLine.Contains($root)
})
if ($first.Count -ne 1) { throw 'Нужен ровно один запущенный первый клиент стенда.' }
$match = [regex]::Match($first[0].CommandLine, ' -cp (.+?) net\.neoforged\.devlaunch\.Main ')
if (!$match.Success) { throw 'Не найден classpath первого клиента.' }
$argsPath = Join-Path $root 'build/moddev/kt-client2-classpath.args'
$classPath = $match.Groups[1].Value.Replace('\', '\\')
@('-cp', ('"' + $classPath + '"'), 'net.neoforged.devlaunch.Main') | Set-Content -LiteralPath $argsPath -Encoding utf8NoBOM
$javaw = Join-Path (Split-Path $first[0].ExecutablePath -Parent) 'javaw.exe'
$modsArg = "-Dfml.modFolders=thaumcraft%%$root\build\classes\java\main;thaumcraft%%$root\build\resources\main"
$arguments = @($modsArg, "@$root\build\moddev\client2RunVmArgs.txt", "@$argsPath", "@$root\build\moddev\client2RunProgramArgs.txt")
# Пользователь запросил интерактивный Minecraft: его окно должно быть видимым для игровых проверок.
$process = Start-Process -FilePath $javaw -ArgumentList $arguments -WorkingDirectory "$root/build/kt-client2" -WindowStyle Normal -PassThru -RedirectStandardOutput "$root/build/kt-client2-javaw.log" -RedirectStandardError "$root/build/kt-client2-javaw-error.log"
Write-Output "KT client2 PID: $($process.Id)"
