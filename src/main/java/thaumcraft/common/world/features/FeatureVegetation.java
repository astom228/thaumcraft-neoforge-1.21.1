package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ModConfig;

/**
 * Растительность (generateVegetation ThaumcraftWorldGenerator), раз на чанк Верхнего мира: серебродрево (1/80),
 * великое дерево (1/25) — этап 17.2; огнецвет — в жарких песчаных биомах (пустыни, бесплодные земли; 1/30).
 */
public class FeatureVegetation extends Feature<NoneFeatureConfiguration> {
    public FeatureVegetation(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!ModConfig.CONFIG_WORLD.generateTrees.get()) {
            return false;
        }
        WorldGenLevel world = ctx.level();
        RandomSource random = ctx.random();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;
        int x = cx + 8;
        int z = cz + 8;
        BlockPos bp = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        Holder<Biome> biome = world.getBiome(bp);
        // biome.topBlock == SAND && температура > 1: пустыни и бесплодные земли (красный песок в 1.12 — тот же блок)
        if ((biome.is(Tags.Biomes.IS_DESERT) || biome.is(Tags.Biomes.IS_BADLANDS)) && biome.value().getBaseTemperature() > 1.0F && random.nextInt(30) == 0) {
            generateFlowers(world, random, bp, BlocksTC.cinderpearl.get().defaultBlockState());
        }
        return true;
    }

    /** WorldGenCustomFlowers: 18 попыток в ±7 по горизонтали и ±3 по высоте; ставит на траву или песок. */
    public static void generateFlowers(WorldGenLevel world, RandomSource random, BlockPos pos, BlockState plant) {
        for (int i = 0; i < 18; i++) {
            BlockPos p = pos.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));
            BlockState below = world.getBlockState(p.below());
            if (world.isEmptyBlock(p) && (below.is(Blocks.GRASS_BLOCK) || below.is(BlockTags.SAND))) {
                world.setBlock(p, plant, 2);
            }
        }
    }
}
