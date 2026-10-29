package thaumcraft.common.items.consumables;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Мыло рассудка (порт ItemSanitySoap): мыться 5 с (держать ПКМ); в конце снимает всё временное Искажение и 1 обычное
 * (+1 под «Защитой от Искажения», +1 стоя в очищающей жидкости). Пена и звуки — на клиенте.
 */
public class ItemSanitySoap extends Item {
    public ItemSanitySoap(Properties props) {
        super(props);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 100;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BLOCK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack stack, int count) {
        int ticks = this.getUseDuration(stack, player) - count;
        if (ticks > 95) {
            player.releaseUsingItem();
        }
        if (level.isClientSide) {
            if (level.random.nextFloat() < 0.2F) {
                level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.PLAYERS, 0.1F,
                        1.5F + level.random.nextFloat() * 0.2F, false);
            }
            for (int a = 0; a < 10; a++) {
                IFXDispatcher.get().crucibleBubble((float) player.getX() - 0.5F + level.random.nextFloat(),
                        (float) player.getBoundingBox().minY + level.random.nextFloat() * player.getBbHeight(),
                        (float) player.getZ() - 0.5F + level.random.nextFloat(), 1.0F, 0.8F, 0.9F);
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level world, LivingEntity player, int timeLeft) {
        int qq = this.getUseDuration(stack, player) - timeLeft;
        if (qq > 95 && player instanceof Player p) {
            stack.consume(1, p);
            if (p instanceof ServerPlayer sp) {
                IPlayerWarp warp = ThaumcraftCapabilities.getWarp(sp);
                int amt = 1;
                if (sp.hasEffect(ConfigPotions.WARP_WARD)) {
                    amt++;
                }
                if (world.getBlockState(sp.blockPosition()).getBlock() == BlocksTC.purifyingFluid.get()) {
                    amt++;
                }
                if (warp.get(IPlayerWarp.EnumWarpType.NORMAL) > 0) {
                    ThaumcraftApi.internalMethods.addWarpToPlayer(sp, -amt, IPlayerWarp.EnumWarpType.NORMAL);
                }
                if (warp.get(IPlayerWarp.EnumWarpType.TEMPORARY) > 0) {
                    ThaumcraftApi.internalMethods.addWarpToPlayer(sp, -warp.get(IPlayerWarp.EnumWarpType.TEMPORARY), IPlayerWarp.EnumWarpType.TEMPORARY);
                }
            } else if (world.isClientSide) {
                world.playLocalSound(p.getX(), p.getY(), p.getZ(), SoundsTC.craftstart.get(), SoundSource.PLAYERS, 0.25F, 1.0F, false);
                for (int a = 0; a < 40; a++) {
                    IFXDispatcher.get().crucibleBubble((float) p.getX() - 0.5F + world.random.nextFloat() * 1.5F,
                            (float) p.getBoundingBox().minY + world.random.nextFloat() * p.getBbHeight(),
                            (float) p.getZ() - 0.5F + world.random.nextFloat() * 1.5F, 1.0F, 0.7F, 0.9F);
                }
            }
        }
    }
}
