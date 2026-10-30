package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.world.biomes.BiomeHandler;
import thaumcraft.common.world.objects.TreeWriter;
import thaumcraft.common.world.objects.WorldGenGreatwoodTrees;
import thaumcraft.common.world.objects.WorldGenSilverwoodTrees;

/**
 * Растительность (generateVegetation ThaumcraftWorldGenerator), раз на чанк Верхнего мира: серебродрево (1/80),
 * великое дерево (1/25); огнецвет — в жарких песчаных биомах (пустыни, бесплодные земли; 1/30).
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
        ChunkPos chunk = new ChunkPos(cx >> 4, cz >> 4);
        if (random.nextInt(80) == 3) {
            generateSilverwood(world, random, chunk);
        }
        if (random.nextInt(25) == 7) {
            generateGreatwood(world, random, chunk);
        }
        int x = cx + 8;
        int z = cz + 8;
        BlockPos bp = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        Holder<Biome> biome = world.getBiome(bp);
        // biome.topBlock == SAND && температура > 1: пустыни и бесплодные земли (красный песок в 1.12 — тот же блок)
        if ((biome.is(Tags.Biomes.IS_DESERT) || biome.is(Tags.Biomes.IS_BADLANDS)) && biome.value().getBaseTemperature() > 1.0F && random.nextInt(30) == 0) {
            generateFlowers(new TreeWriter(world, new ChunkPos(bp)), random, bp, BlocksTC.cinderpearl.get().defaultBlockState());
        }
        return true;
    }

    /** Великое дерево у центра чанка (±4) с шансом «поддержки» биома; каждое восьмое — с пауками. */
    public static boolean generateGreatwood(WorldGenLevel world, RandomSource random, ChunkPos chunk) {
        int x = chunk.getMinBlockX() + 8 + Mth.nextInt(random, -4, 4);
        int z = chunk.getMinBlockZ() + 8 + Mth.nextInt(random, -4, 4);
        BlockPos bp = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        return BiomeHandler.getBiomeSupportsGreatwood(world.getBiome(bp)) > random.nextFloat()
                && new WorldGenGreatwoodTrees(false, random.nextInt(8) == 0).generate(new TreeWriter(world, chunk), random, bp);
    }

    /**
     * Серебродрево: растёт, если биом «поддерживает» великое дерево (с половинным шансом), или в волшебном биоме кроме
     * Волшебного леса, или на холмах берёзового леса (условие оригинала дословно; лесные холмы в 1.21 слиты с лесом).
     */
    public static boolean generateSilverwood(WorldGenLevel world, RandomSource random, ChunkPos chunk) {
        int x = chunk.getMinBlockX() + 8 + Mth.nextInt(random, -4, 4);
        int z = chunk.getMinBlockZ() + 8 + Mth.nextInt(random, -4, 4);
        BlockPos bp = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        Holder<Biome> bio = world.getBiome(bp);
        boolean a = BiomeHandler.getBiomeSupportsGreatwood(bio) / 2.0F > random.nextFloat();
        boolean b = bio.is(BiomeHandler.MAGICAL_FOREST) || !bio.is(Tags.Biomes.IS_MAGICAL);
        boolean hills = bio.is(Biomes.OLD_GROWTH_BIRCH_FOREST);
        return (a || !b || hills) && new WorldGenSilverwoodTrees(false, 7, 4).generate(new TreeWriter(world, chunk), random, bp);
    }

    /** WorldGenCustomFlowers: 18 попыток в ±7 по горизонтали и ±3 по высоте; ставит на траву или песок. */
    public static void generateFlowers(TreeWriter w, RandomSource random, BlockPos pos, BlockState plant) {
        for (int i = 0; i < 18; i++) {
            BlockPos p = pos.offset(random.nextInt(8) - random.nextInt(8), random.nextInt(4) - random.nextInt(4), random.nextInt(8) - random.nextInt(8));
            BlockState below = w.get(p.below());
            if (w.isAir(p) && (below.is(Blocks.GRASS_BLOCK) || below.is(BlockTags.SAND))) {
                w.set(p, plant);
            }
        }
    }
}
