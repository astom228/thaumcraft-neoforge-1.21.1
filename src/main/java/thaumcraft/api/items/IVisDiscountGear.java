package thaumcraft.api.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Броня или аксессуар (Curios), снижающий стоимость Вис, в процентах. */
public interface IVisDiscountGear {
    int getVisDiscount(ItemStack stack, Player player);
}
