package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Предмет, показывающий аспекты содержимого и прочие подсказки в мире (очки откровения и т.п.). */
public interface IGoggles {
    boolean showIngamePopups(ItemStack stack, LivingEntity player);
}
