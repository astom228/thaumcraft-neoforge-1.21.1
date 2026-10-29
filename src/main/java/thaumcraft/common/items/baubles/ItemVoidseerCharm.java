package thaumcraft.common.items.baubles;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Жемчужина провидца пустоты (порт ItemVoidseerCharm): слот оберега; скидка Вис до 25% — по постоянному Искажению
 * (100 и больше — полная), Искажение от неё — скидка / 5.
 */
public class ItemVoidseerCharm extends Item implements ICurioItem, IVisDiscountGear, IWarpingGear {
    public ItemVoidseerCharm(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.voidseer_charm.text").withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.ITALIC));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        int q = 0;
        IPlayerWarp warp = ThaumcraftCapabilities.getWarp(player);
        if (warp != null) {
            int pw = Math.min(100, warp.get(IPlayerWarp.EnumWarpType.PERMANENT));
            q = (int) (pw / 100.0F * 25.0F);
        }
        return q;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return this.getVisDiscount(stack, player) / 5;
    }
}
