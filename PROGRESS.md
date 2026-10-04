# Прогресс

## Текущее состояние

- **Этап:** 2 «Простые блоки и предметы».
- **Подшаг:** 2.0 (план этапа).

### Этап 1 — завершён
- [x] 1.1 Языковой конвейер: `tools/lang/convert-lang.js` (.lang → .json, переименование ключей блоков/предметов
      по ID_MAPPING, исправления и новые строки из `tools/lang/*.json`), вывод en_us.json / ru_ru.json.
- [x] 1.2 Ассеты: текстуры (2 файла переименовать), звуки + sounds.json, атлас для `textures/blocks|items`, логотип.
- [x] 1.3 Код: регистры (`ConfigBlocks`/`ConfigItems`/`SoundsTC`), вкладка творческого режима (иконка — очки откровения,
      пока простой предмет `goggles`), типы звуков блоков.
- [x] 1.4 Конфиг: `ModConfig` → `ModConfigSpec` (графика — client, разное и мир — common), экран конфига, переводы.
- [x] 1.5 Переводы строк этапа (вкладка, конфиг) en + ru; итоговые проверки, «Этап 1 завершён».

## Сделано

### Подготовка
- [x] 1.1 Удалены пустые вложенные репозитории `Taumcraft/` и `thaumcraft-port/`; `.gitignore` дополнен; шаблон MDK закоммичен.
- [x] 1.2 `reference/src-mcp/` — код с MCP-именами (912 файлов), скрипт `reference/tools/remap-mcp.js`.
- [x] 1.3 Решения: DECISIONS.md, DEVIATIONS.md, CLAUDE.md, ID_MAPPING.md.
- [x] 1.4 Curios `9.5.1+1.21.1` и TerraBlender `1.21.1-4.1.0.8` — в build.gradle и как обязательные в neoforge.mods.toml;
      mod_id `thaumcraft`, пакет `thaumcraft`; `gradlew build` OK, сервер стартует (`scripts/check-server.sh`).
- [x] 1.5 GLOSSARY_RU.md.

## Осталось в текущем этапе

- Составить план этапа 2.

## Известные проблемы / TODO

- `goggles` пока простой предмет (иконка вкладки) — заменить на ItemGoggles на этапе 13.
- Логика ModConfig.postInitLoot/modCompatibility/postInitMisc — перенести на этапах 4 (аспекты/урожай), 17 (лут).
- Предупреждения Gradle о deprecated-фичах (из шаблона MDK) — не критично.

## Замечания с контрольных точек

- После этапа 1: опечатку звука `runicshieldcharge` исправить (сделано, DEVIATIONS №7); такие опечатки ключей/ресурсов исправлять самостоятельно.
- КТ 0: всё принято; ошибки перевода исправлять (DEVIATIONS); в обычный мир — только Magical Forest (DECISIONS п. 11).

## Последний коммит

См. `git log -1` (обновляется при каждом коммите).
