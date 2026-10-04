#!/usr/bin/env bash
# Проверка регистраций: запускает выделенный сервер (gradlew runServer), ждёт "Done" или ошибку,
# затем останавливает сервер. Gradle не передаёт stdin серверу, поэтому процесс завершается принудительно.
# Использование: scripts/check-server.sh [таймаут_в_секундах]   (по умолчанию 600)
# Код выхода: 0 — сервер запустился, 1 — ошибка/таймаут. Полный лог: run/logs/latest.log
set -u
cd "$(dirname "$0")/.."
TIMEOUT="${1:-600}"
OUT="build/check-server.out"
mkdir -p run build
[ -f run/eula.txt ] || echo "eula=true" > run/eula.txt
[ -f run/server.properties ] || echo "online-mode=false" > run/server.properties
: > "$OUT"

./gradlew runServer --console=plain > "$OUT" 2>&1 &

result=1
for ((i = 0; i < TIMEOUT; i++)); do
  if grep -qE 'Done \([0-9.]+s\)!' "$OUT"; then result=0; break; fi
  if grep -qE 'Crash report|BUILD FAILED|FAILURE:|Missing or unsupported mandatory dependencies|Exception in server tick loop|Failed to start the minecraft server' "$OUT"; then break; fi
  sleep 1
done

# Остановить сервер (devlaunch) и клиент gradle-wrapper этого запуска
powershell.exe -NoProfile -Command "Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | Where-Object { \$_.CommandLine -match 'serverRunProgramArgs|gradle-wrapper.jar runServer' } | ForEach-Object { Stop-Process -Id \$_.ProcessId -Force }" > /dev/null 2>&1
wait 2> /dev/null

if [ $result -eq 0 ]; then
  echo "SERVER OK: $(grep -oE 'Done \([0-9.]+s\)!' "$OUT" | head -1)"
else
  echo "SERVER FAILED (или таймаут ${TIMEOUT}s). Последние строки с ошибками:"
  grep -nE 'ERROR|Exception|FAILURE|Caused by' "$OUT" | grep -v DEBUG | head -40
fi
exit $result
