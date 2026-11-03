// Обычные shaped/shapeless-рецепты и основная плавка из ConfigRecipes оригинала.
'use strict';
const P = require('./parse-java');
const Legacy = require('./legacy-recipe-items');

function shape(pattern, key) {
  let rows = pattern.slice();
  while (rows.length && !rows[0].trim()) rows.shift();
  while (rows.length && !rows.at(-1).trim()) rows.pop();
  const start = Math.min(...rows.map(r => r.search(/\S/))), end = Math.max(...rows.map(r => r.trimEnd().length));
  rows = rows.map(r => r.slice(start, end));
  return { ingredientMode: 'shaped', width: rows[0].length, height: rows.length,
    ingredients: rows.flatMap(r => [...r].map(c => c === ' ' ? [] : key[c])) };
}

function addReference(expected, manual) {
  function add(args, shaped, context = {}) {
    const name = context.name || (/^"[^"+]+"$/.test(args[0]) ? JSON.parse(args[0])
      : args[0].match(/^new ResourceLocation\("thaumcraft", "([^"]+)"\)$/)?.[1]);
    if (!name) { manual.push(args[0] + ': dynamic normal recipe'); return; }
    const id = 'thaumcraft:' + name.toLowerCase();
    try {
      const output = Legacy.stackOf(args[2], context), raw = args[3];
      if (!/^new Object\[\]\{/.test(raw)) throw Error('unsupported ingredient array');
      const rest = P.splitArgs(raw.slice(raw.indexOf('{') + 1, -1));
      const data = { kind: 'crafting', resultItem: output.id, count: output.count };
      if (shaped) {
        const pattern = [];
        while (rest.length && /^"[^"]*"$/.test(rest[0])) pattern.push(JSON.parse(rest.shift()));
        const key = {};
        for (let i = 0; i < rest.length; i += 2) key[rest[i].slice(1, -1)] = Legacy.ingredient(rest[i + 1], context);
        Object.assign(data, shape(pattern, key));
      } else Object.assign(data, { ingredientMode: 'unordered', ingredients: rest.map(x => Legacy.ingredient(x, context)) });
      expected[id] = data;
    } catch (e) { manual.push(id + ': normal recipe ' + e.message); }
  }
  for (const [call, shaped] of [['oreDictRecipe', true], ['shapelessOreDictRecipe', false], ['GameRegistry.addShapedRecipe', true]]) {
    for (const args of P.calls(P.read(), call)) {
      if (args[0].includes('d.getTranslationKey()')) {
        const prefix = JSON.parse(args[0].slice(0, args[0].indexOf('+')).trim());
        for (const color of Legacy.colors) add(args, shaped, { color, name: prefix + ({ light_blue: 'lightblue', light_gray: 'silver' }[color] || color) });
      } else if (args[0].includes('aspect.getTag()')) {
        if (args[2] !== 'output' || !args[3].includes('new IngredientNBTTC(ItemPhial.makeFilledPhial(aspect))')
            || !P.read().includes('setAspects(output, new AspectList().add(aspect, 1))')) throw Error('Unknown label loop');
        for (const tag of Object.values(Legacy.tags)) expected['thaumcraft:label_' + tag] = {
          kind: 'crafting', resultItem: Legacy.stackOf('new ItemStack(ItemsTC.label, 1, 1)').id,
          count: 1, resultAspects: { [tag]: 1 }, ingredientMode: 'unordered', ingredients: [
            { item: Legacy.stackOf('ItemsTC.label').id },
            { type: 'neoforge:components', items: 'thaumcraft:essentia_phial', components: { 'thaumcraft:aspects': [{ key: tag, amount: 10 }] } },
          ],
        };
      }
      else if (args[0].startsWith('"') || args[0].startsWith('new ResourceLocation(')) add(args, shaped);
    }
  }
  const smelt = { 'thaumcraft:cinnabar_ore': 'smelt_cinnabar_ore', 'thaumcraft:amber_ore': 'smelt_amber_ore', 'thaumcraft:quartz_ore': 'smelt_quartz_ore',
    'thaumcraft:greatwood_log': 'smelt_greatwood_log', 'thaumcraft:silverwood_log': 'smelt_silverwood_log', 'thaumcraft:iron_cluster': 'smelt_iron_cluster',
    'thaumcraft:gold_cluster': 'smelt_gold_cluster', 'thaumcraft:cinnabar_cluster': 'smelt_cinnabar_cluster', 'thaumcraft:quartz_cluster': 'smelt_quartz_cluster' };
  for (const a of P.calls(P.read(), 'GameRegistry.addSmelting')) {
    try {
      const input = Legacy.stackOf(a[0]), output = Legacy.stackOf(a[1]), name = smelt[input.id];
      if (!name) throw Error('unknown smelting input ' + input.id);
      expected['thaumcraft:' + name] = { kind: 'smelting', catalyst: { item: input.id }, resultItem: output.id, count: output.count,
        experience: parseFloat(a[2]), cookingTime: 200 };
    } catch (e) { manual.push('smelting: ' + e.message); }
  }
}

module.exports = { addReference, shape };
