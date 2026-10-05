package thaumcraft.client.renderers.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import thaumcraft.client.lib.obj.MeshModel;
import thaumcraft.common.blocks.world.ore.BlockCrystal;

/**
 * Модель вис-кристалла (порт CrystalModel): к каждой твёрдой каменной грани-соседу — (рост + 1) случайных осколков из
 * models/obj/crystal.obj, повёрнутых к этой грани. Если соседи со всех шести сторон — ничего не рисуется (как в оригинале).
 * Минимальное освещение — блочный свет 180 (getPackedLightmapCoords оригинала).
 */
public class CrystalModel implements BakedModel {
    private static final int PACKED_LIGHT = 180;
    private final MeshModel sourceMesh;
    private final TextureAtlasSprite tex;

    public CrystalModel(MeshModel sourceMesh, TextureAtlasSprite tex) {
        this.sourceMesh = sourceMesh;
        this.tex = tex;
        for (var mp : this.sourceMesh.parts) {
            mp.tintIndex = 0;
        }
    }

    private MeshModel pick(int m, long seed) {
        List<Integer> c = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7);
        Collections.shuffle(c, new Random(seed));
        MeshModel mm = this.sourceMesh.clone();
        mm.parts.clear();
        for (int a = 0; a < m; a++) {
            mm.parts.add(this.sourceMesh.parts.get(c.get(a)));
        }
        return mm;
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        if (side != null || state == null || !(state.getBlock() instanceof BlockCrystal)) {
            return List.of();
        }
        boolean up = state.getValue(BlockCrystal.UP);
        boolean down = state.getValue(BlockCrystal.DOWN);
        boolean east = state.getValue(BlockCrystal.EAST);
        boolean west = state.getValue(BlockCrystal.WEST);
        boolean north = state.getValue(BlockCrystal.NORTH);
        boolean south = state.getValue(BlockCrystal.SOUTH);
        List<BakedQuad> ret = new ArrayList<>();
        if (up && down && east && west && north && south) {
            return ret;
        }
        int m = state.getValue(BlockCrystal.SIZE) + 1;
        long rand = random.nextLong();
        if (up) {
            MeshModel mod = pick(m, rand);
            mod.rotate(Math.toRadians(180.0), new Vector3d(1, 0, 0), new Vector3d(0, 1, 1));
            ret.addAll(mod.bakeModel(this.tex, PACKED_LIGHT));
        }
        if (down) {
            ret.addAll(pick(m, rand + 5L).bakeModel(this.tex, PACKED_LIGHT));
        }
        if (east) {
            MeshModel mod = pick(m, rand + 10L);
            mod.rotate(Math.toRadians(90.0), new Vector3d(1, 0, 0), new Vector3d(0, 0, 0));
            mod.rotate(Math.toRadians(270.0), new Vector3d(0, 1, 0), new Vector3d(1, 1, 0));
            ret.addAll(mod.bakeModel(this.tex, PACKED_LIGHT));
        }
        if (west) {
            MeshModel mod = pick(m, rand + 15L);
            mod.rotate(Math.toRadians(90.0), new Vector3d(1, 0, 0), new Vector3d(0, 0, 0));
            mod.rotate(Math.toRadians(90.0), new Vector3d(0, 1, 0), new Vector3d(0, 1, 1));
            ret.addAll(mod.bakeModel(this.tex, PACKED_LIGHT));
        }
        if (north) {
            MeshModel mod = pick(m, rand + 20L);
            mod.rotate(Math.toRadians(90.0), new Vector3d(1, 0, 0), new Vector3d(0, 1, 0));
            ret.addAll(mod.bakeModel(this.tex, PACKED_LIGHT));
        }
        if (south) {
            MeshModel mod = pick(m, rand + 25L);
            mod.rotate(Math.toRadians(90.0), new Vector3d(1, 0, 0), new Vector3d(0, 0, 0));
            mod.rotate(Math.toRadians(180.0), new Vector3d(0, 1, 0), new Vector3d(1, 1, 1));
            ret.addAll(mod.bakeModel(this.tex, PACKED_LIGHT));
        }
        return ret;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.tex;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }
}
