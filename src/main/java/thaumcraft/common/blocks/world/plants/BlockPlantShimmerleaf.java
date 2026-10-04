package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

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
            // TODO этап 6: FXDispatcher.drawWispyMotes(pos + 0.5/0.4/0.5 ± gauss*0.1, motion gauss*0.01, 10,
            //   r 0.3..0.6, g 0.7..1.0, b 0.7..1.0, 0.0)
        }
    }
}
