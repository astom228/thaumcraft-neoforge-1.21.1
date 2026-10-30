package thaumcraft.common.blocks.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.lighting.LightEngine;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Живая трава (порт BlockGrassAmbient): блок травы Магического леса; в сумерках и ночью над травой вокруг (±8) поднимаются
 * огоньки-пылинки, тем чаще, чем темнее.
 */
public class BlockGrassAmbient extends GrassBlock {
    public BlockGrassAmbient(Properties properties) {
        super(properties);
    }

    /** updateTick BlockGrass 1.12 (Blocks.GRASS в коде): гаснет в темноте под блоком, а на землю рядом распространяет обычную траву. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canBeGrass(level, pos)) {
            if (!level.isAreaLoaded(pos, 1)) {
                return;
            }
            level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        } else if (level.getMaxLocalRawBrightness(pos.above()) >= 9) {
            BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
            for (int i = 0; i < 4; i++) {
                BlockPos p = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
                if (level.isLoaded(p) && level.getBlockState(p).is(Blocks.DIRT) && canBeGrass(level, p) && !level.getFluidState(p.above()).is(FluidTags.WATER)) {
                    level.setBlockAndUpdate(p, grass.setValue(SNOWY, level.getBlockState(p.above()).is(Blocks.SNOW)));
                }
            }
        }
    }

    private static boolean canBeGrass(ServerLevel level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState s = level.getBlockState(above);
        if (s.is(Blocks.SNOW) && s.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        if (s.getFluidState().getAmount() == 8) {
            return false;
        }
        return LightEngine.getLightBlockInto(level, Blocks.GRASS_BLOCK.defaultBlockState(), pos, s, above, Direction.UP, s.getLightBlock(level, above)) < level.getMaxLightLevel();
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        int i = world.getBrightness(LightLayer.SKY, pos.above()) - world.getSkyDarken();
        float f = world.getSunAngle(1.0F);
        float f1 = f < (float) Math.PI ? 0.0F : (float) (Math.PI * 2);
        f += (f1 - f) * 0.2F;
        i = Math.round(i * Mth.cos(f));
        i = Mth.clamp(i, 0, 15);
        if (4 + i * 2 < 1 + rand.nextInt(13)) {
            BlockPos pp = pos.offset(Mth.nextInt(rand, -8, 8), 5, Mth.nextInt(rand, -8, 8));
            for (int q = 0; q < 10 && pp.getY() > 50 && !world.getBlockState(pp).is(Blocks.GRASS_BLOCK); q++) {
                pp = pp.below();
            }
            if (world.getBlockState(pp).is(Blocks.GRASS_BLOCK)) {
                IFXDispatcher.get().drawWispyMotesOnBlock(pp.above(), 400, -0.01F);
            }
        }
    }
}
