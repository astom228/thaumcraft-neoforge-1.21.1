// Конвертация моделей брони 1.12 (ModelBiped + ModelRenderer, client/renderers/models/gear) в модели 1.21
// (HumanoidModel + LayerDefinition).
//   node tools/models/convert-armor-models.js
// Из конструктора берутся: new ModelRenderer(this, u, v), mirror, addBox, setRotationPoint, setRotation, cubeList.clear(),
// addChild (в т.ч. в ветках if (f < 1.0F) — вариант «поножи», иначе — «нагрудник»). Циклы по массивам частей разворачиваются.
// Анимация и скрытие частей (render оригинала) пишутся вручную в классах предметов/клиента.
'use strict';
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..', '..');
const SRC = path.join(root, 'reference/src-mcp/thaumcraft/thaumcraft/client/renderers/models/gear');
const OUT = path.join(root, 'src/main/java/thaumcraft/client/renderers/models/gear');
const MODELS = ['ModelRobe', 'ModelFortressArmor', 'ModelKnightArmor', 'ModelLeaderArmor'];

const BIPED = {
  bipedHead: 'head', bipedHeadwear: 'hat', bipedBody: 'body', bipedRightArm: 'right_arm', bipedLeftArm: 'left_arm',
  bipedRightLeg: 'right_leg', bipedLeftLeg: 'left_leg',
};
// ModelBiped(f, 0, w, h): опорные точки и кубы по умолчанию
const BIPED_DEF = {
  head: { pivot: [0, 0, 0], tex: [0, 0], box: [-4, -8, -4, 8, 8, 8], grow: 0 },
  hat: { pivot: [0, 0, 0], tex: [32, 0], box: [-4, -8, -4, 8, 8, 8], grow: 0.5 },
  body: { pivot: [0, 0, 0], tex: [16, 16], box: [-4, 0, -2, 8, 12, 4], grow: 0 },
  right_arm: { pivot: [-5, 2, 0], tex: [40, 16], box: [-3, -2, -2, 4, 12, 4], grow: 0 },
  left_arm: { pivot: [5, 2, 0], tex: [40, 16], box: [-1, -2, -2, 4, 12, 4], grow: 0, mirror: true },
  right_leg: { pivot: [-1.9, 12, 0], tex: [0, 16], box: [-2, 0, -2, 4, 12, 4], grow: 0 },
  left_leg: { pivot: [1.9, 12, 0], tex: [0, 16], box: [-2, 0, -2, 4, 12, 4], grow: 0, mirror: true },
};

const num = (s) => {
  s = s.trim().replace(/\(float\)/g, '').replace(/Math\.PI/g, String(Math.PI)).replace(/F$/i, '').replace(/(\d)F/gi, '$1');
  // только числовые выражения (после развёртки циклов)
  if (!/^[-+*/().\d\s]+$/.test(s)) throw new Error('не число: ' + s);
  return Function('return (' + s + ')')();
};
const fmt = (n) => {
  const v = Math.round(n * 1e7) / 1e7;
  return (Number.isInteger(v) ? v.toFixed(1) : String(v)) + 'F';
};

function constructorBody(src, cls) {
  const start = src.indexOf('public ' + cls + '(float f)');
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

// развернуть for (int a = 0; a < N; a++) { ... [a] ... }
function unrollLoops(body) {
  const re = /for \(int (\w+) = 0; \1 < (\d+); \1\+\+\) \{([^{}]*)\}/g;
  return body.replace(re, (m, v, n, inner) => {
    let out = '';
    for (let k = 0; k < +n; k++) {
      out += inner.replace(new RegExp('\\[' + v + '\\]', 'g'), String(k)).replace(new RegExp('\\b' + v + '\\b', 'g'), String(k));
    }
    return out;
  });
}

function parse(cls) {
  const src = fs.readFileSync(path.join(SRC, cls + '.java'), 'utf8');
  let body = unrollLoops(constructorBody(src, cls));
  // имена массивов Mask[0] -> Mask0
  body = body.replace(/this\.(\w+)\[(\d+)\]/g, 'this.$1$2');
  const parts = {};
  const order = [];
  const cleared = new Set();
  const attach = []; // { parent, child, variant: null|'legs'|'chest' }
  let texW = 64, texH = 32;
  let variant = null;
  let depth = 0;
  const unparsed = [];
  for (let raw of body.split('\n')) {
    const line = raw.trim();
    if (!line) continue;
    let m;
    if ((m = line.match(/^if \(f < 1\.0F\) \{$/))) { variant = 'legs'; depth++; continue; }
    if ((m = line.match(/^if \(!\(f < 1\.0F\)\) \{$/))) { variant = 'chest'; depth++; continue; }
    if (line === '} else {') { variant = 'chest'; continue; }
    if (line === '}') { variant = null; depth--; continue; }
    if ((m = line.match(/^super\(f, 0, (\d+), (\d+)\);$/))) { texW = +m[1]; texH = +m[2]; continue; }
    if ((m = line.match(/^this\.textureWidth = (\d+);$/))) { texW = +m[1]; continue; }
    if ((m = line.match(/^this\.textureHeight = (\d+);$/))) { texH = +m[1]; continue; }
    if ((m = line.match(/^this\.(\w+) = new ModelRenderer\[\d+\];$/))) continue;
    if ((m = line.match(/^this\.(\w+) = new ModelRenderer\(this, (.+?), (.+?)\);$/))) {
      parts[m[1]] = { name: m[1], u: num(m[2]), v: num(m[3]), mirror: false, boxes: [], rp: [0, 0, 0], rot: [0, 0, 0] };
      order.push(m[1]);
      continue;
    }
    if ((m = line.match(/^this\.(\w+)\.mirror = (true|false);$/))) { parts[m[1]].mirror = m[2] === 'true'; continue; }
    if ((m = line.match(/^this\.(\w+)\.addBox\((.+)\);$/))) {
      const a = m[2].split(',').map(num);
      parts[m[1]].boxes.push({ b: a.slice(0, 6), grow: a[6] || 0, mirror: parts[m[1]].mirror });
      continue;
    }
    if ((m = line.match(/^this\.(\w+)\.setRotationPoint\((.+)\);$/))) {
      if (BIPED[m[1]]) { unparsed.push(line); continue; }
      parts[m[1]].rp = m[2].split(',').map(num);
      continue;
    }
    if ((m = line.match(/^this\.setRotation\(this\.(\w+), (.+)\);$/))) { parts[m[1]].rot = m[2].split(',').map(num); continue; }
    if ((m = line.match(/^this\.(\w+)\.setTextureSize\(/))) continue;
    if ((m = line.match(/^this\.(\w+)\.cubeList\.clear\(\);$/))) { cleared.add(BIPED[m[1]]); continue; }
    if ((m = line.match(/^this\.(\w+)\.addChild\(this\.(\w+)\);$/))) {
      attach.push({ parent: BIPED[m[1]] || m[1], child: m[2], variant });
      continue;
    }
    unparsed.push(line);
  }
  if (unparsed.length) throw new Error(cls + ': неразобранные строки:\n' + unparsed.join('\n'));
  return { cls, parts, order, cleared, attach, texW, texH };
}

function generate(m) {
  const vanilla = Object.values(BIPED);
  const childrenOf = (parent, legs) => m.attach.filter((a) => a.parent === parent
    && (a.variant === null || a.variant === (legs ? 'legs' : 'chest')));
  // путь к части от корня (для полей модели)
  const pathOf = {};
  const walk = (parent, prefix, legs) => {
    for (const a of childrenOf(parent, legs)) {
      pathOf[a.child] = pathOf[a.child] || prefix.concat([a.child]);
      walk(a.child, prefix.concat([a.child]), legs);
    }
  };
  for (const v of vanilla) {
    walk(v, [v], true);
    walk(v, [v], false);
  }
  const fields = m.order.filter((n) => pathOf[n]);
  let s = '';
  s += 'package thaumcraft.client.renderers.models.gear;\n\n';
  s += 'import net.minecraft.client.model.HumanoidModel;\n';
  s += 'import net.minecraft.client.model.geom.ModelPart;\n';
  s += 'import net.minecraft.client.model.geom.PartPose;\n';
  s += 'import net.minecraft.client.model.geom.builders.CubeDeformation;\n';
  s += 'import net.minecraft.client.model.geom.builders.CubeListBuilder;\n';
  s += 'import net.minecraft.client.model.geom.builders.LayerDefinition;\n';
  s += 'import net.minecraft.client.model.geom.builders.MeshDefinition;\n';
  s += 'import net.minecraft.client.model.geom.builders.PartDefinition;\n';
  s += 'import net.minecraft.world.entity.LivingEntity;\n\n';
  s += '/**\n * Порт ' + m.cls + ' (сгенерировано tools/models/convert-armor-models.js — не править вручную). Части — как в оригинале;\n';
  s += ' * legs = true — вариант для поножей (ModelBiped(0.5F)), иначе — для шлема, нагрудника и ботинок (1.0F).\n';
  s += ' * Части, которых нет в варианте, — null. Анимацию и скрытие частей задаёт код предмета (render оригинала).\n */\n';
  s += 'public class ' + m.cls + ' extends HumanoidModel<LivingEntity> {\n';
  for (const f of fields) s += '    public final ModelPart ' + f + ';\n';
  s += '\n    public ' + m.cls + '(ModelPart root) {\n        super(root);\n';
  for (const f of fields) {
    const p = pathOf[f];
    s += '        this.' + f + ' = find(root, ' + p.map((x) => '"' + x + '"').join(', ') + ');\n';
  }
  s += '    }\n\n';
  s += '    private static ModelPart find(ModelPart root, String... path) {\n        ModelPart p = root;\n        for (String n : path) {\n';
  s += '            if (!p.hasChild(n)) {\n                return null;\n            }\n            p = p.getChild(n);\n        }\n        return p;\n    }\n\n';
  s += '    public static LayerDefinition createLayer(boolean legs) {\n';
  s += '        float f = legs ? 0.5F : 1.0F;\n';
  s += '        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n';
  for (const v of vanilla) {
    const d = BIPED_DEF[v];
    let cubes = 'CubeListBuilder.create()';
    if (!m.cleared.has(v)) {
      cubes += '.texOffs(' + d.tex[0] + ', ' + d.tex[1] + ')' + (d.mirror ? '.mirror()' : '') + '.addBox(' + d.box.map(fmt).join(', ')
        + ', new CubeDeformation(f' + (d.grow ? ' + ' + fmt(d.grow) : '') + '))';
    }
    s += '        PartDefinition ' + v + ' = root.addOrReplaceChild("' + v + '", ' + cubes + ', PartPose.offset(' + d.pivot.map(fmt).join(', ') + '));\n';
  }
  const emit = (parent, legs, indent) => {
    let out = '';
    for (const a of childrenOf(parent, legs)) {
      const p = m.parts[a.child];
      let cubes = 'CubeListBuilder.create().texOffs(' + p.u + ', ' + p.v + ')';
      for (const b of p.boxes) {
        cubes += (b.mirror ? '.mirror()' : '.mirror(false)') + '.addBox(' + b.b.map(fmt).join(', ')
          + (b.grow ? ', new CubeDeformation(' + fmt(b.grow) + ')' : '') + ')';
      }
      const pose = 'PartPose.offsetAndRotation(' + p.rp.map(fmt).join(', ') + ', ' + p.rot.map(fmt).join(', ') + ')';
      const sub = childrenOf(a.child, legs);
      if (sub.length) {
        out += indent + '{\n' + indent + '    PartDefinition ' + a.child + ' = ' + parent + '.addOrReplaceChild("' + a.child + '", ' + cubes + ', ' + pose + ');\n';
        out += emit(a.child, legs, indent + '    ');
        out += indent + '}\n';
      } else {
        out += indent + parent + '.addOrReplaceChild("' + a.child + '", ' + cubes + ', ' + pose + ');\n';
      }
    }
    return out;
  };
  let common = '', legsOnly = '', chestOnly = '';
  for (const v of vanilla) {
    const a = emit(v, true, '            ');
    const b = emit(v, false, '            ');
    common += a === b ? a.replace(/^ {4}/gm, '') : '';
    if (a !== b) {
      legsOnly += a;
      chestOnly += b;
    }
  }
  s += common;
  if (legsOnly || chestOnly) {
    s += '        if (legs) {\n' + legsOnly + '        } else {\n' + chestOnly + '        }\n';
  }
  s += '        return LayerDefinition.create(mesh, ' + m.texW + ', ' + m.texH + ');\n    }\n}\n';
  return s;
}

fs.mkdirSync(OUT, { recursive: true });
for (const cls of MODELS) {
  const m = parse(cls);
  fs.writeFileSync(path.join(OUT, cls + '.java'), generate(m));
  console.log(cls + ': частей ' + m.order.length + ', текстура ' + m.texW + 'x' + m.texH + ', без ванильных кубов: ' + [...m.cleared].join(','));
}
