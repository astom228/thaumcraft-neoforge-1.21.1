// Конвертация геометрии моделей существ 1.12 (ModelBase + ModelRenderer, client/renderers/models/entity) в LayerDefinition 1.21.
//   node tools/models/convert-entity-models.js
// Из конструктора берутся: new ModelRenderer(this, u, v), mirror, addBox, setRotationPoint, setRotation / rotateAngleX|Y|Z = …,
// setTextureOffset, addChild, textureWidth/Height. Получается класс <Model>Layer с методом create() — каждая часть с именем
// поля оригинала (дети addChild — вложенные). Анимация и порядок рисования (render оригинала) пишутся вручную в классе модели.
'use strict';
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..', '..');
const SRC = path.join(root, 'reference/src-mcp/thaumcraft/thaumcraft/client/renderers/models/entity');
const OUT = path.join(root, 'src/main/java/thaumcraft/client/renderers/models/entity');
const MODELS = process.argv.slice(2).length ? process.argv.slice(2) : ['ModelEldritchCrab', 'ModelEldritchGuardian', 'ModelEldritchGolem', 'ModelPech'];

const num = (s) => {
  s = s.trim().replace(/\(float\)/g, '').replace(/Math\.PI/g, String(Math.PI)).replace(/(\d)F\b/gi, '$1').replace(/(\d)\.F/gi, '$1.0');
  if (!/^[-+*/().\d\seE]+$/.test(s)) throw new Error('не число: ' + s);
  return Function('return (' + s + ')')();
};
const fmt = (n) => {
  const v = Math.round(n * 1e7) / 1e7;
  return (Number.isInteger(v) ? v.toFixed(1) : String(v)) + 'F';
};

function constructorBody(src, cls) {
  const start = src.indexOf('public ' + cls + '()');
  if (start < 0) throw new Error('нет конструктора ' + cls);
  let i = src.indexOf('{', start) + 1;
  let depth = 1;
  const from = i;
  for (; depth > 0; i++) {
    if (src[i] === '{') depth++;
    else if (src[i] === '}') depth--;
  }
  return src.slice(from, i - 1);
}

function parse(cls) {
  const src = fs.readFileSync(path.join(SRC, cls + '.java'), 'utf8');
  const body = constructorBody(src, cls);
  const parts = {};
  const order = [];
  const attach = [];
  let texW = 64, texH = 32;
  const unparsed = [];
  for (const raw of body.split('\n')) {
    const line = raw.trim();
    if (!line) continue;
    let m;
    if ((m = line.match(/^this\.textureWidth = (\d+);$/))) { texW = +m[1]; continue; }
    if ((m = line.match(/^this\.textureHeight = (\d+);$/))) { texH = +m[1]; continue; }
    if ((m = line.match(/^this\.(\w+) = new ModelRenderer\(this, (.+?), (.+?)\);$/))) {
      parts[m[1]] = { name: m[1], u: num(m[2]), v: num(m[3]), mirror: false, boxes: [], rp: [0, 0, 0], rot: [0, 0, 0] };
      if (!order.includes(m[1])) order.push(m[1]);
      continue;
    }
    if ((m = line.match(/^this\.(\w+) = new ModelRenderer\(this\);$/))) {
      parts[m[1]] = { name: m[1], u: 0, v: 0, mirror: false, boxes: [], rp: [0, 0, 0], rot: [0, 0, 0] };
      if (!order.includes(m[1])) order.push(m[1]);
      continue;
    }
    if ((m = line.match(/^this\.(\w+)\.setTextureOffset\((.+?), (.+?)\);$/))) { parts[m[1]].u = num(m[2]); parts[m[1]].v = num(m[3]); continue; }
    if ((m = line.match(/^this\.(\w+)\.mirror = (true|false);$/))) { parts[m[1]].mirror = m[2] === 'true'; continue; }
    if ((m = line.match(/^this\.(\w+)\.addBox\((.+)\);$/))) {
      const a = m[2].split(',').map(num);
      parts[m[1]].boxes.push({ b: a.slice(0, 6), grow: a[6] || 0, mirror: parts[m[1]].mirror, u: parts[m[1]].u, v: parts[m[1]].v });
      continue;
    }
    if ((m = line.match(/^this\.(\w+)\.setRotationPoint\((.+)\);$/))) { parts[m[1]].rp = m[2].split(',').map(num); continue; }
    if ((m = line.match(/^this\.(\w+)\.rotationPoint([XYZ]) = (.+);$/))) { parts[m[1]].rp['XYZ'.indexOf(m[2])] = num(m[3]); continue; }
    if ((m = line.match(/^this\.setRotation\(this\.(\w+), (.+)\);$/))) { parts[m[1]].rot = m[2].split(',').map(num); continue; }
    if ((m = line.match(/^this\.(\w+)\.rotateAngle([XYZ]) = (.+);$/))) { parts[m[1]].rot['XYZ'.indexOf(m[2])] = num(m[3]); continue; }
    if ((m = line.match(/^this\.(\w+)\.setTextureSize\(/))) continue;
    if ((m = line.match(/^this\.(\w+)\.addChild\(this\.(\w+)\);$/))) { attach.push({ parent: m[1], child: m[2] }); continue; }
    if ((m = line.match(/^super\(\);$/))) continue;
    unparsed.push(line);
  }
  if (unparsed.length) throw new Error(cls + ': неразобранные строки:\n' + unparsed.join('\n'));
  return { cls, parts, order, attach, texW, texH };
}

function generate(m) {
  const childOf = new Set(m.attach.map((a) => a.child));
  const children = (p) => m.attach.filter((a) => a.parent === p).map((a) => a.child);
  const cubes = (p) => {
    let c = 'CubeListBuilder.create()';
    for (const b of p.boxes) {
      c += '.texOffs(' + b.u + ', ' + b.v + ')' + (b.mirror ? '.mirror()' : '.mirror(false)') + '.addBox(' + b.b.map(fmt).join(', ')
        + (b.grow ? ', new CubeDeformation(' + fmt(b.grow) + ')' : '') + ')';
    }
    return c;
  };
  const emit = (name, parentVar, indent) => {
    const p = m.parts[name];
    const pose = 'PartPose.offsetAndRotation(' + p.rp.map(fmt).join(', ') + ', ' + p.rot.map(fmt).join(', ') + ')';
    const kids = children(name);
    if (!kids.length) return indent + parentVar + '.addOrReplaceChild("' + name + '", ' + cubes(p) + ', ' + pose + ');\n';
    let out = indent + '{\n' + indent + '    PartDefinition ' + name + ' = ' + parentVar + '.addOrReplaceChild("' + name + '", ' + cubes(p) + ', ' + pose + ');\n';
    for (const k of kids) out += emit(k, name, indent + '    ');
    return out + indent + '}\n';
  };
  let s = 'package thaumcraft.client.renderers.models.entity;\n\n';
  s += 'import net.minecraft.client.model.geom.PartPose;\n';
  s += 'import net.minecraft.client.model.geom.builders.CubeDeformation;\n';
  s += 'import net.minecraft.client.model.geom.builders.CubeListBuilder;\n';
  s += 'import net.minecraft.client.model.geom.builders.LayerDefinition;\n';
  s += 'import net.minecraft.client.model.geom.builders.MeshDefinition;\n';
  s += 'import net.minecraft.client.model.geom.builders.PartDefinition;\n\n';
  s += '/** Геометрия ' + m.cls + ' (сгенерировано tools/models/convert-entity-models.js — не править вручную). Имена частей — поля оригинала. */\n';
  s += '@SuppressWarnings("unused")\n';
  s += 'public final class ' + m.cls + 'Layer {\n';
  s += '    private ' + m.cls + 'Layer() {\n    }\n\n';
  s += '    public static LayerDefinition create() {\n';
  s += '        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n';
  for (const n of m.order) if (!childOf.has(n)) s += emit(n, 'root', '        ');
  s += '        return LayerDefinition.create(mesh, ' + m.texW + ', ' + m.texH + ');\n    }\n}\n';
  return s;
}

for (const cls of MODELS) {
  const m = parse(cls);
  fs.writeFileSync(path.join(OUT, cls + 'Layer.java'), generate(m));
  console.log(cls + ': частей ' + m.order.length + ', текстура ' + m.texW + 'x' + m.texH);
}
