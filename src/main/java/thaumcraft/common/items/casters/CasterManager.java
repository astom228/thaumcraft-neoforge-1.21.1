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
 * остановке сервера, клиентская — при выходе из мира.
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

    /** Ячейки Curios игрока (в 1.12 — Baubles); null — без Curios. */
    private static net.neoforged.neoforge.items.IItemHandlerModifiable curios(Player player) {
        return CuriosApi.getCuriosInventory(player).map(h -> (net.neoforged.neoforge.items.IItemHandlerModifiable) h.getEquippedCurios()).orElse(null);
    }

    /**
     * Сменить фокус перчатки (changeFocus оригинала): фокусы ищутся в сумках на поясе (Curios), в основном инвентаре и в
     * сумках в нём; ключ — getSortingHelper. focus — нужный ключ (иначе следующий по порядку, иначе первый); «REMOVE» —
     * снять фокус. Снятый фокус кладётся в сумку, иначе в инвентарь (если некуда — остаётся в перчатке).
     * Ячейки сумок: ≥ 0 — основной инвентарь, < 0 — Curios (−(номер + 1); в 1.12 — номер Baubles − 4).
     */
    public static void changeFocus(ItemStack is, net.minecraft.world.level.Level w, Player player, String focus) {
        ICaster wand = (ICaster) is.getItem();
        java.util.TreeMap<String, Integer> foci = new java.util.TreeMap<>();
        HashMap<Integer, Integer> pouches = new HashMap<>();
        int pouchcount = 0;
        ItemStack item;
        net.neoforged.neoforge.items.IItemHandlerModifiable baubles = curios(player);
        if (baubles != null) {
            for (int a = 0; a < baubles.getSlots(); a++) {
                ItemStack bs = baubles.getStackInSlot(a);
                if (bs.getItem() instanceof ItemFocusPouch pouch) {
                    pouchcount++;
                    pouches.put(pouchcount, -(a + 1));
                    net.minecraft.core.NonNullList<ItemStack> inv = pouch.getInventory(bs);
                    for (int q = 0; q < inv.size(); q++) {
                        item = inv.get(q);
                        if (item.getItem() instanceof ItemFocus f) {
                            String sh = f.getSortingHelper(item);
                            if (sh != null) {
                                foci.put(sh, q + pouchcount * 1000);
                            }
                        }
                    }
                }
            }
        }
        net.minecraft.core.NonNullList<ItemStack> main = player.getInventory().items;
        for (int a = 0; a < 36; a++) {
            item = main.get(a);
            if (item.getItem() instanceof ItemFocus f) {
                String sh = f.getSortingHelper(item);
                if (sh == null) {
                    continue;
                }
                foci.put(sh, a);
            }
            if (item.getItem() instanceof ItemFocusPouch pouch) {
                pouches.put(++pouchcount, a);
                net.minecraft.core.NonNullList<ItemStack> inv = pouch.getInventory(item);
                for (int q = 0; q < inv.size(); q++) {
                    ItemStack fi = inv.get(q);
                    if (fi.getItem() instanceof ItemFocus f) {
                        String sh = f.getSortingHelper(fi);
                        if (sh != null) {
                            foci.put(sh, q + pouchcount * 1000);
                        }
                    }
                }
            }
        }
        if (!focus.equals("REMOVE") && !foci.isEmpty()) {
            String newkey = focus;
            if (foci.get(newkey) == null) {
                newkey = foci.higherKey(newkey);
            }
            if (newkey == null || foci.get(newkey) == null) {
                newkey = foci.firstKey();
            }
            item = ItemStack.EMPTY;
            int code = foci.get(newkey);
            if (code < 1000 && code >= 0) {
                item = main.get(code).copy();
            } else {
                int pid = code / 1000;
                if (pouches.containsKey(pid)) {
                    int pouchslot = pouches.get(pid);
                    int focusslot = code - pid * 1000;
                    item = fetchFocusFromPouch(player, focusslot, pouchslot);
                }
            }
            if (item.isEmpty()) {
                return;
            }
            if (code < 1000 && code >= 0) {
                player.getInventory().setItem(code, ItemStack.EMPTY);
            }
            player.playSound(thaumcraft.common.lib.SoundsTC.ticks.get(), 0.3F, 1.0F);
            if (wand.getFocus(is) != null && (addFocusToPouch(player, wand.getFocusStack(is).copy(), pouches)
                    || player.getInventory().add(wand.getFocusStack(is).copy()))) {
                wand.setFocus(is, ItemStack.EMPTY);
            }
            if (wand.getFocus(is) == null) {
                wand.setFocus(is, item);
            } else if (!addFocusToPouch(player, item, pouches)) {
                if (!player.getInventory().add(item)) {
                    player.drop(item, false);
                }
            }
        } else if (wand.getFocus(is) != null && (addFocusToPouch(player, wand.getFocusStack(is).copy(), pouches)
                || player.getInventory().add(wand.getFocusStack(is).copy()))) {
            wand.setFocus(is, null);
            player.playSound(thaumcraft.common.lib.SoundsTC.ticks.get(), 0.3F, 0.9F);
        }
    }

    private static ItemStack pouchAt(Player player, int pouchslot) {
        if (pouchslot >= 0) {
            return player.getInventory().items.get(pouchslot);
        }
        net.neoforged.neoforge.items.IItemHandlerModifiable baubles = curios(player);
        return baubles == null ? ItemStack.EMPTY : baubles.getStackInSlot(-pouchslot - 1);
    }

    private static void pouchChanged(Player player, int pouchslot, ItemStack pouch) {
        if (pouchslot >= 0) {
            player.getInventory().setItem(pouchslot, pouch);
            player.getInventory().setChanged();
        } else {
            net.neoforged.neoforge.items.IItemHandlerModifiable baubles = curios(player);
            if (baubles != null) {
                baubles.setStackInSlot(-pouchslot - 1, pouch);
            }
        }
    }

    private static ItemStack fetchFocusFromPouch(Player player, int focusid, int pouchslot) {
        ItemStack pouch = pouchAt(player, pouchslot);
        if (!(pouch.getItem() instanceof ItemFocusPouch p)) {
            return ItemStack.EMPTY;
        }
        net.minecraft.core.NonNullList<ItemStack> inv = p.getInventory(pouch);
        ItemStack contents = inv.get(focusid);
        ItemStack focus = ItemStack.EMPTY;
        if (contents.getItem() instanceof ItemFocus) {
            focus = contents.copy();
            inv.set(focusid, ItemStack.EMPTY);
            p.setInventory(pouch, inv);
            pouchChanged(player, pouchslot, pouch);
        }
        return focus;
    }

    private static boolean addFocusToPouch(Player player, ItemStack focus, HashMap<Integer, Integer> pouches) {
        for (Integer pouchslot : pouches.values()) {
            ItemStack pouch = pouchAt(player, pouchslot);
            if (!(pouch.getItem() instanceof ItemFocusPouch p)) {
                continue;
            }
            net.minecraft.core.NonNullList<ItemStack> inv = p.getInventory(pouch);
            for (int q = 0; q < inv.size(); q++) {
                if (inv.get(q).isEmpty()) {
                    inv.set(q, focus.copy());
                    p.setInventory(pouch, inv);
                    pouchChanged(player, pouchslot, pouch);
                    return true;
                }
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
