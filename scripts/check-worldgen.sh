#!/usr/bin/env bash
# Проверка генерации мира: запускает выделенный сервер на свежем тестовом мире (level-name=wg_test_<время>),
# затем возвращает прежний server.properties и удаляет тестовый мир. Ошибки генерации — в выводе.
# Использование: scripts/check-worldgen.sh [таймаут_в_секундах]
set -u
cd "$(dirname "$0")/.."
PROPS=run-server/server.properties
BAK=build/server.properties.bak
mkdir -p build run-server
[ -f "$PROPS" ] || echo "online-mode=false" > "$PROPS"
cp "$PROPS" "$BAK"
NAME="wg_test_$(date +%H%M%S)"
grep -v "^level-name" "$BAK" > "$PROPS"
echo "level-name=$NAME" >> "$PROPS"
bash scripts/check-server.sh "${1:-1500}"
result=$?
cp "$BAK" "$PROPS"
grep -E "ERROR|Exception" run-server/logs/latest.log | grep -v "DEBUG" | head -20
rm -rf "run-server/$NAME"
exit $result
