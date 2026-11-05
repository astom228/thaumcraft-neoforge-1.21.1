# Подготовка публикации

Выпуск: `6.1.BETA17-port.1-beta.1`, статус **pre-release / beta**.

- [x] Отдельная версия порта и описание в metadata.
- [x] README с установкой, зависимостями, сборкой и ограничениями.
- [x] CHANGELOG, KNOWN_ISSUES, NOTICE, сохранённая лицензия MDK.
- [x] Удалён путь к локальному JDK; Java21 задаётся переносимо.
- [x] Wrapper с SHA-256, проверка production-JAR, CI Linux/Windows.
- [x] Чистая сборка и обычный Minecraft зафиксированы в RELEASE_VALIDATION.
- [x] Подготовлен `scripts/package-release.ps1`: JAR, документы, ZIP, source ZIP и SHA256SUMS.
- [x] Владелец подтвердил 5 ноября2026 получение разрешения на публичное
      распространение исходников и материалов порта.
- [x] Создан публичный [репозиторий](https://github.com/astom228/thaumcraft-neoforge-1.21.1), загружены main и тег.
- [x] Успешно пройден реальный удалённый [CI Linux/Windows](https://github.com/astom228/thaumcraft-neoforge-1.21.1/actions/runs/42097519324).
- [x] Опубликован [beta-release](https://github.com/astom228/thaumcraft-neoforge-1.21.1/releases/tag/v6.1.BETA17-port.1-beta.1): JAR, release/source ZIP, SHA256SUMS и RELEASE_NOTES.
- [x] Скачанный с GitHub JAR проверен по SHA-256; совпадает с локальным выпуском.

Подготовка файлов не выполняет публикацию автоматически. Внешнее разрешение и
фактический удалённый CI не подменяются локальными проверками.

После обычной сборки и локального коммита:

```powershell
.\scripts\package-release.ps1 -IncludeSource
```

Результат: `build/publication/6.1.BETA17-port.1-beta.1`.
Скрипт ничего не загружает на GitHub и не подтверждает разрешение правообладателя.
