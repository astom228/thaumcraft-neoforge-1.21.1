// Дополнительные формы выражений оригинала для проверки ингредиентов (не генератор новых рецептов).
'use strict';
const Legacy = require('./convert-arcane');
const P = require('./parse-java');
const fs = require('fs');
const path = require('path');
const tags = {};
for (const m of fs.readFileSync(path.join(__dirname, '../../src/main/java/thaumcraft/api/aspects/Aspect.java'), 'utf8')
  .matchAll(/public static final Aspect (\w+) = new Aspect\("(\w+)"/g)) tags[m[1]] = m[2];
const variants = {
  'ItemsTC.ingots': ['thaumium_ingot', 'void_metal_ingot', 'brass_ingot'],
  'ItemsTC.clusters': ['iron_cluster', 'gold_cluster', 'copper_cluster', 'tin_cluster', 'silver_cluster', 'lead_cluster', 'cinnabar_cluster', 'quartz_cluster'],
  'ItemsTC.nuggets': ['minecraft:iron_nugget', 'copper_nugget', 'tin_nugget', 'silver_nugget', 'lead_nugget', 'quicksilver_drop', 'thaumium_nugget', 'void_metal_nugget', 'brass_nugget', 'quartz_sliver', 'rare_earths'],
  'ItemsTC.plates': ['brass_plate', 'iron_plate', 'thaumium_plate', 'void_metal_plate'],
  'ItemsTC.plate': ['brass_plate', 'iron_plate', 'thaumium_plate', 'void_metal_plate'],
  'ItemsTC.label': ['label', 'marked_label'],
};
const seals = { pickup: 'collect_seal', pickup_advanced: 'advanced_collect_seal', fill: 'store_seal', fill_advanced: 'advanced_store_seal',
  empty: 'empty_seal', empty_advanced: 'advanced_empty_seal', provider: 'provide_seal', stock: 'stock_seal', guard: 'guard_seal',
  guard_advanced: 'advanced_guard_seal', harvest: 'harvest_seal', butcher: 'butcher_seal', breaker: 'breaker_seal', lumber: 'lumberjack_seal', use: 'use_seal' };
const dyes = ['ink_sac', 'red_dye', 'green_dye', 'cocoa_beans', 'lapis_lazuli', 'purple_dye', 'cyan_dye', 'light_gray_dye',
  'gray_dye', 'pink_dye', 'lime_dye', 'yellow_dye', 'light_blue_dye', 'magenta_dye', 'orange_dye', 'bone_meal'];
const renamed = { REEDS: 'sugar_cane', WEB: 'cobweb', SKULL: 'skeleton_skull', NETHERBRICK: 'nether_brick', FISH: 'cod', COOKED_FISH: 'cooked_cod', MELON_BLOCK: 'melon' };
const id = value => value.includes(':') ? value : 'thaumcraft:' + value;
const colors = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple', 'blue', 'brown', 'green', 'red', 'black'];

function stackOf(expression, context = {}) {
  const e = expression.trim();
  let m;
  if ((m = e.match(/^GolemHelper\.getSealStack\("thaumcraft:(\w+)"\)$/))) {
    if (!seals[m[1]]) throw Error('unknown seal ' + m[1]);
    return { id: id(seals[m[1]]), count: 1 };
  }
  if ((m = e.match(/^BlocksTC\.nitor\.get\(EnumDyeColor\.(\w+)\)$/))) return { id: id(m[1].toLowerCase() + '_nitor'), count: 1 };
  if ((m = e.match(/^BlocksTC\.candles\.get\(EnumDyeColor\.(\w+)\)$/))) return { id: id(m[1].toLowerCase() + '_candle'), count: 1 };
  if ((m = e.match(/^BlocksTC\.(nitor|candles)\.get\(d\)$/)) && context.color) return { id: id(context.color + (m[1] === 'nitor' ? '_nitor' : '_candle')), count: 1 };
  if (e === 'FluidUtil.getFilledBucket(new FluidStack(ConfigBlocks.FluidDeath.instance, 1000))') return { id: id('liquid_death_bucket'), count: 1 };
  if (/^\w+$/.test(e)) {
    const marker = new RegExp('ItemStack\\s+' + e + '\\s*=\\s*new ItemStack\\(').exec(P.read());
    if (marker) return stackOf('new ItemStack(' + P.balanced(P.read(), marker.index + marker[0].length - 1).inner + ')', context);
  }
  if (e.startsWith('new ItemStack(')) {
    const a = P.splitArgs(P.balanced(e, e.indexOf('(')).inner), count = a[1] ? Number(a[1]) : 1, meta = a[2] ? Number(a[2]) : 0;
    if (variants[a[0]] && meta !== 32767) {
      const value = variants[a[0]][meta];
      if (!value) throw Error('unknown metadata ' + e);
      return { id: id(value), count };
    }
    if (a[0] === 'Items.DYE' && dyes[meta]) return { id: 'minecraft:' + dyes[meta], count };
    if (a[0] === 'Items.SKULL' && meta === 1) return { id: 'minecraft:wither_skeleton_skull', count };
    if (a[0] === 'Items.GOLDEN_APPLE' && meta === 1) return { id: 'minecraft:enchanted_golden_apple', count };
    if (a[0] === 'Items.COAL' && meta === 32767) return { tag: 'minecraft:coals', count };
    if (a[0] === 'Items.COAL' && meta === 1) return { id: 'minecraft:charcoal', count };
    if (a[0] === 'Items.FISH' && meta === 32767) return { tag: 'minecraft:fishes', count };
    if (meta === 0 && /^(Items|Blocks)\./.test(a[0]) && renamed[a[0].split('.')[1]]) return { id: 'minecraft:' + renamed[a[0].split('.')[1]], count };
    if (meta === 0) {
      try { return Legacy.stackOf(e, {}); } catch (_) { /* составное выражение разбирается ниже */ }
      const inner = a[0].replace(/^\(Block\)/, '');
      return { ...stackOf(inner, context), count };
    }
  }
  return Legacy.stackOf(e, {});
}

function ingredient(expression, context = {}) {
  const e = expression.trim();
  let m;
  if ((m = e.match(/^"ore([A-Z]\w*)"$/))) return { tag: 'c:ores/' + m[1].toLowerCase() };
  if (e === '"dustGlowstone"') return { tag: 'c:dusts/glowstone' };
  if ((m = e.match(/^"(ingot|nugget|plate|gem|dust)([A-Z]\w*)"$/))) return { tag: 'c:' + ({ ingot: 'ingots', nugget: 'nuggets', plate: 'plates', gem: 'gems', dust: 'dusts' })[m[1]] + '/' + m[2].toLowerCase() };
  if ((m = e.match(/^"dye([A-Z]\w*)"$/))) return { tag: 'c:dyes/' + m[1].toLowerCase() };
  if (e === '"plankWood"') return { tag: 'minecraft:planks' };
  if (e === '"slabWood"') return { tag: 'minecraft:wooden_slabs' };
  if (e === '"slimeball"') return { tag: 'c:slime_balls' };
  if (e === 'ConfigAspects.dyes[15 - a]' && context.color) return { tag: 'c:dyes/' + context.color };
  if (e === 'ingredientsFromBlocks(BlocksTC.candles.values().toArray(new Block[0]))') return colors.map(c => ({ item: 'thaumcraft:' + c + '_candle' }));
  if ((m = e.match(/^ThaumcraftApiHelper\.makeCrystal\(Aspect\.(\w+)\)$/)) || (m = e.match(/^ConfigItems\.(\w+)_CRYSTAL$/))) {
    if (!tags[m[1]]) throw Error('unknown crystal ' + m[1]);
    return { type: 'neoforge:components', items: 'thaumcraft:crystal_essence', components: { 'thaumcraft:aspects': [{ key: tags[m[1]], amount: 1 }] } };
  }
  if ((m = e.match(/^ItemPhial\.makeFilledPhial\(Aspect\.(\w+)\)$/))) return {
    type: 'neoforge:components', items: 'thaumcraft:essentia_phial', components: { 'thaumcraft:aspects': [{ key: tags[m[1]], amount: 10 }] },
  };
  if (e === 'Ingredient.fromStacks(nitorStacks)') return { tag: 'thaumcraft:nitor' };
  if (/^\w+$/.test(e)) {
    const potion = new RegExp('\\b' + e + '\\.setTagInfo\\("Potion", new NBTTagString\\("([^"\\n]+)"\\)\\)').exec(P.read());
    if (potion) return { type: 'neoforge:components', items: 'minecraft:potion', components: { 'minecraft:potion_contents': { potion: potion[1] } } };
  }
  if (e.startsWith('"')) return Legacy.ingredient(e, {});
  const value = stackOf(e, context);
  return value.tag ? { tag: value.tag } : { item: value.id };
}

module.exports = { stackOf, ingredient, colors, tags };
