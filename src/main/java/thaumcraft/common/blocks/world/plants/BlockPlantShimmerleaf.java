package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.client.fx.FXDispatcher;

/** Мерцающий лист: свет 6 (0.4), растёт на траве/земле (canSustainBush) и пашне (тип Plains). */
public class BlockPlantShimmerleaf extends BlockPlantTC {
    public static final MapCodec<BlockPlantShimmerleaf> CODEC = simpleCodec(BlockPlantShimmerleaf::new);

    public BlockPlantShimmerleaf(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isGrassOrDirt(state) || state.is(Blocks.FARMLAND);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(3) == 0) {
            float xr = (float) (pos.getX() + 0.5F + rand.nextGaussian() * 0.1);
            float yr = (float) (pos.getY() + 0.4F + rand.nextGaussian() * 0.1);
            float zr = (float) (pos.getZ() + 0.5F + rand.nextGaussian() * 0.1);
            FXDispatcher.INSTANCE.drawWispyMotes(xr, yr, zr, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, 10,
                    0.3F + level.random.nextFloat() * 0.3F, 0.7F + level.random.nextFloat() * 0.3F, 0.7F + level.random.nextFloat() * 0.3F, 0.0F);
        }
    }
}
