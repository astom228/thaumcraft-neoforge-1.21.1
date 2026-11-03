// Проверка ресурсов: все текстуры и модели, на которые ссылаются blockstates/модели мода, существуют.
//   node tools/assets/check-assets.js
'use strict';
const fs = require('fs');
const path = require('path');

const res = path.resolve(__dirname, '..', '..', 'src/main/resources/assets');
const ns = 'thaumcraft';
let errors = 0;

function walk(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) =>
    e.isDirectory() ? walk(path.join(dir, e.name)) : e.name.endsWith('.json') ? [path.join(dir, e.name)] : []);
}
const exists = (rl, folder, ext) => {
  const [n, p] = rl.includes(':') ? rl.split(':') : ['minecraft', rl];
  if (n !== ns) return true; // ванильные/чужие ресурсы не проверяем
  return fs.existsSync(path.join(res, n, folder, p + ext));
};
const fail = (file, msg) => { console.error(`✗ ${path.relative(res, file)}: ${msg}`); errors++; };

for (const f of walk(path.join(res, ns, 'models'))) {
  const m = JSON.parse(fs.readFileSync(f, 'utf8'));
  if (m.parent && !m.parent.startsWith('builtin/') && !exists(m.parent, 'models', '.json')) fail(f, `нет модели-родителя ${m.parent}`);
  for (const [k, t] of Object.entries(m.textures || {})) {
    if (t.startsWith('#')) continue;
    if (!exists(t, 'textures', '.png')) fail(f, `нет текстуры ${k} = ${t}`);
  }
}
for (const f of walk(path.join(res, ns, 'blockstates'))) {
  const bs = JSON.parse(fs.readFileSync(f, 'utf8'));
  const list = Object.values(bs.variants || {}).flatMap((v) => (Array.isArray(v) ? v : [v]));
  for (const part of bs.multipart || []) list.push(...(Array.isArray(part.apply) ? part.apply : [part.apply]));
  for (const v of list) if (!exists(v.model, 'models', '.json')) fail(f, `нет модели ${v.model}`);
}
const languages = ['en_us', 'ru_ru'].map(locale => [locale,
  JSON.parse(fs.readFileSync(path.join(res, ns, 'lang', locale + '.json'), 'utf8'))]);
for (const f of walk(path.join(res, ns, 'research'))) {
  for (const entry of JSON.parse(fs.readFileSync(f, 'utf8')).entries || []) {
    for (const [locale, translations] of languages) {
      if (entry.name && !Object.hasOwn(translations, entry.name)) fail(f, `${entry.key}: нет названия ${entry.name} (${locale})`);
    }
    for (const icon of entry.icons || []) {
      if (!icon.startsWith(ns + ':')) continue;
      const id = icon.slice(ns.length + 1).split(';')[0];
      const target = id.startsWith('textures/') ? path.join(res, ns, id) : path.join(res, ns, 'models/item', id + '.json');
      if (!fs.existsSync(target)) fail(f, `${entry.key}: нет ресурса значка ${icon}`);
    }
  }
}
console.log(errors ? `Ошибок: ${errors}` : 'Ресурсы в порядке');
process.exit(errors ? 1 : 0);
