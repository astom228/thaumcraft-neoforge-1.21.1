// Конвертация blockstates/моделей оригинала (1.12, в т.ч. forge_marker) в формат 1.21.
//   node tools/assets/convert-assets.js
// Список того, что конвертировать, — tools/assets/assets.json (пополняется по этапам):
//   blocks: { "<новый id>": { "src": "<имя blockstate оригинала>", "props": {...}, "ignore": [...], "type": "slab|...", "item": false } }
//   items:  { "<новый id>": "<источник>" }  источник: "bs:<blockstate>#<вариант>" (вариант предмета из blockstate)
//                                                  или "model:<имя файла models/item>" (копия модели предмета).
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
function copyBlockModel(ref) {
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
    copyBlockModel(ref);
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

function convertBlock(id, spec) {
  const bs = readSrc(`blockstates/${spec.src}.json`);
  const ignore = new Set(spec.ignore || []);
  const propMap = spec.props || {};
  const models = new Map();
  const defaults = bs.defaults || {};
  const variants = {};
  let inventory = null;

  const simpleKeys = Object.keys(bs.variants).filter((k) => k === 'normal' || k === 'inventory' || k.includes('='));
  const propKeys = bs.forge_marker ? Object.keys(bs.variants).filter((k) => !simpleKeys.includes(k) && !ignore.has(k)) : [];
  // игнорируемые свойства: в ключ не попадают, но их переопределения (первое значение) применяются
  const ignoredOver = bs.forge_marker ? Object.keys(bs.variants).filter((k) => !simpleKeys.includes(k) && ignore.has(k)).map((k) => Object.values(bs.variants[k])[0]) : [];

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
  if (spec.itemModel) return writeItem(id, spec.itemModel);
  if (inventory && (inventory.model === 'forge:item-layer' || (inventory.model || '').startsWith('item/'))) {
    write(`models/item/${id}.json`, itemLayer(inventory));
  } else {
    const inv = inventory && Object.keys(inventory).length ? inventory : null;
    let parent;
    if (inv && (inv.model || inv.textures)) parent = toVariant(id, inv, models).model;
    else parent = asList(Object.values(variants)[0])[0].model;
    write(`models/item/${id}.json`, { parent });
  }
}

function itemLayer(e) {
  const tool = e.transform === 'forge:default-tool';
  return { parent: tool ? 'minecraft:item/handheld' : 'minecraft:item/generated', textures: e.textures };
}

function writeItem(id, src) {
  if (src.startsWith('model:')) {
    const model = readSrc(`models/item/${src.slice(6)}.json`);
    if (model.parent) model.parent = model.parent.includes(':') ? model.parent : 'minecraft:' + model.parent;
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
