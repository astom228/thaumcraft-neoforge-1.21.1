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
import thaumcraft.common.lib.TCDataComponents;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Порт CasterManager: скидка Вис, расход Вис с перчаток в инвентаре, перезарядка заклинаний.
 * Перезарядка — по id существа, отдельные таблицы для клиента и сервера (как в оригинале); серверная очищается при
 * остановке сервера, клиентская — при выходе из мира. TODO этап 12.6: смена фокуса (changeFocus).
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

    /**
     * Клавиша «разное» с фокусом «План»: mod 0 — увеличить размер области по выбранной оси (по всем при оси 0), после
     * предела — с нуля; mod 1 — сменить ось (0 — все, 1 — X, 2 — Z, 3 — Y).
     */
    public static void toggleMisc(ItemStack itemstack, net.minecraft.world.level.Level world, Player player, int mod) {
        if (!(itemstack.getItem() instanceof ICaster caster)) {
            return;
        }
        thaumcraft.api.casters.FocusPackage fp = ItemFocus.getPackage(caster.getFocusStack(itemstack));
        if (fp == null || !thaumcraft.api.casters.FocusEngine.doesPackageContainElement(fp, "thaumcraft.PLAN")) {
            return;
        }
        int dim = getAreaDim(itemstack);
        if (mod == 0) {
            int areax = getAreaX(itemstack);
            int areay = getAreaY(itemstack);
            int areaz = getAreaZ(itemstack);
            int max = getAreaSize(itemstack);
            if (dim == 0) {
                areax++;
                areaz++;
                areay++;
            } else if (dim == 1) {
                areax++;
            } else if (dim == 2) {
                areaz++;
            } else if (dim == 3) {
                areay++;
            }
            if (areax > max) {
                areax = 0;
            }
            if (areaz > max) {
                areaz = 0;
            }
            if (areay > max) {
                areay = 0;
            }
            setArea(itemstack, areax, areay, areaz, dim);
        }
        if (mod == 1) {
            if (++dim > 3) {
                dim = 0;
            }
            setArea(itemstack, getAreaX(itemstack), getAreaY(itemstack), getAreaZ(itemstack), dim);
        }
    }

    /** Предел размера области (в оригинале — 3; ветка «6» не включалась). */
    private static int getAreaSize(ItemStack itemstack) {
        return itemstack.getItem() instanceof ICaster ? 3 : 0;
    }

    private static TCDataComponents.CasterArea area(ItemStack stack) {
        TCDataComponents.CasterArea a = stack.get(TCDataComponents.CASTER_AREA.get());
        int s = getAreaSize(stack);
        return a != null ? a : new TCDataComponents.CasterArea(s, s, s, 0);
    }

    private static void setArea(ItemStack stack, int x, int y, int z, int dim) {
        stack.set(TCDataComponents.CASTER_AREA.get(), new TCDataComponents.CasterArea(x, y, z, dim));
    }

    public static int getAreaDim(ItemStack stack) {
        return area(stack).dim();
    }

    public static int getAreaX(ItemStack stack) {
        return Math.min(area(stack).x(), getAreaSize(stack));
    }

    public static int getAreaY(ItemStack stack) {
        return Math.min(area(stack).y(), getAreaSize(stack));
    }

    public static int getAreaZ(ItemStack stack) {
        return Math.min(area(stack).z(), getAreaSize(stack));
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
