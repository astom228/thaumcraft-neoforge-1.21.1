# Thaumcraft 6 → NeoForge 1.21.1

Личный порт Thaumcraft 6 (Minecraft 1.12.2) на NeoForge 1.21.1. Только для себя, без публикации.

## Исходники для справки

- Оригинал: `reference/Thaumcraft-1.12.2-6.1.BETA17.jar`
- Baubles (для справки): `reference/Baubles-1.12-1.5.2.jar`
- Декомпилированный код (Vineflower): `reference/src/thaumcraft`, `reference/src/baubles`.
  Только для чтения — **не править**. Код там в SRG-именах (`func_xxxxx_x`, `field_xxxxx_x`).
- Папка `reference/` в git не попадает (`.gitignore`).

## Правила

- Цель — перенести механику и графику **1:1**. Ничего не улучшать и не придумывать.
- Текстуры, звуки и переводы брать из оригинала без изменений.
- Mod ID: `thaumcraft`.
- Java 21 в `gradle.properties` (`org.gradle.java.home`) не менять.
- Работать маленькими этапами; после каждого этапа проверять `gradlew build`.
- Перед большими изменениями сначала показывать план.
- Общаться с пользователем по-русски.

## План

Порядок переноса и этапы — в `PORTING_PLAN.md`.
