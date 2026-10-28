package thaumcraft.api.items;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Предмет с зарядом Вис (порт IRechargable): заряжается амулетом Вис, пьедесталом подзарядки и т.п. */
public interface IRechargable {
    int getMaxCharge(ItemStack stack, LivingEntity player);

    EnumChargeDisplay showInHud(ItemStack stack, LivingEntity player);

    enum EnumChargeDisplay {
        NEVER,
        NORMAL,
        PERIODIC
    }
}
