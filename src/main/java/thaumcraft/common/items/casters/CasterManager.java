package thaumcraft.common.items.casters;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.items.IVisDiscountGear;
import top.theillusivec4.curios.api.CuriosApi;

/** Порт CasterManager. Пока — только скидка Вис; остальное (перезарядка, фокусы) — на этапе 12. */
public class CasterManager {

    /**
     * Сумма скидок аксессуаров (Baubles → Curios) и брони, в долях. Штраф от «Истощения Вис» (−10% за уровень)
     * добавится вместе с эффектами на этапе 15.
     */
    public static float getTotalVisDiscount(Player player) {
        int total = 0;
        if (player == null) {
            return 0.0F;
        }
        IItemHandler baubles = CuriosApi.getCuriosInventory(player).map(h -> (IItemHandler) h.getEquippedCurios()).orElse(null);
        if (baubles != null) {
            for (int a = 0; a < baubles.getSlots(); a++) {
                ItemStack stack = baubles.getStackInSlot(a);
                if (stack.getItem() instanceof IVisDiscountGear gear) {
                    total += gear.getVisDiscount(stack, player);
                }
            }
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof IVisDiscountGear gear) {
                total += gear.getVisDiscount(stack, player);
            }
        }
        // TODO этап 15: PotionVisExhaust / PotionInfectiousVisExhaust: total -= (max(уровень) + 1) * 10
        return total / 100.0F;
    }
}
