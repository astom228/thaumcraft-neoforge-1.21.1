package thaumcraft.common.lib.utils;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Работа с инвентарём игрока (порт InventoryUtils; методы добавляются по мере надобности).
 * NBT стека 1.12 → компоненты, отличающиеся от стандартных для предмета (getComponentsPatch).
 */
public class InventoryUtils {
    /**
     * Есть ли у игрока в основном инвентаре нужное количество предмета. Компоненты сравниваются «мягко», как NBT в оригинале
     * (если у образца компонентов нет — не сравниваются). Для «зачарованного заполнителя» достаточно одного предмета
     * с зачарованиями не ниже требуемых.
     */
    public static boolean isPlayerCarryingAmount(Player player, ItemStack stack, boolean ore) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        int count = stack.getCount();
        NonNullList<ItemStack> inv = player.getInventory().items;
        for (ItemStack is : inv) {
            if (!stack.is(ItemsTC.enchantedPlaceholder.get())) {
                if (areItemStacksEqualRelaxed(is, stack)) {
                    count -= is.getCount();
                    if (count <= 0) {
                        return true;
                    }
                }
            } else if (hasPlaceholderEnchantments(stack, is)) {
                return true;
            }
        }
        return false;
    }

    /** Забрать предмет из основного инвентаря (nocheck — без предварительной проверки количества). */
    public static boolean consumePlayerItem(Player player, ItemStack item, boolean nocheck, boolean ore) {
        if (!nocheck && !isPlayerCarryingAmount(player, item, ore)) {
            return false;
        }
        int count = item.getCount();
        NonNullList<ItemStack> inv = player.getInventory().items;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack is = inv.get(i);
            boolean match = item.is(ItemsTC.enchantedPlaceholder.get()) ? hasPlaceholderEnchantments(item, is)
                    : ItemStack.isSameItem(is, item) && areItemStacksEqualRelaxed(is, item);
            if (match) {
                if (is.getCount() > count) {
                    is.shrink(count);
                    count = 0;
                } else {
                    count -= is.getCount();
                    inv.set(i, ItemStack.EMPTY);
                }
                if (count <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * areItemStacksEqual(stack0, stack1, InvFilter(false, !stack1.hasTagCompound(), false, false).setRelaxedNBT()):
     * тот же предмет; если у образца (stack1) есть компоненты — каждый изменённый компонент stack0 должен совпадать с образцом.
     */
    public static boolean areItemStacksEqualRelaxed(ItemStack stack0, ItemStack stack1) {
        if (stack0.isEmpty() || stack1.isEmpty()) {
            return stack0.isEmpty() && stack1.isEmpty();
        }
        if (stack0.getItem() != stack1.getItem()) {
            return false;
        }
        if (stack1.getComponentsPatch().isEmpty()) {
            return true;
        }
        if (stack0.getComponentsPatch().isEmpty()) {
            return false;
        }
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : stack0.getComponentsPatch().entrySet()) {
            Optional<?> other = stack1.getComponentsPatch().get(e.getKey());
            if (other == null || !other.equals(e.getValue())) {
                return false;
            }
        }
        return true;
    }

    /** Зачарования заполнителя (в оригинале — EnchantmentHelper по NBT "ench", т.е. без зачарованных книг). */
    private static boolean hasPlaceholderEnchantments(ItemStack placeholder, ItemStack is) {
        Map<ResourceKey<Enchantment>, Integer> en = placeholder.get(TCDataComponents.PLACEHOLDER_ENCHANTMENTS.get());
        if (en == null || en.isEmpty()) {
            return false;
        }
        ItemEnchantments have = is.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Map.Entry<ResourceKey<Enchantment>, Integer> e : en.entrySet()) {
            int l = 0;
            for (Holder<Enchantment> h : have.keySet()) {
                if (h.is(e.getKey())) {
                    l = have.getLevel(h);
                    break;
                }
            }
            if (l < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** Номер слота основного инвентаря с точно таким же предметом (без учёта количества) или −1. */
    public static int getPlayerSlotFor(Player player, ItemStack stack) {
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (!player.getInventory().items.get(i).isEmpty() && stackEqualExact(stack, player.getInventory().items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /** Тот же предмет с теми же компонентами (в 1.12 — предмет, metadata и NBT). */
    public static boolean stackEqualExact(ItemStack stack1, ItemStack stack2) {
        return ItemStack.isSameItemSameComponents(stack1, stack2);
    }

    /** Вставить стак в инвентарь блока со стороны side (IItemHandler); незагруженный чанк не трогается. Возвращает остаток. */
    public static ItemStack insertStackAt(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side, ItemStack stack,
                                          boolean simulate) {
        if (!world.isLoaded(pos)) {
            return stack;
        }
        net.neoforged.neoforge.items.IItemHandler inventory = world.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, pos, side);
        return inventory == null ? stack : net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(inventory, stack, simulate);
    }

    /** ejectStackAt: в инвентарь перед стороной side, иначе — предметом, вылетающим в эту сторону. */
    public static void ejectStackAt(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side, ItemStack out) {
        out = insertStackAt(world, pos.relative(side), side.getOpposite(), out, false);
        if (!out.isEmpty()) {
            if (world.getBlockState(pos.relative(side)).isCollisionShapeFullBlock(world, pos.relative(side))) {
                pos = pos.relative(side.getOpposite());
            }
            net.minecraft.world.entity.item.ItemEntity e = new net.minecraft.world.entity.item.ItemEntity(world, pos.getX() + 0.5 + side.getStepX(),
                    (float) pos.getY() + side.getStepY(), pos.getZ() + 0.5 + side.getStepZ(), out);
            e.setDeltaMovement(0.3 * side.getStepX(), 0.3 * side.getStepY(), 0.3 * side.getStepZ());
            world.addFreshEntity(e);
        }
    }

    /** Выбросить добычу у блока; followItem — предметы летят к target (EntityFollowingItem, «Собиратель»). */
    public static void dropHarvestsAtPos(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, java.util.List<ItemStack> list,
                                         boolean followItem, int color, net.minecraft.world.entity.Entity target) {
        for (ItemStack item : list) {
            if (!world.isClientSide && world.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS) && !world.restoringBlockSnapshots) {
                float f = 0.5F;
                double d0 = world.random.nextFloat() * f + (1.0F - f) * 0.5;
                double d1 = world.random.nextFloat() * f + (1.0F - f) * 0.5;
                double d2 = world.random.nextFloat() * f + (1.0F - f) * 0.5;
                net.minecraft.world.entity.item.ItemEntity entityitem = followItem && target != null
                        ? new thaumcraft.common.entities.EntityFollowingItem(world, pos.getX() + d0, pos.getY() + d1, pos.getZ() + d2, item, target, color)
                        : new net.minecraft.world.entity.item.ItemEntity(world, pos.getX() + d0, pos.getY() + d1, pos.getZ() + d2, item);
                entityitem.setDefaultPickUpDelay();
                world.addFreshEntity(entityitem);
            }
        }
    }

    /**
     * removeStackFrom оригинала: забирает из инвентаря до stack.getCount() предметов, равных stack (тот же предмет и компоненты;
     * фильтр BASEORE по словарю руд 1.12 не переносится — см. DECISIONS 119). Возвращает забранное (или пустой стак).
     */
    public static ItemStack removeStackFrom(net.neoforged.neoforge.items.IItemHandler inventory, ItemStack stack, boolean simulate) {
        int amount = stack.getCount();
        int removed = 0;
        if (inventory != null) {
            for (int a = 0; a < inventory.getSlots(); a++) {
                ItemStack in = inventory.getStackInSlot(a);
                if (!in.isEmpty() && ItemStack.isSameItemSameComponents(stack, in)) {
                    int n = Math.min(amount - removed, in.getCount());
                    ItemStack es = inventory.extractItem(a, n, simulate);
                    if (!es.isEmpty()) {
                        removed += es.getCount();
                    }
                }
                if (removed >= amount) {
                    break;
                }
            }
        }
        return removed == 0 ? ItemStack.EMPTY : stack.copyWithCount(removed);
    }

    // --- фильтры печатей (InvFilter оригинала) ---

    /**
     * Условия сравнения предметов для печатей: igDmg — без учёта повреждения (metadata 1.12), igNBT — без учёта остальных
     * данных предмета, useOre — «словарь руд»: в 1.21 — общий тег c: (DECISIONS), useMod — любые предметы того же мода.
     */
    public static class InvFilter {
        boolean igDmg;
        boolean igNBT;
        boolean useOre;
        boolean useMod;
        boolean relaxedNBT = false;
        public static final InvFilter STRICT = new InvFilter(false, false, false, false);
        public static final InvFilter BASEORE = new InvFilter(false, false, true, false);

        public InvFilter(boolean ignoreDamage, boolean ignoreNBT, boolean useOre, boolean useMod) {
            this.igDmg = ignoreDamage;
            this.igNBT = ignoreNBT;
            this.useOre = useOre;
            this.useMod = useMod;
        }

        public InvFilter setRelaxedNBT() {
            this.relaxedNBT = true;
            return this;
        }
    }

    /** Общий тег c: у двух предметов (замена словаря руд 1.12). */
    private static boolean shareCommonTag(ItemStack a, ItemStack b) {
        return a.getTags().anyMatch(t -> t.location().getNamespace().equals("c") && b.is(t));
    }

    /** Данные предмета, кроме повреждения, совпадают (relaxed — у второго могут быть лишние данные). */
    private static boolean componentsEqualIgnoringDamage(ItemStack a, ItemStack b, boolean relaxed) {
        net.minecraft.core.component.DataComponentPatch pa = a.getComponentsPatch().forget(c -> c == DataComponents.DAMAGE);
        net.minecraft.core.component.DataComponentPatch pb = b.getComponentsPatch().forget(c -> c == DataComponents.DAMAGE);
        if (!relaxed) {
            return pa.equals(pb);
        }
        for (Map.Entry<DataComponentType<?>, Optional<?>> e : pa.entrySet()) {
            Optional<?> other = pb.get(e.getKey());
            if (other == null || !other.equals(e.getValue())) {
                return false;
            }
        }
        return true;
    }

    public static boolean areItemStacksEqual(ItemStack stack0, ItemStack stack1, InvFilter filter) {
        if (stack0 == null || stack1 == null) {
            return stack0 == stack1;
        }
        if (stack0.isEmpty() || stack1.isEmpty()) {
            return stack0.isEmpty() && stack1.isEmpty();
        }
        if (filter.useMod) {
            return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack0.getItem()).getNamespace()
                    .equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack1.getItem()).getNamespace());
        }
        if (filter.useOre && shareCommonTag(stack0, stack1)) {
            return true;
        }
        if (stack0.getItem() != stack1.getItem()) {
            return false;
        }
        if (!filter.igDmg && stack0.getDamageValue() != stack1.getDamageValue()) {
            return false;
        }
        return filter.igNBT || componentsEqualIgnoringDamage(stack0, stack1, filter.relaxedNBT);
    }

    public static boolean areItemStacksEqualStrict(ItemStack stack0, ItemStack stack1) {
        return areItemStacksEqual(stack0, stack1, InvFilter.STRICT);
    }

    public static boolean hasRoomFor(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side, ItemStack stack) {
        ItemStack testStack = insertStackAt(world, pos, side, stack.copy(), true);
        return stack.getCount() == 0 || testStack.getCount() != stack.getCount();
    }

    public static ItemStack removeStackFrom(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side, ItemStack stack,
                                           InvFilter filter, boolean simulate) {
        return removeStackFrom(thaumcraft.api.ThaumcraftApiHelper.getItemHandlerAt(world, pos, side), stack, filter, simulate);
    }

    public static ItemStack removeStackFrom(net.neoforged.neoforge.items.IItemHandler inventory, ItemStack stack, InvFilter filter, boolean simulate) {
        int amount = stack.getCount();
        int removed = 0;
        if (inventory != null) {
            for (int a = 0; a < inventory.getSlots(); a++) {
                if (areItemStacksEqual(stack, inventory.getStackInSlot(a), filter)) {
                    int s = Math.min(amount - removed, inventory.getStackInSlot(a).getCount());
                    ItemStack es = inventory.extractItem(a, s, simulate);
                    if (!es.isEmpty()) {
                        removed += es.getCount();
                    }
                }
                if (removed >= amount) {
                    break;
                }
            }
        }
        if (removed == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack s = stack.copy();
        s.setCount(removed);
        return s;
    }

    /** Сколько таких предметов лежит на земле рядом (в оригинале считались только пустые стаки — всегда 0, DEVIATIONS). */
    public static int countStackInWorld(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, ItemStack stack, double range, InvFilter filter) {
        int count = 0;
        net.minecraft.world.phys.Vec3 c = net.minecraft.world.phys.Vec3.atCenterOf(pos);
        for (net.minecraft.world.entity.item.ItemEntity ei : world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(pos).inflate(range))) {
            if (ei.distanceToSqr(c) <= range * range && !ei.getItem().isEmpty() && areItemStacksEqual(stack, ei.getItem(), filter)) {
                count += ei.getItem().getCount();
            }
        }
        return count;
    }

    public static int countStackIn(net.minecraft.world.level.Level world, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction side, ItemStack stack,
                                   InvFilter filter) {
        return countStackIn(thaumcraft.api.ThaumcraftApiHelper.getItemHandlerAt(world, pos, side), stack, filter);
    }

    public static int countStackIn(net.neoforged.neoforge.items.IItemHandler inventory, ItemStack stack, InvFilter filter) {
        int count = 0;
        if (inventory != null) {
            for (int a = 0; a < inventory.getSlots(); a++) {
                if (areItemStacksEqual(stack, inventory.getStackInSlot(a), filter)) {
                    count += inventory.getStackInSlot(a).getCount();
                }
            }
        }
        return count;
    }

    public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, boolean blacklist, net.neoforged.neoforge.items.IItemHandler inv,
                                                     net.minecraft.core.Direction face, InvFilter filter) {
        return findFirstMatchFromFilter(filterStacks, blacklist, inv, face, filter, false);
    }

    public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, boolean blacklist, net.neoforged.neoforge.items.IItemHandler inv,
                                                     net.minecraft.core.Direction face, InvFilter filter, boolean leaveOne) {
        outer:
        for (int a = 0; a < inv.getSlots(); a++) {
            ItemStack is = inv.getStackInSlot(a);
            if (!is.isEmpty() && (!leaveOne || countStackIn(inv, is, filter) >= 2)) {
                boolean allow = false;
                boolean allEmpty = true;
                for (ItemStack fs : filterStacks) {
                    if (!fs.isEmpty()) {
                        allEmpty = false;
                        boolean r = areItemStacksEqual(fs.copy(), is.copy(), filter);
                        if (blacklist) {
                            if (r) {
                                continue outer;
                            }
                            allow = true;
                        } else if (r) {
                            return is;
                        }
                    }
                }
                if (blacklist && (allow || allEmpty)) {
                    return is;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean matchesFilters(NonNullList<ItemStack> filterStacks, boolean blacklist, ItemStack is, InvFilter filter) {
        if (is == null || is.isEmpty()) {
            return false;
        }
        boolean allow = false;
        boolean allEmpty = true;
        for (ItemStack fs : filterStacks) {
            if (!fs.isEmpty()) {
                allEmpty = false;
                boolean r = areItemStacksEqual(fs.copy(), is.copy(), filter);
                if (blacklist) {
                    if (r) {
                        return false;
                    }
                    allow = true;
                } else if (r) {
                    return true;
                }
            }
        }
        return blacklist && (allow || allEmpty);
    }

    public static ItemStack findFirstMatchFromFilter(NonNullList<ItemStack> filterStacks, NonNullList<Integer> filterStacksSizes, boolean blacklist,
                                                     NonNullList<ItemStack> itemStacks, InvFilter filter) {
        return findFirstMatchFromFilterTuple(filterStacks, filterStacksSizes, blacklist, itemStacks, filter).getA();
    }

    /** Первый подходящий под фильтр предмет и ограничение количества его ячейки фильтра (0 — без ограничения). */
    public static net.minecraft.util.Tuple<ItemStack, Integer> findFirstMatchFromFilterTuple(NonNullList<ItemStack> filterStacks,
                                                                                         NonNullList<Integer> filterStacksSizes, boolean blacklist,
                                                                                         NonNullList<ItemStack> stacks, InvFilter filter) {
        outer:
        for (ItemStack is : stacks) {
            if (is != null && !is.isEmpty()) {
                boolean allow = false;
                boolean allEmpty = true;
                for (int idx = 0; idx < filterStacks.size(); idx++) {
                    ItemStack fs = filterStacks.get(idx);
                    if (!fs.isEmpty()) {
                        allEmpty = false;
                        boolean r = areItemStacksEqual(fs.copy(), is.copy(), filter);
                        if (blacklist) {
                            if (r) {
                                continue outer;
                            }
                            allow = true;
                        } else if (r) {
                            return new net.minecraft.util.Tuple<>(is, filterStacksSizes.get(idx));
                        }
                    }
                }
                if (blacklist && (allow || allEmpty)) {
                    return new net.minecraft.util.Tuple<>(is, 0);
                }
            }
        }
        return new net.minecraft.util.Tuple<>(ItemStack.EMPTY, 0);
    }
}
