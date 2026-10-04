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
console.log(errors ? `Ошибок: ${errors}` : 'Ресурсы в порядке');
process.exit(errors ? 1 : 0);
