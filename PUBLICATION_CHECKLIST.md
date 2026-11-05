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
- [ ] Репозиторий создан на GitHub, выполнен push и проверен реальный удалённый CI.
- [ ] Создан GitHub Release с beta/pre-release, JAR, SHA256SUMS и текстом RELEASE_NOTES.

Подготовка файлов не выполняет публикацию автоматически. Внешнее разрешение и
фактический удалённый CI не подменяются локальными проверками.

После обычной сборки и локального коммита:

```powershell
.\scripts\package-release.ps1 -IncludeSource
```

Результат: `build/publication/6.1.BETA17-port.1-beta.1`.
Скрипт ничего не загружает на GitHub и не подтверждает разрешение правообладателя.
