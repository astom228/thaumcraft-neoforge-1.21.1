package thaumcraft.common.blocks.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Очищающая жидкость (порт BlockFluidPure): замедляет сущности по высоте жидкости; игрок без «Защиты от Искажения»,
 * вошедший в источник, получает её (200000 / √(постоянное Искажение) тиков, не больше 32000), источник исчезает.
 */
public class BlockFluidPure extends LiquidBlock {
    public BlockFluidPure(FlowingFluid fluid, Properties props) {
        super(fluid, props);
    }

    /** getQuantaPercentage 1.12: доля «кванта» = количество жидкости / 8. */
    static float quanta(BlockState state) {
        return state.getFluidState().getAmount() / 8.0F;
    }

    static void slow(BlockState state, Entity entity) {
        float k = 1.0F - quanta(state) / 2.0F;
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(k, 1.0, k));
    }

    @Override
    protected void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        slow(state, entity);
        if (!world.isClientSide && state.getFluidState().isSource() && entity instanceof Player player
                && !player.hasEffect(ConfigPotions.WARP_WARD)) {
            int warp = ThaumcraftCapabilities.getWarp(player).get(IPlayerWarp.EnumWarpType.PERMANENT);
            int div = 1;
            if (warp > 0) {
                div = Math.max(1, (int) Math.sqrt(warp));
            }
            player.addEffect(new MobEffectInstance(ConfigPotions.WARP_WARD, Math.min(32000, 200000 / div), 0, true, true));
            world.removeBlock(pos, false);
        }
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource rand) {
        // meta 1.12 = 8 − количество жидкости
        int meta = 8 - state.getFluidState().getAmount();
        if (rand.nextInt(10) == 0) {
            IFXDispatcher.get().purifyingFluidFx(pos.getX() + rand.nextFloat(), pos.getY() + 0.125F * (8 - meta), pos.getZ() + rand.nextFloat());
        }
        if (rand.nextInt(50) == 0) {
            world.playLocalSound(pos.getX() + rand.nextFloat(), pos.getY() + 0.5, pos.getZ() + rand.nextFloat(), SoundEvents.LAVA_POP,
                    SoundSource.BLOCKS, 0.1F + rand.nextFloat() * 0.1F, 0.9F + rand.nextFloat() * 0.15F, false);
        }
    }
}
