package thaumcraft.common.items.baubles;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.items.RechargeHelper;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Камень Вис («found», найденный) и амулет Вис («crafted») — порт ItemAmuletVis. Надетый на шею, раз в 40 (камень) или 5
 * (амулет) тиков заряжает на 1 Вис из ауры первый подходящий предмет: хотбар, затем безделушки, затем броня.
 */
public class ItemAmuletVis extends Item implements ICurioItem {
    private final boolean crafted;

    public ItemAmuletVis(boolean crafted, Properties props) {
        super(props);
        this.crafted = crafted;
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!(slotContext.entity() instanceof Player player) || player.level().isClientSide
                || player.tickCount % (this.crafted ? 5 : 40) != 0) {
            return;
        }
        for (int a = 0; a < 9; a++) {
            if (recharge(player, player.getInventory().items.get(a))) {
                return;
            }
        }
        IItemHandler baubles = CuriosApi.getCuriosInventory(player).map(h -> (IItemHandler) h.getEquippedCurios()).orElse(null);
        if (baubles != null) {
            for (int a = 0; a < baubles.getSlots(); a++) {
                if (recharge(player, baubles.getStackInSlot(a))) {
                    return;
                }
            }
        }
        for (ItemStack armor : player.getInventory().armor) {
            if (recharge(player, armor)) {
                return;
            }
        }
    }

    private static boolean recharge(Player player, ItemStack stack) {
        return RechargeHelper.rechargeItem(player.level(), stack, player.blockPosition(), player, 1) > 0.0F;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.amulet_vis.text").withStyle(ChatFormatting.AQUA));
    }
}
