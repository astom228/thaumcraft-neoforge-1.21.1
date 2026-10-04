# Решения

Формат: дата — решение — почему. Отступления от оригинала с описанием «было/стало» — в DEVIATIONS.md.

## Подготовка (2026-10-04)

1. **Baubles → Curios API** `9.5.1+1.21.1` (maven.theillusivec4.top). Слоты: amulet → `necklace`, ring → `ring` (2 шт.),
   belt → `belt`, head → `head`, charm → `charm`. Причина: Baubles для 1.21.1 нет. (Задано в MASTER_PROMPT.)
2. **Биомы → TerraBlender** `1.21.1-4.1.0.8` (maven.minecraftforge.net). (Задано в MASTER_PROMPT.)
3. **Mod ID `thaumcraft`, Java-пакет `thaumcraft`** (не `com.thaumcraft`). Причина: структура пакетов совпадает с оригиналом
   (`thaumcraft.common.blocks…`), поэтому класс порта легко найти по классу в `reference/src-mcp` и наоборот.
   Версия мода `6.1.BETA17`.
4. **ID в ванильном стиле**, варианты metadata разделены. Правила и таблица — `ID_MAPPING.md`. Совместимость с мирами 1.12.2 не нужна.
5. **Ресурсы с заглавными буквами** переименованы в строчные (2 файла, см. `ID_MAPPING.md`).
6. **Справочный код**: `reference/src-mcp` — копия `reference/src` (только `.java`) с MCP-именами stable_39-1.12,
   скрипт `reference/tools/remap-mcp.js`. Без имени остались ~120 идентификаторов (в основном параметры методов).
   Ассеты оригинала берутся из `reference/src/thaumcraft/assets`.
7. **Проверка сервера** — `scripts/check-server.sh`: Gradle не передаёт stdin серверу, поэтому команда `stop` не работает;
   скрипт ждёт `Done (…)!` или ошибку и завершает процесс сервера сам.
8. **Аспекты предметов/сущностей** переносятся **кодом** (порт `ConfigAspects` 1:1, OreDictionary → теги `c:`),
   вычисляются на сервере после загрузки тегов и рецептов (и при `/reload`), результат отправляется клиенту пакетом.
   Причина: ближе всего к оригиналу (900 строк кода, а не данных), и вычисление всё равно требует сервера.
9. **Имена цветов**: баннеры, свечи и ниторы используют ванильные имена цветов 1.21 (`light_gray` вместо `silver`).
10. **Тестовый мир сервера**: при запуске сервера в `run/` создаётся мир `run/world` — он в `.gitignore` (весь `run/`).

## Биомы (2026-10-04, проверено по коду оригинала)

11. **В обычный мир через TerraBlender добавляется только Magical Forest.** В оригинале (`Registrar.registerBiomes`)
    через `BiomeManager.addBiome` добавлен лишь `MAGICAL_FOREST` — типы WARM и COOL, вес `biomeMagicalForestWeight = 5`,
    включается `generateMagicForest` (`ModConfig.CONFIG_WORLD`).
    - **Eerie** и **Outer Lands** (`ELDRITCH`, тип END) только регистрируются и нигде не генерируются; измерения
      для Outer Lands в BETA17 нет. Они используются лишь в проверках: `EntityPech` (не спавнится в Magical Forest/Eerie),
      `EntityTaintSeed`/`EntityTaintacle` (особое поведение в Outer Lands). Переносим их как зарегистрированные
      (data-driven) биомы без размещения в мире — чтобы эти проверки остались 1:1.
    - **Смена биома в рантайме** (`Utils.setBiomeAt`, `PacketBiomeChange`) в BETA17 нигде не вызывается — мёртвый код,
      не переносим.

## Этап 1

12. **Звук `runicshieldcharge`.** В оригинальном `sounds.json` событие записано с опечаткой `runicshieldecharge`, а код
    регистрирует и проигрывает `thaumcraft:runicshieldcharge` — в оригинале этот звук не играет. `sounds.json` перенесён 1:1
    (с опечаткой), поведение то же. Если захочется исправить — переименовать ключ в `sounds.json`.
13. **Атлас блоков**: текстуры оригинала лежат в `textures/blocks` и `textures/items`, а атлас 1.21 по умолчанию берёт
    `block/` и `item/`. Добавлены источники `directory` в `assets/minecraft/atlases/blocks.json` — файлы не переименовываются,
    модели ссылаются на `thaumcraft:blocks/...` и `thaumcraft:items/...` как в оригинале.
14. **Переводы** генерируются скриптом `tools/lang/convert-lang.js` (см. шапку скрипта). Руками `lang/*.json` не править —
    правки вносить в `tools/lang/*.extra.json` или `id-map.json` и перезапускать скрипт. В `ru_ru.json` попадают только
    переведённые строки; непереведённые показываются по-английски, их список — `build/lang-untranslated.txt`.
15. **Конфиг** (`ModConfig` → `ModConfigSpec`): имена опций, значения по умолчанию, диапазоны и комментарии — 1:1.
    Раздел графики — клиентский конфиг (`thaumcraft-client.toml`), разделы «разное» и «мир» — общий (`thaumcraft-common.toml`).
    `@RequiresMcRestart` → `gameRestart()`. `overworldDim` (0) и `dimensionOuterId` (-42) — теперь имена измерений
    (`minecraft:overworld`, `thaumcraft:outer_lands`): в 1.21 нет числовых id, а в коде они используются только для сравнения
    с текущим измерением (Outer Lands в BETA17 нет — как и в оригинале, сравнение никогда не срабатывает).
    Подписи опций на экране настроек — новые строки (в оригинале их не было), en + ru.
