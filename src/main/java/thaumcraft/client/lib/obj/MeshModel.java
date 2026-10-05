package thaumcraft.client.lib.obj;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Quaterniond;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;

/**
 * OBJ-сетка Thaumcraft (порт MeshModel): позиции, текстурные координаты, части; поворот (как codechicken Rotation + смещение)
 * и запекание в BakedQuad одной текстурой.
 */
public class MeshModel {
    public List<Vector3f> positions;
    public List<Vector3f> normals;
    public List<Vector2f> texCoords;
    public List<MeshPart> parts = new ArrayList<>();

    @Override
    public MeshModel clone() {
        MeshModel mm = new MeshModel();
        mm.parts = new ArrayList<>(this.parts);
        if (this.positions != null) {
            mm.positions = new ArrayList<>();
            for (Vector3f v : this.positions) {
                mm.positions.add(new Vector3f(v));
            }
        }
        if (this.normals != null) {
            mm.normals = new ArrayList<>();
            for (Vector3f v : this.normals) {
                mm.normals.add(new Vector3f(v));
            }
        }
        if (this.texCoords != null) {
            mm.texCoords = new ArrayList<>();
            for (Vector2f v : this.texCoords) {
                mm.texCoords.add(new Vector2f(v));
            }
        }
        return mm;
    }

    /** Поворот на угол (радианы) вокруг оси, затем смещение (codechicken Rotation.apply + Vector3.add). */
    public void rotate(double angle, Vector3d axis, Vector3d offset) {
        Quaterniond q = new Quaterniond().fromAxisAngleRad(axis.x, axis.y, axis.z, angle);
        List<Vector3f> p = new ArrayList<>();
        for (Vector3f v : this.positions) {
            Vector3d vec = new Vector3d(v.x, v.y, v.z);
            q.transform(vec);
            vec.add(offset);
            p.add(new Vector3f((float) vec.x, (float) vec.y, (float) vec.z));
        }
        this.positions = p;
    }

    public void addPosition(float x, float y, float z) {
        if (this.positions == null) {
            this.positions = new ArrayList<>();
        }
        this.positions.add(new Vector3f(x, y, z));
    }

    public void addNormal(float x, float y, float z) {
        if (this.normals == null) {
            this.normals = new ArrayList<>();
        }
        this.normals.add(new Vector3f(x, y, z));
    }

    public void addTexCoords(float x, float y) {
        if (this.texCoords == null) {
            this.texCoords = new ArrayList<>();
        }
        this.texCoords.add(new Vector2f(x, y));
    }

    public void addPart(MeshPart part) {
        this.parts.add(part);
    }

    /** bakeModel(sprite) оригинала: все части одной текстурой, без затенения (applyDiffuseLighting = false). */
    public List<BakedQuad> bakeModel(TextureAtlasSprite sprite, int packedLight) {
        List<BakedQuad> bakeList = new ArrayList<>();
        for (MeshPart part : this.parts) {
            for (int i = 0; i < part.indices.size(); i += 4) {
                bakeList.add(this.bakeQuad(part, i, sprite, packedLight));
            }
        }
        return bakeList;
    }

    private BakedQuad bakeQuad(MeshPart part, int startIndex, TextureAtlasSprite sprite, int packedLight) {
        Vector3f[] pos = new Vector3f[4];
        Vector2f[] uv = new Vector2f[4];
        for (int i = 0; i < 4; i++) {
            int[] indices = part.indices.get(startIndex + i);
            int p = 0;
            pos[i] = this.positions != null ? this.positions.get(indices[p++]) : new Vector3f();
            if (this.normals != null) {
                p++;
            }
            uv[i] = this.texCoords != null ? this.texCoords.get(indices[p]) : new Vector2f();
        }
        Vector3f normal = new Vector3f(pos[2]).sub(pos[1]).cross(new Vector3f(pos[0]).sub(pos[1]));
        if (normal.lengthSquared() > 0) {
            normal.normalize();
        }
        QuadBakingVertexConsumer builder = new QuadBakingVertexConsumer();
        builder.setSprite(sprite);
        builder.setTintIndex(part.name != null && part.name.contains("focus") ? 1 : part.tintIndex);
        builder.setDirection(Direction.getNearest(normal.x, normal.y, normal.z));
        builder.setShade(false);
        builder.setHasAmbientOcclusion(true);
        for (int i = 0; i < 4; i++) {
            builder.addVertex(pos[i].x, pos[i].y, pos[i].z)
                    .setColor(-1)
                    .setUv(sprite.getU(uv[i].x), sprite.getV(uv[i].y))
                    .setLight(0)
                    .setNormal(normal.x, normal.y, normal.z);
        }
        BakedQuad quad = builder.bakeQuad();
        if (packedLight != 0) {
            // Минимальное освещение квада (в 1.12 — getPackedLightmapCoords блока); рендер берёт максимум с освещением мира
            int[] data = quad.getVertices().clone();
            for (int i = 0; i < 4; i++) {
                data[i * IQuadTransformer.STRIDE + IQuadTransformer.UV2] = packedLight;
            }
            quad = new BakedQuad(data, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
        }
        return quad;
    }
}
