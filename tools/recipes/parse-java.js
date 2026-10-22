// Разбор вызовов рецептов из ConfigRecipes.java оригинала (MCP-имена): аргументы — как дерево выражений.
const fs = require('fs');
const path = require('path');

const SRC = path.join(__dirname, '../../reference/src-mcp/thaumcraft/thaumcraft/common/config/ConfigRecipes.java');

// Тело метода по имени
function methodBody(src, name) {
  const i = src.indexOf(`public static void ${name}(`);
  if (i < 0) throw new Error('нет метода ' + name);
  let j = src.indexOf('{', i), depth = 0;
  for (let k = j; k < src.length; k++) {
    if (src[k] === '{') depth++;
    else if (src[k] === '}') { depth--; if (depth === 0) return src.slice(j + 1, k); }
  }
  throw new Error('конец метода не найден');
}

// Разделить список аргументов верхнего уровня (учёт скобок, строк и символов)
function splitArgs(s) {
  const out = []; let depth = 0, cur = '', q = null;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (q) { cur += c; if (c === '\\') { cur += s[++i]; continue; } if (c === q) q = null; continue; }
    if (c === '"' || c === "'") { q = c; cur += c; continue; }
    if (c === '(' || c === '{' || c === '[') depth++;
    if (c === ')' || c === '}' || c === ']') depth--;
    if (c === ',' && depth === 0) { out.push(cur.trim()); cur = ''; continue; }
    cur += c;
  }
  if (cur.trim()) out.push(cur.trim());
  return out;
}

// Содержимое скобок после позиции открывающей скобки
function balanced(s, open) {
  let depth = 0, q = null;
  for (let i = open; i < s.length; i++) {
    const c = s[i];
    if (q) { if (c === '\\') { i++; continue; } if (c === q) q = null; continue; }
    if (c === '"' || c === "'") { q = c; continue; }
    if (c === '(') depth++;
    else if (c === ')') { depth--; if (depth === 0) return { inner: s.slice(open + 1, i), end: i }; }
  }
  throw new Error('несбалансированные скобки');
}

// Все вызовы call(...) в тексте
function calls(text, call) {
  const res = [];
  let i = 0;
  while ((i = text.indexOf(call + '(', i)) >= 0) {
    const b = balanced(text, i + call.length);
    res.push(splitArgs(b.inner));
    i = b.end;
  }
  return res;
}

module.exports = { SRC, methodBody, splitArgs, balanced, calls, read: () => fs.readFileSync(SRC, 'utf8') };
