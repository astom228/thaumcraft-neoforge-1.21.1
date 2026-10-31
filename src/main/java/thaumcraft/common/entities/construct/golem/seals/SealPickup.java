package thaumcraft.common.entities.construct.golem.seals;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Печать сбора (порт SealPickup): раз в секунду ставит задачу на один лежащий в зоне предмет, подходящий под фильтр;
 * голем подбирает его. Карта «задача → предмет» общая для всех печатей сбора измерения (один предмет — одна задача);
 * только сервер.
 */
public class SealPickup extends SealFiltered implements ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(100);
    static final Map<ResourceKey<Level>, Map<Integer, Integer>> itemEntities = new ConcurrentHashMap<>();
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_pickup");
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"),
            new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"),
            new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod")};

    @Override
    public String getKey() {
        return "thaumcraft:pickup";
    }

    private static Map<Integer, Integer> map(Level world) {
        return itemEntities.computeIfAbsent(world.dimension(), k -> new ConcurrentHashMap<>());
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay++ % 20 != 0) {
            return;
        }
        Map<Integer, Integer> m = map(world);
        List<ItemEntity> list = world.getEntitiesOfClass(ItemEntity.class, GolemHelper.getBoundsForArea(seal));
        for (ItemEntity ent : list) {
            if (ent.onGround() && !ent.hasPickUpDelay() && !ent.getItem().isEmpty() && !m.containsValue(ent.getId())) {
                ItemStack stack = InventoryUtils.findFirstMatchFromFilter(this.filter, this.filterSize, this.isBlacklist(), NonNullList.withSize(1, ent.getItem()),
                        invFilter(this.props));
                if (!stack.isEmpty()) {
                    Task task = new Task(seal.getSealPos(), ent);
                    task.setPriority(seal.getPriority());
                    m.put(task.getId(), ent.getId());
                    TaskHandler.addTask(world.dimension(), task);
                    break;
                }
            }
        }
        if (this.delay % 100 != 0) {
            Iterator<Integer> it = m.values().iterator();
            while (it.hasNext()) {
                Entity e = world.getEntity(it.next());
                if (e == null || !e.isAlive()) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        ItemEntity ei = this.getItemEntity(world, task);
        if (ei != null && !ei.getItem().isEmpty()) {
            ItemStack stack = InventoryUtils.findFirstMatchFromFilter(this.filter, this.filterSize, this.isBlacklist(), NonNullList.withSize(1, ei.getItem()),
                    invFilter(this.props));
            if (!stack.isEmpty()) {
                ItemStack is = golem.holdItem(ei.getItem());
                if (is != null && !is.isEmpty()) {
                    ei.setItem(is);
                } else {
                    ei.discard();
                }
                golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F, ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                golem.swingArm();
            }
        }
        task.setSuspended(true);
        map(world).remove(task.getId());
        return true;
    }

    protected ItemEntity getItemEntity(Level world, Task task) {
        Integer ei = map(world).get(task.getId());
        if (ei != null && world.getEntity(ei) instanceof ItemEntity item) {
            return item;
        }
        return null;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        ItemEntity ei = this.getItemEntity(golem.getGolemWorld(), task);
        if (ei == null) {
            return false;
        }
        if (!ei.isAlive()) {
            task.setSuspended(true);
            return false;
        }
        return golem.canCarry(ei.getItem(), true);
    }

    @Override
    public boolean canPlaceAt(Level world, BlockPos pos, Direction side) {
        return !world.isEmptyBlock(pos);
    }

    @Override
    public ResourceLocation getSealIcon() {
        return this.icon;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] {2, 1, 0, 4};
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return null;
    }

    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return new EnumGolemTrait[] {EnumGolemTrait.CLUMSY};
    }

    @Override
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
    }

    @Override
    public void onRemoval(Level world, BlockPos pos, Direction side) {
    }
}
