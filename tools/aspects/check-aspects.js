// Сопоставление привязок и выражений аспектов оригинала и порта. Неподдержанные динамические формы перечисляются явно.
'use strict';
const fs = require('fs');
const path = require('path');
const P = require('../recipes/parse-java');
const Items = require('../recipes/legacy-recipe-items');
const root = path.resolve(__dirname, '../..');
const old = fs.readFileSync(path.join(root, 'reference/src-mcp/thaumcraft/thaumcraft/common/config/ConfigAspects.java'), 'utf8');
const current = fs.readFileSync(path.join(root, 'src/main/java/thaumcraft/common/config/ConfigAspects.java'), 'utf8');
const manual = [];
const excluded = [];

function body(source, name) {
  return P.methodBody(source.replace(new RegExp('(?:private\\s+)?static void ' + name, 'g'), 'public static void ' + name), name);
}

function subjects(expression, context = '') {
  const e = expression.trim();
  if (/^"/.test(e)) return ['ore:' + JSON.parse(e)];
  let m;
  if (/^stack[234]?$|^sis$/.test(e)) return ['dynamic:' + e];
  if (e === 'tag(c("gears"))') return ['ore:gear*'];
  if (/^new ItemStack\(ca(?:\.get\(\))?\)$/.test(e)) return ['dynamic:' + (context.includes('ELDRITCH') ? 'banners' : 'candles')];
  if (e === 'T(ca)') return ['dynamic:banners'];
  if ((m = e.match(/^O\((.+)\)$/s))) return ['ore:' + JSON.parse(m[1])];
  if ((m = e.match(/^L\((.+)\)$/s))) {
    const a = P.splitArgs(m[1]);
    return ['vanilla:' + JSON.parse(a[0]) + '#' + a[1].replace('LegacyItems.WILDCARD', '32767')];
  }
  if ((m = e.match(/^T\((.+)\)$/s))) return P.splitArgs(m[1]).map(s => 'item:thaumcraft:' + JSON.parse(s)).sort();
  if (e.startsWith('new ItemStack(')) {
    const a = P.splitArgs(P.balanced(e, e.indexOf('(')).inner);
    const meta = a[2] || '0';
    if (/^(Items|Blocks)\.\w+$/.test(a[0])) {
      const field = a[0].split('.')[1];
      return ['vanilla:' + ({ OAK_DOOR: 'wooden_door', POTIONITEM: 'potion' }[field] || field.toLowerCase()) + '#' + meta];
    }
    if (meta === '32767') {
      const found = new Set();
      for (let i = 0; i < 16; i++) {
        try { found.add('item:' + Items.stackOf('new ItemStack(' + a[0] + ', 1, ' + i + ')').id); } catch (_) { /* нет варианта */ }
      }
      if (found.size) return [...found].sort();
    }
  }
  return ['item:' + Items.stackOf(e).id];
}

function rewrite(text, name, action) {
  let i = 0;
  while ((i = text.indexOf(name + '(', i)) >= 0) {
    const parsed = P.balanced(text, i + name.length);
    const replacement = action(P.splitArgs(parsed.inner));
    text = text.slice(0, i) + replacement + text.slice(parsed.end + 1);
    i += replacement.length;
  }
  return text;
}

function value(expression) {
  let e = expression;
  e = rewrite(e, 'new AspectList', a => a.length ? 'ASPECTS[' + subjects(a[0]).join('|') + ']' : 'EMPTY');
  for (const name of ['aspectsOf', 'AspectHelper.getObjectAspects']) e = rewrite(e, name, a => 'ASPECTS[' + subjects(a[0]).join('|') + ']');
  return e.replace(/\s+/g, '');
}

function collect(source, calls, label) {
  const result = new Map();
  for (const [name, mode] of calls) {
    for (const a of P.calls(body(source, 'registerItemAspects'), name)) {
      const input = a[0].replace(/\s+/g, '');
      if (label === 'original' && (/^newItemStack\((?:ItemsTC\.(?:eldritchEye|runedTablet)|BlocksTC\.eldritch)(?:,.*)?\)$/.test(input)
          || /^newItemStack\(BlocksTC\.pedestalAncient,1,1\)$/.test(input) || /^newItemStack\(BlocksTC\.pedestalEldritch,1,2\)$/.test(input))) {
        excluded.push(a[0]); // не зарегистрировано либо недостижимая metadata, DECISIONS 29/135
        continue;
      }
      try {
        const v = mode + ':' + value(a[1]);
        for (const key of subjects(a[0], a[1])) {
          const list = result.get(key) || [];
          if (list.at(-1) !== v) list.push(v);
          result.set(key, list);
        }
      } catch (e) { manual.push(label + ' ' + a[0] + ': ' + e.message); }
    }
  }
  return result;
}
const original = collect(old, [['ThaumcraftApi.registerObjectTag', 'object'], ['ThaumcraftApi.registerComplexObjectTag', 'complex']], 'original');
const port = collect(current, [['obj', 'object'], ['complex', 'complex']], 'port');
const differences = [];
let compared = 0;
for (const [key, list] of original) {
  if (!port.has(key)) { manual.push('Unmatched original binding: ' + key); continue; }
  compared++;
  if (JSON.stringify(list) !== JSON.stringify(port.get(key))) differences.push(key + '\n  original: ' + JSON.stringify(list) + '\n  port: ' + JSON.stringify(port.get(key)));
}
for (const key of port.keys()) if (!original.has(key)) differences.push('Unmatched port binding: ' + key);

// Числа и условия NBT существ: имена EntityList сохраняются в вызовах, перевод ID выполняет отдельный помощник.
const oldEntities = P.calls(body(old, 'registerEntityAspects'), 'ThaumcraftApi.registerEntityTag');
const newEntities = P.calls(body(current, 'registerEntityAspects'), 'entity');
const entitySignature = a => a.map(s => s.replace(/ThaumcraftApi\.EntityTagsNBT/g, 'EntityTagsNBT').replace(/\s+/g, '')).join(',');
for (let i = 0; i < Math.max(oldEntities.length, newEntities.length); i++) {
  if (!oldEntities[i] || !newEntities[i] || entitySignature(oldEntities[i]) !== entitySignature(newEntities[i])) {
    differences.push('Entity binding ' + i + '\n  original: ' + JSON.stringify(oldEntities[i]) + '\n  port: ' + JSON.stringify(newEntities[i]));
  }
}
fs.mkdirSync(path.join(root, 'build'), { recursive: true });
fs.writeFileSync(path.join(root, 'build/aspect-audit-manual.txt'), manual.join('\n') + '\n');
fs.writeFileSync(path.join(root, 'build/aspect-audit-excluded.txt'), excluded.join('\n') + '\n');
fs.writeFileSync(path.join(root, 'build/aspect-audit-differences.txt'), differences.join('\n') + '\n');
console.log('Aspect bindings compared:', compared, 'original/port:', original.size + '/' + port.size, 'entity declarations:', oldEntities.length,
  'differences:', differences.length, 'manual:', manual.length, 'excluded:', excluded.length);
differences.slice(0, 15).forEach(d => console.error(d));
if (differences.length || manual.length) process.exitCode = 1;
