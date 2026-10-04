package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Тлеющая жемчужина: свет 7 (0.5), растёт на песке, земле и терракоте; дымит и горит огоньком. */
public class BlockPlantCinderpearl extends BlockPlantTC {
    public static final MapCodec<BlockPlantCinderpearl> CODEC = simpleCodec(BlockPlantCinderpearl::new);

    public BlockPlantCinderpearl(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        // canSustainBush: SAND, DIRT, STAINED_HARDENED_CLAY, HARDENED_CLAY; тип Desert: SAND и терракота
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.PODZOL) || state.is(BlockTags.TERRACOTTA);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextBoolean()) {
            float xr = pos.getX() + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.1F;
            float yr = pos.getY() + 0.6F + (rand.nextFloat() - rand.nextFloat()) * 0.1F;
            float zr = pos.getZ() + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.1F;
            level.addParticle(ParticleTypes.SMOKE, xr, yr, zr, 0.0, 0.0, 0.0);
            level.addParticle(ParticleTypes.FLAME, xr, yr, zr, 0.0, 0.0, 0.0);
        }
    }
}
