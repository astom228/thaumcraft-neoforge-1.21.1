package thaumcraft.api.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.common.lib.TCDataComponents;

/** Заряд Вис предметов IRechargable (порт RechargeHelper; NBT "tc.charge" → компонент thaumcraft:charge). Менять — только на сервере. */
public class RechargeHelper {
    /** Заряжает предмет из ауры в точке pos; возвращает, сколько Вис взято. */
    public static float rechargeItem(Level world, ItemStack is, BlockPos pos, Player player, int amt) {
        if (is != null && !is.isEmpty() && is.getItem() instanceof IRechargable chargeItem) {
            if (player != null && AuraHelper.shouldPreserveAura(world, player, pos)) {
                return 0.0F;
            }
            amt = Math.min(amt, chargeItem.getMaxCharge(is, player) - getCharge(is));
            int drained = (int) AuraHelper.drainVis(world, pos, amt, false);
            if (drained > 0) {
                addCharge(is, player, drained);
                return drained;
            }
            return 0.0F;
        }
        return 0.0F;
    }

    public static float rechargeItemBlindly(ItemStack is, Player player, int amt) {
        if (is != null && !is.isEmpty() && is.getItem() instanceof IRechargable chargeItem) {
            amt = Math.min(amt, chargeItem.getMaxCharge(is, player) - getCharge(is));
            if (amt > 0) {
                addCharge(is, player, amt);
            }
            return amt;
        }
        return 0.0F;
    }

    private static void addCharge(ItemStack is, LivingEntity player, int amt) {
        if (is != null && !is.isEmpty() && is.getItem() instanceof IRechargable chargeItem) {
            int amount = Math.min(chargeItem.getMaxCharge(is, player), amt + getCharge(is));
            is.set(TCDataComponents.CHARGE.get(), amount);
        }
    }

    /** Заряд предмета; −1 — предмет не заряжаемый. */
    public static int getCharge(ItemStack is) {
        if (is == null || is.isEmpty() || !(is.getItem() instanceof IRechargable)) {
            return -1;
        }
        return is.getOrDefault(TCDataComponents.CHARGE.get(), 0);
    }

    public static float getChargePercentage(ItemStack is, Player player) {
        if (is != null && !is.isEmpty() && is.getItem() instanceof IRechargable chargeItem) {
            float c = getCharge(is);
            float m = chargeItem.getMaxCharge(is, player);
            return c / m;
        }
        return -1.0F;
    }

    public static boolean consumeCharge(ItemStack is, LivingEntity player, int amt) {
        if (is != null && !is.isEmpty() && is.getItem() instanceof IRechargable) {
            int charge = is.getOrDefault(TCDataComponents.CHARGE.get(), 0);
            if (charge >= amt) {
                is.set(TCDataComponents.CHARGE.get(), charge - amt);
                return true;
            }
        }
        return false;
    }
}
