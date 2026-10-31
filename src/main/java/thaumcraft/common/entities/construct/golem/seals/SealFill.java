package thaumcraft.common.entities.construct.golem.seals;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Печать хранения (порт SealFill): голем с подходящим под фильтр предметом кладёт его в инвентарь блока (без инвентаря —
 * бросает перед гранью). В белом списке ячейки фильтра задают, сколько держать в контейнере.
 */
public class SealFill extends SealFiltered {
    int delay = new Random(System.nanoTime()).nextInt(50);
    int watchedTask = Integer.MIN_VALUE;
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"),
            new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"),
            new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
            new ISealConfigToggles.SealToggle(false, "pexist", "golem.prop.exist")};
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_fill");

    @Override
    public String getKey() {
        return "thaumcraft:fill";
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay++ % 20 == 0) {
            Task oldTask = TaskHandler.getTask(world.dimension(), this.watchedTask);
            if (oldTask == null || oldTask.isReserved() || oldTask.isSuspended() || oldTask.isCompleted()) {
                Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
                task.setPriority(seal.getPriority());
                TaskHandler.addTask(world.dimension(), task);
                this.watchedTask = task.getId();
            }
        }
    }

    @Override
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
        ISealEntity se = SealHandler.getSealEntity(world.dimension(), task.getSealPos());
        if (se != null && !se.isStoppedByRedstone(world)) {
            Task newTask = new Task(task.getSealPos(), task.getSealPos().pos);
            newTask.setPriority(se.getPriority());
            TaskHandler.addTask(world.dimension(), newTask);
            this.watchedTask = newTask.getId();
        }
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        InventoryUtils.InvFilter filter = invFilter(this.props);
        Tuple<ItemStack, Integer> tuple = InventoryUtils.findFirstMatchFromFilterTuple(this.getInv(), this.getSizes(), this.isBlacklist(), golem.getCarrying(),
                filter);
        if (!tuple.getA().isEmpty()) {
            BlockPos pos = task.getSealPos().pos;
            Direction face = task.getSealPos().face;
            IItemHandler inv = ThaumcraftApiHelper.getItemHandlerAt(world, pos, face);
            int limit = tuple.getA().getCount();
            if (this.hasStacksizeLimiters() && tuple.getB() != null && tuple.getB() > 0) {
                int c = inv == null ? InventoryUtils.countStackInWorld(golem.getGolemWorld(), pos, tuple.getA(), 1.5, filter)
                        : InventoryUtils.countStackIn(inv, tuple.getA(), filter);
                limit = c < tuple.getB() ? tuple.getB() - c : 0;
            }
            if (limit > 0) {
                ItemStack t = tuple.getA().copy();
                t.setCount(limit);
                ItemStack s = golem.dropItem(t);
                if (inv == null) {
                    ItemEntity ie = new ItemEntity(world, pos.getX() + 0.5 + face.getStepX(), pos.getY() + 0.5 + face.getStepY(), pos.getZ() + 0.5 + face.getStepZ(),
                            s);
                    ie.setDeltaMovement(ie.getDeltaMovement().multiply(0.2, 0.5, 0.2));
                    world.addFreshEntity(ie);
                } else {
                    golem.holdItem(ItemHandlerHelper.insertItemStacked(inv, s, false));
                }
                golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F, (world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F);
                golem.addRankXp(1);
                golem.swingArm();
            }
        }
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        InventoryUtils.InvFilter filter = invFilter(this.props);
        Tuple<ItemStack, Integer> tuple = InventoryUtils.findFirstMatchFromFilterTuple(this.getInv(), this.getSizes(), this.isBlacklist(), golem.getCarrying(),
                filter);
        ItemStack first = tuple.getA();
        if (first.isEmpty()) {
            return false;
        }
        BlockPos pos = task.getSealPos().pos;
        IItemHandler inv = ThaumcraftApiHelper.getItemHandlerAt(golem.getGolemWorld(), pos, task.getSealPos().face);
        if (inv != null) {
            if (this.props[4].getValue() && InventoryUtils.countStackIn(inv, first, filter) <= 0) {
                return false;
            }
            if (InventoryUtils.hasRoomFor(golem.getGolemWorld(), pos, task.getSealPos().face, first)) {
                if (!this.hasStacksizeLimiters() || tuple.getB() == null || tuple.getB() <= 0) {
                    return true;
                }
                return InventoryUtils.countStackIn(inv, first, filter) < tuple.getB();
            }
            return false;
        }
        if (this.hasStacksizeLimiters() && tuple.getB() != null && tuple.getB() > 0) {
            return InventoryUtils.countStackInWorld(golem.getGolemWorld(), pos, first, 1.5, filter) < tuple.getB();
        }
        return true;
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
        return new int[] {1, 0, 4};
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
    public void onTaskSuspension(Level world, Task task) {
    }

    @Override
    public void onRemoval(Level world, BlockPos pos, Direction side) {
    }

    @Override
    public boolean hasStacksizeLimiters() {
        return !this.isBlacklist();
    }
}
