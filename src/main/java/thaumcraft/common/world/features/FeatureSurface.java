package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;
import thaumcraft.common.world.objects.TreeWriter;
import thaumcraft.common.world.objects.WorldGenMound;

/**
 * Поверхность Верхнего мира (generateSurface ThaumcraftWorldGenerator), раз на чанк, при generateStructure: курган
 * (1/100) на 9 блоков ниже поверхности у центра чанка (±4), иначе (1/500) — малый багровый портал на земле, камне,
 * песке или снегу.
 */
public class FeatureSurface extends Feature<NoneFeatureConfiguration> {
    public FeatureSurface(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!ModConfig.CONFIG_WORLD.generateStructure.get()) {
            return false;
        }
        WorldGenLevel world = ctx.level();
        RandomSource random = ctx.random();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;
        int x = cx + 8 + Mth.nextInt(random, -4, 4);
        int z = cz + 8 + Mth.nextInt(random, -4, 4);
        BlockPos p = new BlockPos(x, world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z).below(9);
        if (p.getY() >= world.getMaxBuildHeight()) {
            return false;
        }
        if (random.nextInt(100) == 0) {
            return new WorldGenMound().generate(new TreeWriter(world, new ChunkPos(cx >> 4, cz >> 4)), random, p);
        }
        if (random.nextInt(500) == 0) {
            BlockPos p2 = p.above(8);
            BlockState bs = world.getBlockState(p2);
            if (bs.is(BlockTags.DIRT) || bs.is(BlockTags.BASE_STONE_OVERWORLD) || bs.is(BlockTags.SAND) || bs.is(Blocks.GRAVEL) || bs.is(Blocks.SNOW)
                    || bs.is(Blocks.SNOW_BLOCK)) {
                EntityCultistPortalLesser eg = ConfigEntities.CULTIST_PORTAL_LESSER.get().create(world.getLevel());
                if (eg != null) {
                    eg.moveTo(p2.getX() + 0.5, p2.getY() + 1, p2.getZ() + 0.5, 0.0F, 0.0F);
                    eg.finalizeSpawn(world, world.getCurrentDifficultyAt(p2), MobSpawnType.STRUCTURE, null);
                    world.addFreshEntityWithPassengers(eg);
                }
            }
        }
        return true;
    }
}
