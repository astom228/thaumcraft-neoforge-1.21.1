// Проверка регистраций звуков, ссылок, определений событий и неизменности аудиозаписей оригинала.
// node tools/assets/check-sounds.js
'use strict';
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../..');
const original = path.join(root, 'reference/src/thaumcraft/assets/thaumcraft');
const current = path.join(root, 'src/main/resources/assets/thaumcraft');
const source = JSON.parse(fs.readFileSync(path.join(original, 'sounds.json'), 'utf8'));
const events = JSON.parse(fs.readFileSync(path.join(current, 'sounds.json'), 'utf8'));
const registrations = [...fs.readFileSync(path.join(root, 'src/main/java/thaumcraft/common/lib/SoundsTC.java'), 'utf8')
  .matchAll(/= register\("([^"]+)"\)/g)].map(m => m[1]);
const errors = [];
let count = 0;
function walk(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]);
}
for (const file of walk(path.join(original, 'sounds')).filter(f => f.endsWith('.ogg'))) {
  const relative = path.relative(original, file), destination = path.join(current, relative);
  if (!fs.existsSync(destination) || !fs.readFileSync(file).equals(fs.readFileSync(destination))) errors.push('Changed/missing audio: ' + relative);
  count++;
}
for (const name of registrations) if (!events[name]) errors.push('Missing sound event: ' + name);
for (const [name, event] of Object.entries(events)) {
  if (!registrations.includes(name)) errors.push('Unregistered sound event: ' + name);
  for (const entry of event.sounds) {
    const sound = typeof entry === 'string' ? entry : entry.name;
    const [namespace, file] = sound.includes(':') ? sound.split(':') : ['minecraft', sound];
    if (namespace === 'thaumcraft' && !fs.existsSync(path.join(current, 'sounds', file + '.ogg'))) errors.push('Missing audio reference: ' + sound);
  }
}
for (const [name, event] of Object.entries(source)) {
  // Единственное исправленное имя оригинала, DEVIATIONS 7; само определение и аудиофайл сохранены.
  const renamed = name === 'runicshieldecharge' ? 'runicshieldcharge' : name;
  if (JSON.stringify(event) !== JSON.stringify(events[renamed])) errors.push('Changed sound definition: ' + name);
}
// Проверяем покрытие обращений к событиям, а не равенство числа вызовов:
// при переносе обработчики объединены/перенесены, SoundType задаются в свойствах блоков.
function usedEvents(directory) {
  return new Set(walk(directory).filter(f => f.endsWith('.java')).flatMap(file =>
    [...fs.readFileSync(file, 'utf8').matchAll(/SoundsTC\.([a-z][A-Za-z0-9_]*)\b/g)]
      .map(m => m[1]).filter(name => registrations.includes(name))));
}
const sourceJava = path.join(root, 'reference/src-mcp/thaumcraft/thaumcraft');
const usedOriginal = usedEvents(sourceJava);
const usedCurrent = usedEvents(path.join(root, 'src/main/java/thaumcraft'));
for (const name of usedOriginal) {
  if (usedCurrent.has(name)) continue;
  // В BETA17 crystal есть только в невызванном playSocketSound GuiFocalManipulator.
  const gui = fs.readFileSync(path.join(sourceJava, 'client/gui/GuiFocalManipulator.java'), 'utf8');
  const socketReferences = walk(sourceJava).filter(f => f.endsWith('.java')).reduce((n, file) =>
    n + [...fs.readFileSync(file, 'utf8').matchAll(/\bplaySocketSound\s*\(/g)].length, 0);
  if (name === 'crystal' && socketReferences === 1 && /SoundsTC\.crystal\b/.test(gui)) continue;
  errors.push('Original sound event has no port usage: ' + name);
}
errors.forEach(e => console.error(e));
console.log('Sounds:', registrations.length, 'events,', count, 'original audio files; errors:', errors.length);
console.log('Java event coverage:', usedOriginal.size, 'original,', usedCurrent.size, 'port (crystal: unused original helper).');
if (errors.length) process.exitCode = 1;
