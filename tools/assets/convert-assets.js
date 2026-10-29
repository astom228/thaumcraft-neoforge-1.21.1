// Конвертация blockstates/моделей оригинала (1.12, в т.ч. forge_marker) в формат 1.21.
//   node tools/assets/convert-assets.js
// Список того, что конвертировать, — tools/assets/assets.json (пополняется по этапам):
//   blocks: { "<новый id>": { "src": "<имя blockstate оригинала>", "props": {...}, "ignore": [...], "type": "slab|...", "item": false,
//             "itemModel": "<источник>", "overrides": [{ "predicate": {...}, "model": "..." }] } }
//   items:  { "<новый id>": "<источник>" }  источник: "bs:<blockstate>#<вариант>" (вариант предмета из blockstate)
//                                                  или "model:<имя файла models/item>" (копия модели предмета),
//                                                  или "obj:<blockstate>#<вариант>" (OBJ-модель -> neoforge:obj).
// Содержимое моделей/текстур не меняется: меняются только формат и имена файлов.
'use strict';
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..', '..');
const SRC = path.join(root, 'reference/src/thaumcraft/assets/thaumcraft');
const OUT = path.join(root, 'src/main/resources/assets/thaumcraft');
const cfg = JSON.parse(fs.readFileSync(path.join(__dirname, 'assets.json'), 'utf8'));

const readSrc = (rel) => JSON.parse(fs.readFileSync(path.join(SRC, rel), 'utf8'));
const write = (rel, obj) => {
  const f = path.join(OUT, rel);
  fs.mkdirSync(path.dirname(f), { recursive: true });
  fs.writeFileSync(f, JSON.stringify(obj, null, 2) + '\n');
};

// Ванильные модели, переименованные между 1.12 и 1.21
const VANILLA_RENAMED = { half_slab: 'slab', upper_slab: 'slab_top' };

// Имя модели 1.12 -> ResourceLocation модели 1.21
function modelRef(m) {
  if (!m) return null;
  const base = m.replace(/^(minecraft:)?(block\/)?/, '');
  if (VANILLA_RENAMED[base] && !m.startsWith('thaumcraft:')) return 'minecraft:block/' + VANILLA_RENAMED[base];
  if (m.includes('/')) return m.includes(':') ? m : 'minecraft:' + m; // "block/cube", "thaumcraft:block/x"
  if (m.includes(':')) {
    const [ns, name] = m.split(':');
    return `${ns}:block/${name}`;
  }
  return 'minecraft:block/' + m; // "cube_all", "cross", "leaves"...
}

// Копирует собственную модель оригинала (models/block/<name>.json) как есть
const copied = new Set();
function copyBlockModel(ref, renderType) {
  const m = ref.match(/^thaumcraft:block\/(.+)$/);
  if (!m || copied.has(m[1])) return;
  const src = path.join(SRC, 'models/block', m[1] + '.json');
  if (!fs.existsSync(src)) throw new Error('нет модели оригинала: ' + src);
  copied.add(m[1]);
  const model = JSON.parse(fs.readFileSync(src, 'utf8'));
  if (model.parent) {
    model.parent = modelRef(model.parent);
    copyBlockModel(model.parent);
  }
  if (renderType && !model.render_type) model.render_type = renderType;
  write(`models/block/${m[1]}.json`, model);
}

// Слияние записей forge: defaults <- property overrides <- variant entry
function merge(...parts) {
  const out = {};
  for (const p of parts) {
    if (!p) continue;
    for (const [k, v] of Object.entries(p)) {
      if (k === 'textures') out.textures = { ...(out.textures || {}), ...v };
      else if (k !== 'transform' && k !== 'submodel' && k !== 'custom') out[k] = v;
    }
  }
  return out;
}

// Запись варианта -> запись blockstate 1.21 (+ генерация модели при наличии текстур)
function toVariant(id, entry, models, renderType) {
  // OBJ-модель блока (forge OBJ loader 1.12, flip-v по умолчанию выкл.) -> загрузчик neoforge:obj
  if ((entry.model || '').endsWith('.obj')) {
    const key = JSON.stringify(['obj', entry.model, entry.textures]);
    if (!models.has(key)) {
      const name = models.size === 0 ? id : `${id}_${models.size}`;
      models.set(key, `thaumcraft:block/${name}`);
      const textures = {};
      for (const [k, t] of Object.entries(entry.textures || {})) textures[k.replace(/^#/, '')] = t;
      if (!textures.particle) textures.particle = Object.values(textures)[0];
      const model = { loader: 'neoforge:obj', model: copyObj(entry.model.replace(/^thaumcraft:/, '')), flip_v: false, textures };
      if (renderType) model.render_type = renderType;
      write(`models/block/${name}.json`, model);
    }
    const v = { model: models.get(key) };
    if (entry.x) v.x = entry.x;
    if (entry.y) v.y = entry.y;
    return v;
  }
  let ref = modelRef(entry.model);
  if (entry.textures && Object.keys(entry.textures).length) {
    const key = JSON.stringify([ref, entry.textures]);
    if (!models.has(key)) {
      const name = models.size === 0 ? id : `${id}_${models.size}`;
      models.set(key, `thaumcraft:block/${name}`);
      const model = { parent: ref, textures: entry.textures };
      if (renderType) model.render_type = renderType;
      write(`models/block/${name}.json`, model);
      copyBlockModel(ref);
    }
    ref = models.get(key);
  } else {
    copyBlockModel(ref, renderType);
  }
  const v = { model: ref };
  if (entry.x) v.x = entry.x;
  if (entry.y) v.y = entry.y;
  if (entry.uvlock) v.uvlock = entry.uvlock;
  if (entry.weight && entry.weight !== 1) v.weight = entry.weight;
  return v;
}

const variantOut = (list) => (list.length === 1 ? list[0] : list);
const asList = (x) => (Array.isArray(x) ? x : [x]);

// Мультипарт (трубки): условия when переносятся как есть, модели — по ссылке (копируются); предмет — из spec.itemModel
function convertMultipart(id, spec, bs) {
  const multipart = bs.multipart.map((part) => {
    const apply = asList(part.apply).map((e) => toVariant(id, e, new Map(), spec.renderType));
    const out = { apply: variantOut(apply) };
    if (part.when) out.when = part.when;
    return out;
  });
  write(`blockstates/${id}.json`, { multipart });
  if (spec.itemModel) writeItem(id, spec.itemModel);
}

function convertBlock(id, spec) {
  const bs = readSrc(`blockstates/${spec.src}.json`);
  if (bs.multipart) return convertMultipart(id, spec, bs);
  const ignore = new Set(spec.ignore || []);
  const propMap = spec.props || {};
  const models = new Map();
  const defaults = bs.defaults || {};
  const variants = {};
  let inventory = null;

  const skip = new Set(spec.skip || []); // варианты, которых нет у свойства 1.21 (например facing=up у горизонтальной стороны)
  const simpleKeys = Object.keys(bs.variants).filter((k) => !skip.has(k) && (k === 'normal' || k === 'inventory' || k.includes('=')));
  const propKeys = bs.forge_marker ? Object.keys(bs.variants).filter((k) => !simpleKeys.includes(k) && !skip.has(k) && !ignore.has(k)) : [];
  // игнорируемые свойства: в ключ не попадают, но их переопределения (первое значение) применяются
  const ignoredOver = bs.forge_marker ? Object.keys(bs.variants).filter((k) => !simpleKeys.includes(k) && !skip.has(k) && ignore.has(k)).map((k) => Object.values(bs.variants[k])[0]) : [];

  for (const k of simpleKeys) {
    const entries = asList(bs.variants[k]).map((e) => merge(defaults, ...ignoredOver, e));
    if (k === 'inventory') { inventory = entries[0]; continue; }
    if (k === 'normal') {
      if (propKeys.length) continue; // при наличии свойств «normal» — только значение по умолчанию
      variants[''] = variantOut(entries.map((e) => toVariant(id, e, models, spec.renderType)));
      continue;
    }
    // "prop=value,..." — переименование/фильтрация свойств
    const parts = k.split(',').map((p) => p.split('=')).filter(([p]) => !ignore.has(p));
    const key = parts.map(([p, v]) => `${propMap[p] || p}=${(spec.values && spec.values[p] && spec.values[p][v]) || v}`).join(',');
    variants[key] = variantOut(entries.map((e) => toVariant(id, e, models, spec.renderType)));
  }

  if (propKeys.length) {
    // декартово произведение свойств forge_marker
    let combos = [{ key: [], parts: [] }];
    for (const p of propKeys) {
      const next = [];
      for (const c of combos)
        for (const [val, over] of Object.entries(bs.variants[p]))
          next.push({ key: [...c.key, `${propMap[p] || p}=${(spec.values && spec.values[p] && spec.values[p][val]) || val}`], parts: [...c.parts, over] });
      combos = next;
    }
    const normal = bs.variants.normal ? asList(bs.variants.normal) : [{}];
    for (const c of combos) {
      const entries = normal.map((n) => merge(defaults, ...ignoredOver, ...c.parts, n));
      variants[c.key.join(',')] = variantOut(entries.map((e) => toVariant(id, e, models, spec.renderType)));
    }
  }

  // Плиты: half=bottom/top из оригинала + type=double из blockstate двойной плиты
  if (spec.type === 'slab') {
    const out = {};
    for (const [k, v] of Object.entries(variants)) out[k.replace(/^half=/, 'type=')] = v;
    const dbs = readSrc(`blockstates/${spec.double}.json`);
    const de = merge(dbs.defaults || {}, asList(Object.values(dbs.variants).find((v, i) => Object.keys(dbs.variants)[i] !== 'inventory'))[0]);
    out['type=double'] = toVariant(id + '_double', de, new Map(), spec.renderType);
    Object.keys(variants).forEach((k) => delete variants[k]);
    Object.assign(variants, out);
  }

  write(`blockstates/${id}.json`, { variants });

  if (spec.item === false) return;
  // Модель предмета
  if (spec.itemModel) { writeItem(id, spec.itemModel); return addOverrides(id, spec); }
  if (inventory && (inventory.model === 'forge:item-layer' || (inventory.model || '').startsWith('item/'))) {
    write(`models/item/${id}.json`, itemLayer(inventory));
  } else {
    const inv = inventory && Object.keys(inventory).length ? inventory : null;
    let parent;
    if (inv && (inv.model || inv.textures)) parent = toVariant(id, inv, models, spec.renderType).model;
    else parent = asList(Object.values(variants)[0])[0].model;
    const item = { parent };
    // forge:default-block — стандартные преобразования блока (как у minecraft:block/block); нужны, если у модели нет родителя
    const invRaw = bs.variants.inventory ? asList(bs.variants.inventory)[0] : null;
    const tr = (invRaw && invRaw.transform) || defaults.transform;
    if (tr === 'forge:default-block' && !hasParent(parent)) item.display = DEFAULT_BLOCK_DISPLAY;
    write(`models/item/${id}.json`, item);
  }
}

const DEFAULT_BLOCK_DISPLAY = {
  gui: { rotation: [30, 225, 0], translation: [0, 0, 0], scale: [0.625, 0.625, 0.625] },
  ground: { rotation: [0, 0, 0], translation: [0, 3, 0], scale: [0.25, 0.25, 0.25] },
  fixed: { rotation: [0, 0, 0], translation: [0, 0, 0], scale: [0.5, 0.5, 0.5] },
  thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.375, 0.375, 0.375] },
  firstperson_righthand: { rotation: [0, 45, 0], translation: [0, 0, 0], scale: [0.4, 0.4, 0.4] },
  firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, 0, 0], scale: [0.4, 0.4, 0.4] },
};
// Есть ли у модели thaumcraft:block/... цепочка родителей (тогда преобразования наследуются)
function hasParent(ref) {
  const m = ref.match(/^thaumcraft:block\/(.+)$/);
  if (!m) return true;
  const p = path.join(OUT, 'models/block', m[1] + '.json');
  if (!fs.existsSync(p)) return true;
  const model = JSON.parse(fs.readFileSync(p, 'utf8'));
  return model.parent ? hasParent(model.parent) : false;
}

function itemLayer(e) {
  const tool = e.transform === 'forge:default-tool';
  return { parent: tool ? 'minecraft:item/handheld' : 'minecraft:item/generated', textures: e.textures };
}

// --- OBJ-модели предметов (forge_marker с "model": "*.obj") -> загрузчик neoforge:obj ---------------------------------
// Преобразования forge (TRSR вокруг центра блока, сдвиг в блоках, список поворотов) -> display 1.21
// (сдвиг в 1/16, углы Эйлера XYZ того же поворота, масштаб тройкой).
const PERSPECTIVES = {
  firstperson: 'firstperson_righthand', firstperson_lefthand: 'firstperson_lefthand',
  thirdperson: 'thirdperson_righthand', thirdperson_lefthand: 'thirdperson_lefthand',
  gui: 'gui', ground: 'ground', fixed: 'fixed', head: 'head',
};
function axisMatrix(axis, deg) {
  const a = deg * Math.PI / 180, c = Math.cos(a), sn = Math.sin(a);
  if (axis === 'x') return [[1, 0, 0], [0, c, -sn], [0, sn, c]];
  if (axis === 'y') return [[c, 0, sn], [0, 1, 0], [-sn, 0, c]];
  return [[c, -sn, 0], [sn, c, 0], [0, 0, 1]];
}
const mul = (A, B) => A.map((r, i) => B[0].map((_, j) => r.reduce((acc, _v, k) => acc + A[i][k] * B[k][j], 0)));
const round = (v) => Math.round(v * 1e4) / 1e4 || 0;
function eulerXYZ(rotList) {
  let M = [[1, 0, 0], [0, 1, 0], [0, 0, 1]];
  for (const r of asList(rotList)) for (const [ax, deg] of Object.entries(r)) M = mul(M, axisMatrix(ax, deg));
  const y = Math.asin(Math.max(-1, Math.min(1, M[0][2])));
  let x, z;
  if (Math.abs(M[0][2]) < 0.99999) { x = Math.atan2(-M[1][2], M[2][2]); z = Math.atan2(-M[0][1], M[0][0]); }
  else { x = Math.atan2(M[2][1], M[1][1]); z = 0; }
  return [x, y, z].map((v) => round(v * 180 / Math.PI));
}
function convertDisplay(transform) {
  const display = {};
  for (const [k, t] of Object.entries(transform || {})) {
    const name = PERSPECTIVES[k];
    if (!name) throw new Error('неизвестная перспектива ' + k);
    const d = {};
    if (t.rotation) d.rotation = eulerXYZ(t.rotation);
    if (t.translation) d.translation = t.translation.map((v) => round(v * 16));
    if (t.scale !== undefined) d.scale = Array.isArray(t.scale) ? t.scale : [t.scale, t.scale, t.scale];
    display[name] = d;
  }
  return display;
}
// Копия OBJ (без изменений) и MTL: пути текстур заменяются ссылками "#материал" (в 1.21 обратная косая черта недопустима;
// сами текстуры задаёт blockstate оригинала через "#материал").
const copiedObj = new Set();
function copyObj(objName) {
  const dir = ['models/item', 'models/obj', 'models/block'].find((d) => fs.existsSync(path.join(SRC, d, objName)));
  if (!dir) throw new Error('нет OBJ ' + objName);
  const rel = `${dir}/${objName}`;
  if (!copiedObj.has(rel)) {
    copiedObj.add(rel);
    const out = path.join(OUT, rel);
    fs.mkdirSync(path.dirname(out), { recursive: true });
    fs.copyFileSync(path.join(SRC, rel), out);
    const mtl = path.join(SRC, dir, objName.replace(/\.obj$/, '.mtl'));
    if (fs.existsSync(mtl)) {
      let m = fs.readFileSync(mtl, 'utf8'), mat = null;
      m = m.split(/\r?\n/).map((line) => {
        const nm = line.match(/^\s*newmtl\s+(\S+)/);
        if (nm) mat = nm[1];
        return line.replace(/^(\s*map_K[ad]\s+).*$/, (_, p) => p + '#' + mat);
      }).join('\n');
      fs.writeFileSync(out.replace(/\.obj$/, '.mtl'), m);
    }
  }
  return 'thaumcraft:' + rel;
}
function writeObjItem(id, bsName, variant) {
  const bs = readSrc(`blockstates/${bsName}.json`);
  const d = bs.defaults || {};
  const v = asList(bs.variants[variant])[0] || {};
  const modelName = (v.model || d.model).replace(/^thaumcraft:/, '');
  const textures = {};
  for (const [k, t] of Object.entries(Object.assign({}, d.textures, v.textures))) textures[k.replace(/^#/, '')] = t;
  if (!textures.particle) textures.particle = Object.values(textures)[0];
  const custom = Object.assign({}, d.custom, v.custom);
  const model = { loader: 'neoforge:obj', model: copyObj(modelName), flip_v: custom['flip-v'] === true, textures };
  const tr = v.transform !== undefined ? v.transform : d.transform;
  // строковый transform forge (forge:default-block) — стандартные преобразования блока
  const display = typeof tr === 'string' ? (tr === 'forge:default-block' ? DEFAULT_BLOCK_DISPLAY : {}) : convertDisplay(Object.assign({}, typeof d.transform === 'object' ? d.transform : {}, v.transform));
  if (Object.keys(display).length) model.display = display;
  write(`models/item/${id}.json`, model);
}

// Переключение модели предмета по свойству (spec.overrides: [{ predicate, model }]) — замена metadata-вариантов предмета
function addOverrides(id, spec) {
  if (!spec.overrides) return;
  const p = path.join(OUT, `models/item/${id}.json`);
  const model = JSON.parse(fs.readFileSync(p, "utf8"));
  model.overrides = spec.overrides;
  fs.writeFileSync(p, JSON.stringify(model, null, 2) + '\n');
}

function writeItem(id, src) {
  const om = src.match(/^obj:([^#]+)#(.+)$/);
  if (om) return writeObjItem(id, om[1], om[2]);
  if (src.startsWith('model:')) {
    const model = readSrc(`models/item/${src.slice(6)}.json`);
    if (model.parent === 'builtin/generated') model.parent = 'item/generated';
    if (model.parent) model.parent = model.parent.includes(':') ? model.parent : 'minecraft:' + model.parent;
    // display 1.8–1.12 без руки (thirdperson/firstperson) -> правая рука 1.21 (левая зеркалится движком)
    if (model.display) {
      const D = { thirdperson: 'thirdperson_righthand', firstperson: 'firstperson_righthand' };
      const out = {};
      for (const [k, v] of Object.entries(model.display)) out[D[k] || k] = v;
      model.display = out;
    }
    // свойства предметов Thaumcraft в 1.12 регистрировались без пространства имён; в 1.21 — thaumcraft:<имя>
    const TC_PROPS = { focus: 'thaumcraft:focus' };
    for (const o of model.overrides || []) {
      const p = {};
      for (const [k, v] of Object.entries(o.predicate || {})) p[TC_PROPS[k] || k] = v;
      o.predicate = p;
      if (o.model && !o.model.includes(':')) o.model = 'minecraft:' + o.model;
    }
    write(`models/item/${id}.json`, model);
    return;
  }
  const m = src.match(/^bs:([^#]+)#(.+)$/);
  if (!m) throw new Error('неизвестный источник предмета: ' + src);
  const bs = readSrc(`blockstates/${m[1]}.json`);
  const e = merge(bs.defaults || {}, asList(bs.variants[m[2]] || (bs.variants.type && bs.variants.type[m[2]]))[0]);
  if (!e.model && !e.textures) throw new Error(`нет варианта ${m[2]} в ${m[1]}`);
  write(`models/item/${id}.json`, itemLayer(e));
}

let nb = 0, ni = 0;
for (const [id, spec] of Object.entries(cfg.blocks || {})) { convertBlock(id, spec); nb++; }
for (const [id, src] of Object.entries(cfg.items || {})) { writeItem(id, src); ni++; }
console.log(`блоков: ${nb}, предметов: ${ni}, скопировано моделей оригинала: ${copied.size}`);
