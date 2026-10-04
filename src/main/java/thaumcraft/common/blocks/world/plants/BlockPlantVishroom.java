package thaumcraft.common.blocks.world.plants;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.client.fx.FXDispatcher;

/**
 * Висомор: свет 6 (0.4). Тип растения Cave — растёт на любой твёрдой верхней грани (плюс трава/земля/пашня).
 * Касание существа: с шансом 1/5 тошнота на 200 тиков.
 */
public class BlockPlantVishroom extends BlockPlantTC {
    public static final MapCodec<BlockPlantVishroom> CODEC = simpleCodec(BlockPlantVishroom::new);

    public BlockPlantVishroom(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return isGrassOrDirt(state) || state.is(Blocks.FARMLAND) || state.isFaceSturdy(level, pos, Direction.UP);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && level.random.nextInt(5) == 0) {
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(3) == 0) {
            float xr = pos.getX() + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.4F;
            float yr = pos.getY() + 0.3F;
            float zr = pos.getZ() + 0.5F + (rand.nextFloat() - rand.nextFloat()) * 0.4F;
            FXDispatcher.INSTANCE.drawWispyMotes(xr, yr, zr, 0.0, 0.0, 0.0, 10, 0.5F, 0.3F, 0.8F, 0.001F);
        }
    }
}
