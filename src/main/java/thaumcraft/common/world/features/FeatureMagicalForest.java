package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.world.objects.TreeWriter;
import thaumcraft.common.world.objects.WorldGenBigMagicTree;
import thaumcraft.common.world.objects.WorldGenGreatwoodTrees;
import thaumcraft.common.world.objects.WorldGenSilverwoodTrees;

/**
 * Украшение Магического леса (BiomeGenMagicalForest.decorate и getRandomTreeFeature). Фаза «до» (pre): одна живая трава,
 * 0–2 валуна из замшелого булыжника, огромные грибы (1/40 на клетку 4×4), деревья (2 на чанк, +1 с шансом 10%:
 * серебродрево 1/18, великое 1/12, иначе большое магическое). Фаза «после» (post): до 8 вишгрибов у брёвен.
 * Цветы, трава, кувшинки и грибы — ванильными фичами в JSON биома. Позиции — внутри своего чанка (в 1.12 со сдвигом +8).
 */
public class FeatureMagicalForest extends Feature<NoneFeatureConfiguration> {
    private final boolean post;

    public FeatureMagicalForest(Codec<NoneFeatureConfiguration> codec, boolean post) {
        super(codec);
        this.post = post;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        RandomSource random = ctx.random();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;
        ChunkPos chunk = new ChunkPos(cx >> 4, cz >> 4);
        if (this.post) {
            this.vishrooms(world, random, cx, cz);
            return true;
        }
        for (int a = 0; a < 3; a++) {
            int x = cx + 4 + random.nextInt(8);
            int z = cz + 4 + random.nextInt(8);
            BlockPos pp = new BlockPos(x, world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
            while (pp.getY() > 30 && !world.getBlockState(pp).is(Blocks.GRASS_BLOCK)) {
                pp = pp.below();
            }
            if (world.getBlockState(pp).is(Blocks.GRASS_BLOCK)) {
                world.setBlock(pp, BlocksTC.grassAmbient.get().defaultBlockState(), 2);
                break;
            }
        }
        int k = random.nextInt(3);
        for (int l = 0; l < k; l++) {
            int x = cx + random.nextInt(16);
            int z = cz + random.nextInt(16);
            BlockPos p2 = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            Feature.FOREST_ROCK.place(new BlockStateConfiguration(Blocks.MOSSY_COBBLESTONE.defaultBlockState()), world, ctx.chunkGenerator(), random, p2);
        }
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (random.nextInt(40) == 0) {
                    int x = cx + i * 4 + 1 + random.nextInt(3);
                    int z = cz + j * 4 + 1 + random.nextInt(3);
                    BlockPos p2 = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
                    ResourceKey<ConfiguredFeature<?, ?>> key = random.nextBoolean() ? TreeFeatures.HUGE_BROWN_MUSHROOM : TreeFeatures.HUGE_RED_MUSHROOM;
                    world.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getHolder(key)
                            .ifPresent(h -> h.value().place(world, ctx.chunkGenerator(), random, p2));
                }
            }
        }
        int trees = 2 + (random.nextFloat() < 0.1F ? 1 : 0);
        for (int t = 0; t < trees; t++) {
            int x = cx + random.nextInt(16);
            int z = cz + random.nextInt(16);
            BlockPos p = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            TreeWriter w = new TreeWriter(world, chunk);
            if (random.nextInt(18) == 0) {
                new WorldGenSilverwoodTrees(false, 8, 5).generate(w, random, p);
            } else if (random.nextInt(12) == 0) {
                new WorldGenGreatwoodTrees(false, random.nextInt(8) == 0).generate(w, random, p);
            } else {
                new WorldGenBigMagicTree().generate(w, random, p);
            }
        }
        return true;
    }

    private void vishrooms(WorldGenLevel world, RandomSource random, int cx, int cz) {
        for (int a = 0; a < 8; a++) {
            int x = cx + random.nextInt(16);
            int z = cz + random.nextInt(16);
            BlockPos p2 = new BlockPos(x, world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z), z);
            while (p2.getY() > 50 && !world.getBlockState(p2).is(Blocks.GRASS_BLOCK)) {
                p2 = p2.below();
            }
            if (world.getBlockState(p2).is(Blocks.GRASS_BLOCK) && world.getBlockState(p2.above()).canBeReplaced() && adjacentToWood(world, p2.above())) {
                world.setBlock(p2.above(), BlocksTC.vishroom.get().defaultBlockState(), 2);
            }
        }
    }

    /** isBlockAdjacentToWood: бревно среди 26 соседей (в пределах своего чанка и соседних). */
    private static boolean adjacentToWood(WorldGenLevel world, BlockPos pos) {
        for (int xx = -1; xx <= 1; xx++) {
            for (int yy = -1; yy <= 1; yy++) {
                for (int zz = -1; zz <= 1; zz++) {
                    if (xx != 0 || yy != 0 || zz != 0) {
                        BlockState s = world.getBlockState(pos.offset(xx, yy, zz));
                        if (s.is(BlockTags.LOGS)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
