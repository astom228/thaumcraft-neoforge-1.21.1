// Магические рецепты оригинала (ConfigRecipes.initializeArcaneRecipes) -> JSON рецептов 1.21 (thaumcraft:arcane_shaped/shapeless).
//   node tools/recipes/convert-arcane.js
// Пишутся только рецепты, все предметы которых уже зарегистрированы в порте (поля ItemsTC/BlocksTC с теми же именами,
// что в оригинале). Остальные — в tools/recipes/arcane-pending.txt; после переноса их предметов скрипт запускается снова.
// Список записанных файлов — tools/recipes/arcane-generated.json (при перезапуске старые файлы удаляются).
const fs = require('fs');
const path = require('path');
const P = require('./parse-java');

const ROOT = path.join(__dirname, '../..');
const OUT = path.join(ROOT, 'src/main/resources/data/thaumcraft/recipe');
const MANIFEST = path.join(__dirname, 'arcane-generated.json');
const PENDING = path.join(__dirname, 'arcane-pending.txt');

// --- поля ItemsTC / BlocksTC порта -> ID (первая строка-литерал в объявлении) ---
function fieldIds(file) {
  const src = fs.readFileSync(path.join(ROOT, file), 'utf8');
  const map = {};
  const re = /public static final Deferred\w+<[^>]+> (\w+) = [^;]*?"([a-z0-9_]+)"/g;
  let m;
  while ((m = re.exec(src))) map[m[1]] = m[2];
  return map;
}
const ITEMS = fieldIds('src/main/java/thaumcraft/api/items/ItemsTC.java');
const BLOCKS = fieldIds('src/main/java/thaumcraft/api/blocks/BlocksTC.java');
// ID, зарегистрированные картами по цвету (свечи, ниторы…) — "<цвет>_<имя>"
const BLOCKS_SRC = fs.readFileSync(path.join(ROOT, 'src/main/java/thaumcraft/api/blocks/BlocksTC.java'), 'utf8');
const COLOR_MAPS = {};
for (const m of BLOCKS_SRC.matchAll(/(\w+)\.put\(dye, register\(dye\.getSerializedName\(\) \+ "(_[a-z_]+)"/g)) COLOR_MAPS[m[1]] = m[2];

// Аспекты: поле Aspect.X -> тег
const ASPECTS = {};
for (const m of fs.readFileSync(path.join(ROOT, 'src/main/java/thaumcraft/api/aspects/Aspect.java'), 'utf8')
  .matchAll(/public static final Aspect (\w+) = new Aspect\("(\w+)"/g)) ASPECTS[m[1]] = m[2];

// все зарегистрированные ID порта (включая карты по цвету)
const REGISTERED = new Set([...Object.values(ITEMS), ...Object.values(BLOCKS)]);
for (const suffix of Object.values(COLOR_MAPS)) {
  for (const d of ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue',
    'brown', 'green', 'red', 'black']) REGISTERED.add(d + suffix);
}

// Варианты metadata оригинала -> ID порта (ID_MAPPING.md); рецепт пишется, когда такой ID зарегистрирован
const VARIANTS = {
  'ItemsTC.nuggets': { 9: 'quartz_sliver', 10: 'rare_earths' },
  'ItemsTC.modules': { 0: 'vision_module', 1: 'aggression_module' },
  'ItemsTC.mind': { 0: 'clockwork_mind', 1: 'biothaumic_mind' },
  'ItemsTC.filter': { 0: 'essentia_filter' },
  'BlocksTC.tube': { 0: 'essentia_tube' },
  'ItemsTC.baubles': { 0: 'mundane_amulet', 1: 'mundane_ring', 2: 'mundane_belt', 3: 'apprentice_ring', 4: 'fancy_amulet', 5: 'fancy_ring', 6: 'fancy_belt' },
  'ItemsTC.amuletVis': { 0: 'vis_stone', 1: 'vis_amulet' },
  'ItemsTC.turretPlacer': { 0: 'crossbow_turret', 1: 'advanced_crossbow_turret' },
};

// Цвета 1.12 (EnumDyeColor, порядок = metadata шерсти) -> 1.21
const DYES = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown',
  'green', 'red', 'black'];
const DYE_KEYS = ['white', 'orange', 'magenta', 'lightBlue', 'yellow', 'lime', 'pink', 'gray', 'silver', 'cyan', 'purple', 'blue', 'brown',
  'green', 'red', 'black'];

// Ванильные блоки/предметы 1.12 -> ID 1.21 (поле в нижнем регистре, кроме переименованных)
const VANILLA_RENAMED = { STONE_SLAB: 'smooth_stone_slab', TRAPDOOR: 'oak_trapdoor' };
const vanilla = (field) => 'minecraft:' + (VANILLA_RENAMED[field] || field.toLowerCase());

// OreDictionary -> ингредиент (DECISIONS.md п. 27: категории -> теги, имена конкретных блоков -> те же предметы, что у Forge 1.12)
const STAINED = (kind) => DYES.map((d) => ({ item: `minecraft:${d}_stained_${kind}` }));
const ORE = {
  plankWood: { tag: 'minecraft:planks' },
  slabWood: { tag: 'minecraft:wooden_slabs' },
  stickWood: { tag: 'c:rods/wooden' },
  stone: { item: 'minecraft:stone' },
  cobblestone: { item: 'minecraft:cobblestone' },
  workbench: { item: 'minecraft:crafting_table' },
  blockGlass: [{ item: 'minecraft:glass' }, ...STAINED('glass')],
  paneGlass: [{ item: 'minecraft:glass_pane' }, ...STAINED('glass_pane')],
  blockRedstone: { tag: 'c:storage_blocks/redstone' },
  dustRedstone: { tag: 'c:dusts/redstone' },
  gemDiamond: { tag: 'c:gems/diamond' },
  gemQuartz: { tag: 'c:gems/quartz' },
  gemAmber: { tag: 'c:gems/amber' },
  ingotGold: { tag: 'c:ingots/gold' },
  ingotIron: { tag: 'c:ingots/iron' },
  ingotBrass: { tag: 'c:ingots/brass' },
  nuggetBrass: { tag: 'c:nuggets/brass' },
  nuggetQuicksilver: { tag: 'c:nuggets/quicksilver' },
  plateBrass: { tag: 'c:plates/brass' },
  plateIron: { tag: 'c:plates/iron' },
  plateThaumium: { tag: 'c:plates/thaumium' },
  plateVoid: { tag: 'c:plates/void' },
  leather: { tag: 'c:leathers' },
  string: { tag: 'c:strings' },
  dyeRed: { tag: 'c:dyes/red' },
  nitor: { tag: 'thaumcraft:nitor' },
};

class Missing extends Error {}

// Поле ItemsTC/BlocksTC -> ID порта (или Missing)
function tcId(ref) {
  const [cls, field] = ref.split('.');
  const id = (cls === 'ItemsTC' ? ITEMS : BLOCKS)[field];
  if (!id) throw new Missing(ref);
  return 'thaumcraft:' + id;
}

// Выражение-предмет -> { id, count }
function stackOf(e, ctx) {
  e = e.trim();
  let m;
  if ((m = e.match(/^(ItemsTC|BlocksTC)\.(\w+)$/))) return { id: tcId(e), count: 1 };
  if ((m = e.match(/^(Blocks|Items)\.(\w+)$/))) return { id: vanilla(m[2]), count: 1 };
  if ((m = e.match(/^Ingredient\.fromItem\((.+)\)$/))) return stackOf(m[1], ctx);
  if (e === 'banner') return { id: ctx.banner, count: 1 };
  if ((m = e.match(/^new ItemStack\((.+)\)$/))) {
    const a = P.splitArgs(m[1]);
    const count = a[1] ? Number(a[1]) : 1;
    const meta = a[2];
    let ref = a[0];
    if (ref === 'Blocks.WOOL') {
      if (meta === '32767') return { tag: 'minecraft:wool', count };
      const idx = meta === 'a' ? ctx.dyeIndex : Number(meta);
      return { id: `minecraft:${DYES[idx]}_wool`, count };
    }
    if (meta !== undefined && meta !== '32767') {
      const v = VARIANTS[ref];
      if (!v || !(Number(meta) in v)) throw new Error(`нет варианта ${ref} meta ${meta}`);
      const vid = v[Number(meta)];
      if (!REGISTERED.has(vid)) throw new Missing(`${vid} (${ref} meta ${meta})`);
      return { id: 'thaumcraft:' + vid, count };
    }
    const s = stackOf(ref, ctx);
    return { ...s, count };
  }
  throw new Error('неизвестное выражение: ' + e);
}

function ingredient(e, ctx) {
  e = e.trim();
  let m;
  if ((m = e.match(/^"(\w+)"$/))) {
    const o = ORE[m[1]];
    if (!o) throw new Error('нет соответствия OreDictionary: ' + m[1]);
    return o;
  }
  const s = stackOf(e, ctx);
  return s.tag ? { tag: s.tag } : { item: s.id };
}

function aspects(e) {
  if (e.trim() === 'null') return null;
  const out = {};
  for (const m of e.matchAll(/\.add\(Aspect\.(\w+), (\d+)\)/g)) out[ASPECTS[m[1]]] = Number(m[2]);
  return out;
}

function group(e) {
  if (e === 'defaultGroup') return '';
  if (e === 'bannerGroup') return 'thaumcraft:banners';
  throw new Error('неизвестная группа ' + e);
}

function convert(recipeExpr, ctx) {
  const type = recipeExpr.match(/^new (Shaped|Shapeless)ArcaneRecipe\(/)[1];
  const a = P.splitArgs(P.balanced(recipeExpr, recipeExpr.indexOf('(')).inner);
  const json = { type: `thaumcraft:arcane_${type.toLowerCase()}` };
  const g = group(a[0]);
  if (g) json.group = g;
  json.research = JSON.parse(a[1]);
  json.vis = Number(a[2]);
  const cr = aspects(a[3]);
  if (cr) json.crystals = cr;
  const res = stackOf(a[4], ctx);
  // в оригинале варианты-аргументы (Object[]) раскрываются
  let rest = [];
  for (const x of a.slice(5)) {
    const om = x.match(/^new Object\[\]\{(.*)\}$/s);
    if (om) rest.push(...P.splitArgs(om[1]));
    else rest.push(x);
  }
  if (type === 'Shaped') {
    const pattern = [];
    while (rest.length && /^"[^"]*"$/.test(rest[0])) pattern.push(JSON.parse(rest.shift()));
    const key = {};
    for (let i = 0; i < rest.length; i += 2) key[rest[i].slice(1, -1)] = ingredient(rest[i + 1], ctx);
    json.key = key;
    json.pattern = pattern;
  } else {
    json.ingredients = rest.map((x) => ingredient(x, ctx));
  }
  json.result = { id: res.id };
  if (res.count !== 1) json.result.count = res.count;
  return json;
}

// --- сбор рецептов ---
const body = P.methodBody(P.read(), 'initializeArcaneRecipes');
const list = [];
for (const r of P.calls(body, 'ThaumcraftApi.addArcaneCraftingRecipe')) {
  if (r[0].includes('"thaumcraft:Banner" +')) {
    DYES.forEach((d, i) => list.push({ id: 'banner' + DYE_KEYS[i].toLowerCase(), expr: r[1], ctx: { dyeIndex: i, banner: null, dye: d } }));
    continue;
  }
  list.push({ id: r[0].match(/"thaumcraft:(.+)"/)[1].toLowerCase(), expr: r[1], ctx: {} });
}

// баннеры — BlocksTC.banners (по цвету)
for (const e of list) {
  if (e.ctx.dye !== undefined) {
    if (!COLOR_MAPS.banners) e.ctx.missing = 'BlocksTC.banners';
    else e.ctx.banner = `thaumcraft:${e.ctx.dye}${COLOR_MAPS.banners}`;
  }
}

if (fs.existsSync(MANIFEST)) for (const f of JSON.parse(fs.readFileSync(MANIFEST, 'utf8'))) fs.rmSync(path.join(OUT, f), { force: true });
fs.mkdirSync(OUT, { recursive: true });
const written = [];
const pending = [];
for (const e of list) {
  try {
    if (e.ctx.missing) throw new Missing(e.ctx.missing);
    const json = convert(e.expr, e.ctx);
    const file = e.id + '.json';
    fs.writeFileSync(path.join(OUT, file), JSON.stringify(json, null, 2) + '\n');
    written.push(file);
  } catch (err) {
    if (!(err instanceof Missing)) throw err;
    pending.push(`${e.id}: нет ${err.message}`);
  }
}
fs.writeFileSync(MANIFEST, JSON.stringify(written, null, 2) + '\n');
fs.writeFileSync(PENDING, '# Магические рецепты, ожидающие предметов (tools/recipes/convert-arcane.js)\n' + pending.join('\n') + '\n');
console.log(`рецептов: ${list.length}, записано: ${written.length}, ожидают: ${pending.length}`);
