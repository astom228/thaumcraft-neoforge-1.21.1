package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Предмет, открывающий скрытое (разломы выброса и т.п.). */
public interface IRevealer {
    boolean showNodes(ItemStack stack, LivingEntity player);
}
