// Конвертация переводов оригинала (.lang 1.12) в .json 1.21.
//   node tools/lang/convert-lang.js
// Источники:
//   reference/src/thaumcraft/assets/thaumcraft/lang/{en_us,ru_ru}.lang — оригинал (только чтение);
//   tools/lang/id-map.json — переименование ключей названий блоков/предметов по ID_MAPPING.md, удаляемые ключи;
//   tools/lang/en_us.extra.json, tools/lang/ru_ru.extra.json — новые строки порта и исправления/переводы (перекрывают оригинал).
// Результат: src/main/resources/assets/thaumcraft/lang/{en_us,ru_ru}.json.
// В ru_ru.json попадают только строки, где есть кириллица (или явно заданные в ru_ru.extra.json);
// непереведённое отображается из en_us — список печатается в отчёте (build/lang-untranslated.txt).
'use strict';
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..', '..');
const srcDir = path.join(root, 'reference/src/thaumcraft/assets/thaumcraft/lang');
const outDir = path.join(root, 'src/main/resources/assets/thaumcraft/lang');
const map = JSON.parse(fs.readFileSync(path.join(__dirname, 'id-map.json'), 'utf8'));
const readJson = (f) => (fs.existsSync(f) ? JSON.parse(fs.readFileSync(f, 'utf8')) : {});
const enExtra = readJson(path.join(__dirname, 'en_us.extra.json'));
const ruExtra = readJson(path.join(__dirname, 'ru_ru.extra.json'));
delete enExtra._comment;
delete ruExtra._comment;

function loadLang(file) {
  const out = new Map();
  for (const line of fs.readFileSync(file, 'utf8').replace(/^﻿/, '').split(/\r?\n/)) {
    if (!line.trim() || line.trimStart().startsWith('#')) continue;
    const i = line.indexOf('=');
    if (i < 0) continue;
    out.set(line.slice(0, i), line.slice(i + 1));
  }
  return out;
}

const drop = map.drop.map((r) => new RegExp(r));

// Старый ключ -> массив новых ключей (или null, если ключ удаляется)
function renameKey(key) {
  if (drop.some((r) => r.test(key))) return null;
  let m = key.match(/^item\.(.+)\.name$/);
  if (m) {
    const id = map.items[m[1]] ?? m[1];
    return [].concat(id).map((x) => `item.thaumcraft.${x}`);
  }
  m = key.match(/^tile\.(.+)\.name$/);
  if (m) {
    let old = m[1];
    const c = old.match(/^([a-z]+)_([a-z]+)$/);
    if (c && map.colorBlocks.includes(c[1]) && map.colors[c[2]]) return [`block.thaumcraft.${map.colors[c[2]]}_${c[1]}`];
    const id = map.blocks[old] ?? old;
    return [].concat(id).map((x) => `block.thaumcraft.${x}`);
  }
  return [key];
}

function convert(lang) {
  const out = new Map();
  for (const [k, v] of lang) {
    const keys = renameKey(k);
    if (!keys) continue;
    for (const nk of keys) {
      if (out.has(nk) && out.get(nk) !== v) console.warn(`  ! конфликт ключа ${nk} (из ${k})`);
      out.set(nk, v);
    }
  }
  return out;
}

const en = convert(loadLang(path.join(srcDir, 'en_us.lang')));
const ruAll = convert(loadLang(path.join(srcDir, 'ru_ru.lang')));
const cyr = /[а-яё]/i;

for (const [k, v] of Object.entries(enExtra)) en.set(k, v);

const ru = new Map();
for (const [k, v] of ruAll) if (en.has(k) && cyr.test(v)) ru.set(k, v);
for (const [k, v] of Object.entries(ruExtra)) ru.set(k, v);

// Проверки
let errors = 0;
for (const k of ru.keys()) if (!en.has(k)) { console.error(`  ✗ ключ ru без en: ${k}`); errors++; }
for (const k of Object.keys(enExtra)) if (!ru.has(k)) { console.error(`  ✗ новая строка без перевода на русский: ${k}`); errors++; }

const sortObj = (m) => Object.fromEntries([...m.entries()].sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0)));
fs.mkdirSync(outDir, { recursive: true });
fs.writeFileSync(path.join(outDir, 'en_us.json'), JSON.stringify(sortObj(en), null, 2) + '\n');
fs.writeFileSync(path.join(outDir, 'ru_ru.json'), JSON.stringify(sortObj(ru), null, 2) + '\n');

const untranslated = [...en.keys()].filter((k) => !ru.has(k)).sort();
fs.mkdirSync(path.join(root, 'build'), { recursive: true });
fs.writeFileSync(path.join(root, 'build/lang-untranslated.txt'), untranslated.join('\n') + '\n');
const byPrefix = {};
for (const k of untranslated) { const p = k.split('.')[0]; byPrefix[p] = (byPrefix[p] || 0) + 1; }
console.log(`en_us: ${en.size} ключей, ru_ru: ${ru.size} ключей, без перевода: ${untranslated.length}`);
console.log('  без перевода по префиксам:', JSON.stringify(byPrefix));
if (errors) { console.error(`Ошибок: ${errors}`); process.exit(1); }
