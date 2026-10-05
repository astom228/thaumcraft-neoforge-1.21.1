#!/usr/bin/env node
// Перевод JSON исследований оригинала (assets/thaumcraft/research/*.json) под ID порта.
// Строки предметов "id;кол-во;meta;{nbt}" → "id[;кол-во][;{nbt}]": metadata заменяется отдельным ID (ID_MAPPING.md),
// числовые ID зачарований 1.12 → ID 1.21. Структура записей и все остальные поля — без изменений.
// Использование: node tools/research/convert-research.js
// Вход:  reference/src/thaumcraft/assets/thaumcraft/research/*.json
// Выход: src/main/resources/assets/thaumcraft/research/*.json
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const SRC = path.join(ROOT, 'reference/src/thaumcraft/assets/thaumcraft/research');
const OUT = path.join(ROOT, 'src/main/resources/assets/thaumcraft/research');

// Старый ID[#meta] → новый ID. Не указанные — ID не меняется.
const MAP = {
  'thaumcraft:amulet_vis#1': 'thaumcraft:vis_amulet',
  'thaumcraft:bottle_taint': 'thaumcraft:bottle_of_taint',
  'thaumcraft:brain': 'thaumcraft:zombie_brain',
  'thaumcraft:crystal_aer': 'thaumcraft:aer_crystal',
  'thaumcraft:crystal_aqua': 'thaumcraft:aqua_crystal',
  'thaumcraft:crystal_ignis': 'thaumcraft:ignis_crystal',
  'thaumcraft:crystal_terra': 'thaumcraft:terra_crystal',
  'thaumcraft:crystal_ordo': 'thaumcraft:ordo_crystal',
  'thaumcraft:crystal_perditio': 'thaumcraft:perditio_crystal',
  'thaumcraft:crystal_vitium': 'thaumcraft:vitium_crystal',
  'thaumcraft:curio#6': 'thaumcraft:crimson_rites',
  'thaumcraft:elemental_pick': 'thaumcraft:elemental_pickaxe',
  'thaumcraft:thaumium_pick': 'thaumcraft:thaumium_pickaxe',
  'thaumcraft:fortress_helm': 'thaumcraft:fortress_helmet',
  'thaumcraft:grapple_gun': 'thaumcraft:grappler',
  'thaumcraft:ingot#0': 'thaumcraft:thaumium_ingot',
  'thaumcraft:ingot#1': 'thaumcraft:void_metal_ingot',
  'thaumcraft:ingot#2': 'thaumcraft:brass_ingot',
  'thaumcraft:jar_brain': 'thaumcraft:brain_jar',
  'thaumcraft:jar_normal': 'thaumcraft:warded_jar',
  'thaumcraft:lamp_arcane': 'thaumcraft:arcane_lamp',
  'thaumcraft:lamp_fertility': 'thaumcraft:fertility_lamp',
  'thaumcraft:lamp_growth': 'thaumcraft:growth_lamp',
  'thaumcraft:log_greatwood': 'thaumcraft:greatwood_log',
  'thaumcraft:sapling_greatwood': 'thaumcraft:greatwood_sapling',
  'thaumcraft:sapling_silverwood': 'thaumcraft:silverwood_sapling',
  'thaumcraft:matrix_cost': 'thaumcraft:infusion_cost_stone',
  'thaumcraft:matrix_speed': 'thaumcraft:infusion_speed_stone',
  'thaumcraft:mind#0': 'thaumcraft:clockwork_mind',
  'thaumcraft:mind#1': 'thaumcraft:biothaumic_mind',
  'thaumcraft:mirror': 'thaumcraft:magic_mirror',
  'thaumcraft:mirror_essentia': 'thaumcraft:essentia_mirror',
  'thaumcraft:module#0': 'thaumcraft:vision_module',
  'thaumcraft:nugget#10': 'thaumcraft:rare_earths',
  'thaumcraft:paving_stone_barrier': 'thaumcraft:barrier_paving_stone',
  'thaumcraft:paving_stone_travel': 'thaumcraft:travel_paving_stone',
  'thaumcraft:phial#0': 'thaumcraft:phial',
  'thaumcraft:phial#1': 'thaumcraft:essentia_phial',
  'thaumcraft:plate#0': 'thaumcraft:brass_plate',
  'thaumcraft:plate#1': 'thaumcraft:iron_plate',
  'thaumcraft:plate#2': 'thaumcraft:thaumium_plate',
  'thaumcraft:plate#3': 'thaumcraft:void_metal_plate',
  'thaumcraft:seal#0': 'thaumcraft:blank_seal',
  'thaumcraft:smelter_aux': 'thaumcraft:auxiliary_slurry_pump',
  'thaumcraft:smelter_basic': 'thaumcraft:essentia_smeltery',
  'thaumcraft:smelter_thaumium': 'thaumcraft:thaumium_essentia_smeltery',
  'thaumcraft:smelter_vent': 'thaumcraft:auxiliary_venting_port',
  'thaumcraft:stone_ancient': 'thaumcraft:ancient_stone',
  'thaumcraft:stone_eldritch_tile': 'thaumcraft:eldritch_stone',
  'thaumcraft:tallow': 'thaumcraft:magic_tallow',
  'thaumcraft:tube': 'thaumcraft:essentia_tube',
  'thaumcraft:turret#0': 'thaumcraft:crossbow_turret',
  'thaumcraft:turret#1': 'thaumcraft:advanced_crossbow_turret',
  'thaumcraft:turret#2': 'thaumcraft:arcane_bore',
  'thaumcraft:wand_workbench': 'thaumcraft:focal_manipulator',
  // ванильные ID 1.12 → 1.21 (как DataFixer)
  'minecraft:dye#0': 'minecraft:ink_sac',
  'minecraft:dye#15': 'minecraft:bone_meal',
  'minecraft:hardened_clay': 'minecraft:terracotta',
  'minecraft:noteblock': 'minecraft:note_block',
  'minecraft:web': 'minecraft:cobweb',
};

// Предметы, которых в 1.12.2 уже не было: оригинал получал пустой стек и молча пропускал требование.
// Поведение сохраняется (DECISIONS.md) — строки удаляются.
const DEAD = new Set(['thaumcraft:leather', 'thaumcraft:metal#2', 'thaumcraft:metal#3', 'thaumcraft:nitor#4']);

// Исправленные опечатки (DEVIATIONS.md).
const TYPO = { 'minecraft:thaumcraft:enchanted_placeholder': 'thaumcraft:enchanted_placeholder' };

// Числовые ID зачарований 1.12 (Enchantment.REGISTRY), встречающиеся в данных.
const ENCH = { 0: 'minecraft:protection', 16: 'minecraft:sharpness', 33: 'minecraft:silk_touch', 35: 'minecraft:fortune' };

const ITEM_FIELDS_ENTRY = ['icons', 'reward_item'];
const ITEM_FIELDS_STAGE = ['required_item', 'required_craft'];
const warnings = [];

function convertItem(s, where) {
  if (s.includes('textures/') || s.startsWith('focus')) return s; // иконка-картинка / иконка фокуса
  const parts = s.split(';');
  let name = parts[0];
  if (TYPO[name]) name = TYPO[name];
  let num = -1, dam = -1, nbt = null;
  for (let a = 1; a < parts.length; a++) {
    if (parts[a].startsWith('{')) { nbt = parts.slice(a).join(';'); break; }
    const q = parseInt(parts[a], 10);
    if (isNaN(q)) continue;
    if (q >= 0 && num < 0) num = q; else if (q >= 0 && dam < 0) dam = q;
  }
  if (num < 0) num = 1;
  if (dam < 0) dam = 0;
  const key = `${name}#${dam}`;
  if (DEAD.has(key) || DEAD.has(name)) return null;
  let id = MAP[key] || (dam === 0 && MAP[name]) || null;
  if (!id) {
    if (dam !== 0) warnings.push(`${where}: нет соответствия для ${key}`);
    id = name;
  }
  if (nbt) {
    nbt = nbt.replace(/\{id:(\d+)s,/g, (m, n) => {
      if (!ENCH[n]) warnings.push(`${where}: неизвестное зачарование ${n}`);
      return `{id:'${ENCH[n] || n}',`;
    });
  }
  let out = id;
  if (num !== 1 || nbt) out += ';' + num;
  if (nbt) out += ';' + nbt;
  return out;
}

function convertList(obj, field, where) {
  if (!Array.isArray(obj[field])) return;
  const out = [];
  for (const s of obj[field]) {
    const c = convertItem(s, `${where}.${field}`);
    if (c !== null) out.push(c);
  }
  if (out.length > 0) obj[field] = out; else delete obj[field];
}

fs.mkdirSync(OUT, { recursive: true });
for (const f of fs.readdirSync(SRC).filter(f => f.endsWith('.json')).sort()) {
  const data = JSON.parse(fs.readFileSync(path.join(SRC, f), 'utf8'));
  for (const e of data.entries) {
    for (const field of ITEM_FIELDS_ENTRY) convertList(e, field, e.key);
    (e.stages || []).forEach((st, i) => {
      for (const field of ITEM_FIELDS_STAGE) convertList(st, field, `${e.key}.stage${i + 1}`);
    });
  }
  fs.writeFileSync(path.join(OUT, f), JSON.stringify(data, null, 2) + '\n');
  console.log(`${f}: ${data.entries.length} записей`);
}
if (warnings.length) {
  console.log('Предупреждения:\n  ' + warnings.join('\n  '));
  process.exitCode = 1;
}
