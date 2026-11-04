param([Parameter(Mandatory=$true)][string[]]$Commands)
$ErrorActionPreference = 'Stop'
$serverDir = Join-Path $PSScriptRoot '../build/kt-server'
if (!(Test-Path (Join-Path $serverDir 'tc-acceptance'))) { throw 'Acceptance marker missing' }
$request = Join-Path $serverDir 'commands.txt'
if (Test-Path $request) { throw 'Previous acceptance command is still pending' }
$lines = @($Commands | ForEach-Object { $_ -split '\r?\n' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$lines += "# request $([guid]::NewGuid())"
$payload = $lines -join "`n"
$pending = Join-Path $serverDir 'commands.pending'
[IO.File]::WriteAllText($pending, $payload)
Move-Item -LiteralPath $pending -Destination $request
$deadline = [DateTime]::UtcNow.AddSeconds(10)
do {
    $done = Join-Path $serverDir 'commands.done'
    if ((Test-Path $done) -and [IO.File]::ReadAllText($done) -ceq $payload) { return }
    Start-Sleep -Milliseconds 100
} while ([DateTime]::UtcNow -lt $deadline)
throw 'Acceptance server did not acknowledge the request in 10 seconds'
