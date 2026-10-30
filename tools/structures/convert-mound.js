// Конвертация WorldGenMound оригинала (последовательность world.setBlockState) в файл данных кургана.
//   node tools/structures/convert-mound.js
// Выход: src/main/resources/thaumcraft/structures/mound.txt — строки «x y z p» (смещения от угла, индекс палитры
// WorldGenMound.PALETTE в порядке оригинала; ящики/урны, сундук, рассадники ставятся кодом).
'use strict';
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '..', '..');
const full = fs.readFileSync(path.join(root, 'reference/src-mcp/thaumcraft/thaumcraft/common/world/objects/WorldGenMound.java'), 'utf8');
// хвост после «float rr = rand.nextFloat()» (добыча, сундук, рассадники) — в коде
const src = full.slice(0, full.indexOf('float rr = rand.nextFloat()'));
const re = /world\.setBlockState\(new BlockPos\(i \+ (-?\d+), j ([+-]) (\d+), k \+ (\d+)\), Blocks\.([A-Z_]+)\.(?:getDefaultState\(\)|getStateFromMeta\((\d+)\))\);/g;
const PAL = { AIR: 0, DIRT: 1, COBBLESTONE: 2, GRASS: 3, MOSSY_COBBLESTONE: 4, IRON_BARS: 5 };
const out = [];
let m;
while ((m = re.exec(src))) {
  const x = +m[1], y = (m[2] === '-' ? -1 : 1) * +m[3], z = +m[4], b = m[5], meta = m[6] === undefined ? 0 : +m[6];
  let p;
  if (b in PAL) p = PAL[b];
  else if (b === 'LADDER' && meta === 5) p = 6;
  else if (b === 'STONEBRICK' && meta === 3) p = 7;
  else if (b === 'TALLGRASS' && meta === 1) p = 8;
  else if (b === 'STONE_STAIRS') p = 10 + meta;
  else throw new Error('неизвестный блок ' + b + ':' + meta);
  out.push(`${x} ${y} ${z} ${p}`);
}
const dst = path.join(root, 'src/main/resources/thaumcraft/structures/mound.txt');
fs.mkdirSync(path.dirname(dst), { recursive: true });
fs.writeFileSync(dst, out.join('\n') + '\n');
console.log('блоков:', out.length);
