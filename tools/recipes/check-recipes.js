// Сверка чисел оригинальных рецептов с серверным дампом.
// node tools/recipes/check-recipes.js [build/recipe-audit-actual.json]
// Развёрнутые циклы и особые классы перечисляются отдельно, а не считаются автоматически проверенными.
'use strict';
const fs = require('fs');
const path = require('path');
const P = require('./parse-java');
const root = path.resolve(__dirname, '../..');
const tags = {};
for (const m of fs.readFileSync(path.join(root, 'src/main/java/thaumcraft/api/aspects/Aspect.java'), 'utf8')
  .matchAll(/public static final Aspect (\w+) = new Aspect\("(\w+)"/g)) tags[m[1]] = m[2];

const expected = {};
const manual = [];
function aspects(expression) {
  if (!/^new AspectList\(\)/.test(expression.trim())) throw Error('computed aspects');
  const result = {};
  for (const m of expression.matchAll(/\.(add|merge)\(Aspect\.(\w+),\s*(\d+)\)/g)) {
    if (!tags[m[2]]) throw Error('unknown aspect ' + m[2]);
    const amount = Number(m[3]), tag = tags[m[2]];
    result[tag] = m[1] === 'add' ? (result[tag] || 0) + amount : Math.max(result[tag] || 0, amount);
  }
  if (expression.replace(/new AspectList\(\)|\s|\.(?:add|merge)\(Aspect\.\w+,\s*\d+\)/g, '')) throw Error('computed aspects');
  return result;
}

for (const [call, kind] of [['ThaumcraftApi.addInfusionCraftingRecipe', 'infusion'], ['ThaumcraftApi.addCrucibleRecipe', 'crucible']]) {
  for (const args of P.calls(P.read(), call)) {
    const idMatch = args[0].match(/^new ResourceLocation\("thaumcraft:([^"+]+)"\)$/);
    if (!idMatch) {
      if (kind === 'crucible' && args[0].includes('"thaumcraft:vis_crystal_" + aspect.getTag()')) {
        const a = P.splitArgs(P.balanced(args[1], args[1].indexOf('(')).inner);
        const cost = a[3].match(/^new AspectList\(\)\.add\(aspect,\s*(\d+)\)$/);
        if (!cost || a[1] !== 'ThaumcraftApiHelper.makeCrystal(aspect)') throw Error('Unknown crystal loop');
        for (const tag of Object.values(tags)) expected['thaumcraft:vis_crystal_' + tag] = {
          kind: 'crucible', research: JSON.parse(a[0]), aspects: { [tag]: Number(cost[1]) }, count: 1,
        };
      } else manual.push(args[0] + ': dynamic ID');
      continue;
    }
    const id = 'thaumcraft:' + idMatch[1].toLowerCase();
    const expr = args[1];
    const definition = 'InfusionEnchantmentRecipe ' + expr + ' = new InfusionEnchantmentRecipe(';
    const index = P.read().indexOf(definition);
    if (kind === 'infusion' && index >= 0) {
      const a = P.splitArgs(P.balanced(P.read(), index + definition.length - 1).inner);
      const source = fs.readFileSync(path.join(root, 'reference/src-mcp/thaumcraft/thaumcraft/common/lib/crafting/InfusionEnchantmentRecipe.java'), 'utf8');
      const instability = source.match(/super\(ench\.research, null, (\d+), as,/);
      if (!instability) throw Error('Unknown enchantment constructor');
      expected[id] = { kind: 'enchantment', enchantment: a[0].split('.').at(-1), instability: Number(instability[1]),
        aspects: aspects(a[1]), components: a.length - 2 };
      continue;
    }
    if (id === 'thaumcraft:runicarmor') {
      const source = fs.readFileSync(path.join(root, 'reference/src-mcp/thaumcraft/thaumcraft/common/lib/crafting/InfusionRunicAugmentRecipe.java'), 'utf8');
      if (!source.includes('20 + (int)(20.0 * Math.pow(2.0, PlayerEvents.getRunicCharge(input)))')
          || !source.includes('5 + PlayerEvents.getRunicCharge(input) / 2')) throw Error('Unknown runic formula');
      expected[id] = { kind: 'runic', samples: Array.from({length: 7}, (_, charge) => {
        const cost = 20 + 20 * 2 ** charge;
        return { charge, instability: 5 + Math.floor(charge / 2), aspects: { praemunio: cost, vitreus: cost / 2, potentia: cost / 2 },
          components: charge + 2, outputCharge: charge + 1 };
      }) };
      continue;
    }
    if (!expr.startsWith(kind === 'infusion' ? 'new InfusionRecipe(' : 'new CrucibleRecipe(')) {
      manual.push(id + ': special recipe class'); continue;
    }
    const a = P.splitArgs(P.balanced(expr, expr.indexOf('(')).inner);
    try {
      const data = { kind, research: JSON.parse(a[0]), aspects: aspects(a[3]) };
      if (kind === 'infusion') {
        if (!/^\d+$/.test(a[2])) throw Error('computed instability');
        data.instability = Number(a[2]);
        const inputs = a.slice(5).flatMap(x => /^new Object\[\]\{/.test(x) ? P.splitArgs(x.slice(x.indexOf('{') + 1, -1)) : [x]);
        data.components = inputs.length;
      }
      if (a[1].startsWith('new ItemStack(')) {
        const output = P.splitArgs(P.balanced(a[1], a[1].indexOf('(')).inner);
        if (output[1] && !/^\d+$/.test(output[1])) throw Error('computed output count');
        data.count = output[1] ? Number(output[1]) : 1;
      }
      expected[id] = data;
    } catch (e) { manual.push(id + ': ' + e.message); }
  }
}

// Магические рецепты получены convert-arcane.js непосредственно из initializeArcaneRecipes.
for (const file of JSON.parse(fs.readFileSync(path.join(__dirname, 'arcane-generated.json'), 'utf8'))) {
  const json = JSON.parse(fs.readFileSync(path.join(root, 'src/main/resources/data/thaumcraft/recipe', file), 'utf8'));
  expected['thaumcraft:' + file.replace(/\.json$/, '')] = {
    kind: 'arcane', research: json.research, vis: json.vis, aspects: json.crystals || {}, count: json.result.count || 1,
  };
}
fs.mkdirSync(path.join(root, 'build'), { recursive: true });
fs.writeFileSync(path.join(root, 'build/recipe-audit-expected.json'), JSON.stringify(expected, null, 2) + '\n');
fs.writeFileSync(path.join(root, 'build/recipe-audit-manual.txt'), manual.join('\n') + '\n');
const counts = {};
for (const data of Object.values(expected)) counts[data.kind] = (counts[data.kind] || 0) + 1;
console.log('Numeric reference:', JSON.stringify(counts), 'manual expressions:', manual.length);

if (process.argv[2]) {
  const actual = JSON.parse(fs.readFileSync(path.resolve(root, process.argv[2]), 'utf8'));
  const canonical = value => Array.isArray(value) ? value.map(canonical) : value && typeof value === 'object'
    ? Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k])])) : value;
  const problems = [];
  for (const [id, reference] of Object.entries(expected)) {
    if (!actual[id]) { problems.push(id + ': missing recipe'); continue; }
    for (const [key, value] of Object.entries(reference)) {
      if (JSON.stringify(canonical(value)) !== JSON.stringify(canonical(actual[id][key]))) {
        problems.push(id + ': ' + key + ' expected ' + JSON.stringify(value) + ', actual ' + JSON.stringify(actual[id][key]));
      }
    }
  }
  fs.writeFileSync(path.join(root, 'build/recipe-audit-differences.txt'), problems.join('\n') + '\n');
  problems.forEach(p => console.error(p));
  console.log('Compared:', Object.keys(expected).length, 'differences:', problems.length);
  if (problems.length) process.exitCode = 1;
}
