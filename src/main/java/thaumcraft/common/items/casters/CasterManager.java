package thaumcraft.common.items.casters;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.casters.ICaster;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.common.config.ConfigPotions;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Порт CasterManager: скидка Вис, расход Вис с перчаток в инвентаре, перезарядка заклинаний.
 * Перезарядка — по id существа, отдельные таблицы для клиента и сервера (как в оригинале); серверная очищается при
 * остановке сервера, клиентская — при выходе из мира. TODO этап 12.6: смена фокуса (changeFocus), toggleMisc («План»).
 */
public class CasterManager {
    static final Map<Integer, Long> cooldownServer = new HashMap<>();
    static final Map<Integer, Long> cooldownClient = new HashMap<>();

    /**
     * Сумма скидок аксессуаров (Baubles → Curios) и брони, в долях; «Истощение Вис» — минус 10% за уровень.
     * TODO этап 15: «Заразное истощение Вис» (PotionInfectiousVisExhaust) — тот же штраф, берётся больший уровень.
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
        MobEffectInstance exhaust = player.getEffect(ConfigPotions.VIS_EXHAUST);
        if (exhaust != null) {
            total -= (exhaust.getAmplifier() + 1) * 10;
        }
        return total / 100.0F;
    }

    /** Вис для ремесла — с первой перчатки в основном инвентаре (с конца), которая может заплатить. */
    public static boolean consumeVisFromInventory(Player player, float cost) {
        for (int a = player.getInventory().items.size() - 1; a >= 0; a--) {
            ItemStack item = player.getInventory().items.get(a);
            if (item.getItem() instanceof ICaster caster && caster.consumeVis(item, player, cost, true, false)) {
                return true;
            }
        }
        return false;
    }

    private static Map<Integer, Long> table(LivingEntity e) {
        return e.level().isClientSide ? cooldownClient : cooldownServer;
    }

    static boolean isOnCooldown(LivingEntity entityLiving) {
        Long t = table(entityLiving).get(entityLiving.getId());
        return t != null && t > System.currentTimeMillis();
    }

    /** Оставшаяся перезарядка в секундах (только клиент, для HUD). */
    public static float getCooldown(LivingEntity entityLiving) {
        Long t = entityLiving.level().isClientSide ? cooldownClient.get(entityLiving.getId()) : null;
        return t != null ? (t - System.currentTimeMillis()) / 1000.0F : 0.0F;
    }

    public static void setCooldown(LivingEntity entityLiving, int cd) {
        if (cd == 0) {
            cooldownClient.remove(entityLiving.getId());
            cooldownServer.remove(entityLiving.getId());
        } else {
            table(entityLiving).put(entityLiving.getId(), System.currentTimeMillis() + cd * 50L);
        }
    }

    public static void clearServer() {
        cooldownServer.clear();
    }

    public static void clearClient() {
        cooldownClient.clear();
    }
}
