package thaumcraft.common.world.features;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.blocks.world.ore.BlockCrystal;
import thaumcraft.common.blocks.world.ore.ShardType;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.world.biomes.BiomeHandler;

/**
 * Руды и вис-кристаллы (generateOres ThaumcraftWorldGenerator): раз на чанк, точки — у центра чанка (±6).
 * Киноварь — 18 попыток на высоте 0–50, кварц — 18 на 0–63, янтарь — 20 до 24 блоков под поверхностью; заменяется
 * только камень (с гранитом, диоритом, андезитом — в 1.12 это один блок). Кристаллы — до 8 гнёзд 3×3×3 ниже поверхности,
 * у камня, каждый третий — аспекта биома. Количество × oreDensity/100. Высоты — абсолютные, как в мире 1.12.
 */
public class FeatureOres extends Feature<NoneFeatureConfiguration> {

    public FeatureOres(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private static Block crystal(int md) {
        return switch (md) {
            case 1 -> BlocksTC.crystalFire.get();
            case 2 -> BlocksTC.crystalWater.get();
            case 3 -> BlocksTC.crystalEarth.get();
            case 4 -> BlocksTC.crystalOrder.get();
            case 5 -> BlocksTC.crystalEntropy.get();
            case 6 -> BlocksTC.crystalTaint.get();
            default -> BlocksTC.crystalAir.get();
        };
    }

    private static boolean replaceable(BlockState state) {
        return state.is(BlockTags.STONE_ORE_REPLACEABLES);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        RandomSource random = ctx.random();
        int cx = ctx.origin().getX() & ~15;
        int cz = ctx.origin().getZ() & ~15;
        float density = ModConfig.CONFIG_WORLD.oreDensity.get() / 100.0F;
        if (ModConfig.CONFIG_WORLD.generateCinnabar.get()) {
            for (int i = 0; i < Math.round(18.0F * density); i++) {
                BlockPos pos = new BlockPos(cx + 8 + Mth.nextInt(random, -6, 6), random.nextInt(256 / 5), cz + 8 + Mth.nextInt(random, -6, 6));
                if (replaceable(world.getBlockState(pos))) {
                    world.setBlock(pos, BlocksTC.oreCinnabar.get().defaultBlockState(), 2);
                }
            }
        }
        if (ModConfig.CONFIG_WORLD.generateQuartz.get()) {
            for (int i = 0; i < Math.round(18.0F * density); i++) {
                BlockPos pos = new BlockPos(cx + 8 + Mth.nextInt(random, -6, 6), random.nextInt(256 / 4), cz + 8 + Mth.nextInt(random, -6, 6));
                if (replaceable(world.getBlockState(pos))) {
                    world.setBlock(pos, BlocksTC.oreQuartz.get().defaultBlockState(), 2);
                }
            }
        }
        if (ModConfig.CONFIG_WORLD.generateAmber.get()) {
            for (int i = 0; i < Math.round(20.0F * density); i++) {
                int x = cx + 8 + Mth.nextInt(random, -6, 6);
                int z = cz + 8 + Mth.nextInt(random, -6, 6);
                BlockPos pos = new BlockPos(x, world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - random.nextInt(25), z);
                if (replaceable(world.getBlockState(pos))) {
                    world.setBlock(pos, BlocksTC.oreAmber.get().defaultBlockState(), 2);
                }
            }
        }
        if (ModConfig.CONFIG_WORLD.generateCrystals.get()) {
            int maxCrystals = Math.round(64.0F * density);
            int cc = 0;
            for (int i = 0; i < Math.round(8 * density); i++) {
                int x = cx + 8 + Mth.nextInt(random, -6, 6);
                int z = cz + 8 + Mth.nextInt(random, -6, 6);
                int y = random.nextInt(Math.max(5, world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 5));
                BlockPos bp = new BlockPos(x, y, z);
                int md = random.nextInt(6);
                if (random.nextInt(3) == 0) {
                    Aspect tag = BiomeHandler.getRandomBiomeTag(world.getBiome(bp), random);
                    md = tag == null ? random.nextInt(6) : ShardType.getMetaByAspect(tag);
                }
                Block oreBlock = crystal(md);
                for (int xx = -1; xx <= 1; xx++) {
                    for (int yy = -1; yy <= 1; yy++) {
                        for (int zz = -1; zz <= 1; zz++) {
                            if (random.nextInt(3) != 0) {
                                BlockPos p = bp.offset(xx, yy, zz);
                                BlockState bs = world.getBlockState(p);
                                if (bs.getFluidState().isEmpty() && (bs.isAir() || bs.canBeReplaced()) && BlockCrystal.isTouchingRock(world, p)) {
                                    int amt = 1 + random.nextInt(3);
                                    BlockState cs = oreBlock.defaultBlockState().setValue(BlockCrystal.SIZE, amt);
                                    if (oreBlock instanceof BlockCrystal c) {
                                        cs = c.withConnections(cs, world, p);
                                    }
                                    world.setBlock(p, cs, 2);
                                    cc += amt;
                                }
                            }
                        }
                    }
                }
                if (cc > maxCrystals) {
                    break;
                }
            }
        }
        return true;
    }
}
