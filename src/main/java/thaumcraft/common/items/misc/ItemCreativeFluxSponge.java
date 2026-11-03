package thaumcraft.common.items.misc;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.lib.SoundsTC;

/**
 * Творческая губка Выброса (порт ItemCreativeFluxSponge): поглощает до 500 Выброса из каждого чанка области 9×9;
 * при использовании крадучись удаляет разломы в области ±32 блока. Незагруженные чанки пропускаются.
 */
public class ItemCreativeFluxSponge extends Item {
    public ItemCreativeFluxSponge(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tc.flux_sponge.tooltip.1").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tc.flux_sponge.tooltip.2").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tc.flux_sponge.tooltip.3").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tc.flux_sponge.tooltip.4").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tc.flux_sponge.creative").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (world.isClientSide) {
            world.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundsTC.craftstart.get(),
                    SoundSource.PLAYERS, 0.15F, 1.0F, false);
        } else {
            int drained = 0;
            BlockPos origin = player.blockPosition();
            for (int x = -4; x <= 4; x++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = origin.offset(16 * x, 0, 16 * z);
                    if (world.hasChunkAt(pos)) {
                        drained = (int) (drained + AuraHelper.drainFlux(world, pos, 500.0F, false));
                    }
                }
            }
            player.sendSystemMessage(Component.translatable("tc.flux_sponge.drained", drained).withStyle(ChatFormatting.GREEN));
            if (player.isShiftKeyDown()) {
                var rifts = world.getEntitiesOfClass(EntityFluxRift.class, new AABB(player.position(), player.position()).inflate(32.0));
                for (EntityFluxRift rift : rifts) {
                    rift.discard();
                }
                player.sendSystemMessage(Component.translatable("tc.flux_sponge.removed", rifts.size()).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), world.isClientSide);
    }
}
