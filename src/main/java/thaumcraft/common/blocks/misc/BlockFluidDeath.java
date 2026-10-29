package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Жидкая смерть (порт BlockFluidDeath): «квант» 4; замедляет сущности и растворяет живых — урон 5 у источника, 2 у края
 * (4 − meta + 1, мимо брони).
 */
public class BlockFluidDeath extends LiquidBlock {
    public BlockFluidDeath(FlowingFluid fluid, Properties props) {
        super(fluid, props);
    }

    /** meta 1.12 при кванте 4: источник 0, далее 1–3 (количество 8, 6, 4, 2). */
    static int meta(BlockState state) {
        return (8 - state.getFluidState().getAmount()) / 2;
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        BlockFluidPure.slow(state, entity);
        if (!world.isClientSide && entity instanceof LivingEntity) {
            entity.hurt(DamageSourceThaumcraft.dissolve(world), 4 - meta(state) + 1);
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(20) == 0) {
            float h = rand.nextFloat() * 0.075F;
            IFXDispatcher.get().slimyBubble(pos.getX() + rand.nextFloat(), pos.getY() + 0.1F + 0.225F * (4 - meta(state)), pos.getZ() + rand.nextFloat(),
                    0.075F + h, 0.8F, 0.3F - rand.nextFloat() * 0.1F, 0.0F, 0.4F + rand.nextFloat() * 0.1F);
        }
        if (rand.nextInt(50) == 0) {
            // getMaxRenderHeightMeta 1.12 = кванты − 1 = 3
            world.playLocalSound(pos.getX() + rand.nextFloat(), pos.getY() + 3 / 4.0F, pos.getZ() + rand.nextFloat(), SoundEvents.LAVA_POP,
                    SoundSource.BLOCKS, 0.1F + rand.nextFloat() * 0.1F, 0.9F + rand.nextFloat() * 0.15F, false);
        }
    }
}
