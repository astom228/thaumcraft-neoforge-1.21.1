package thaumcraft.common.entities.construct.golem.seals;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Печать опустошения (порт SealEmpty): раз в секунду находит в инвентаре блока подходящий предмет и ставит задачу
 * забрать его (срок 5 с); «оставлять 1» не выбирает последний предмет.
 */
public class SealEmpty extends SealFiltered {
    int delay = new Random(System.nanoTime()).nextInt(30);
    int filterInc = 0;
    final Map<Integer, ItemStack> cache = new HashMap<>();
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_empty");
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"),
            new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"),
            new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
            new ISealConfigToggles.SealToggle(false, "pcycle", "golem.prop.cycle"),
            new ISealConfigToggles.SealToggle(false, "pleave", "golem.prop.leave")};

    @Override
    public String getKey() {
        return "thaumcraft:empty";
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay % 100 == 0) {
            this.cache.keySet().removeIf(id -> TaskHandler.getTask(world.dimension(), id) == null);
        }
        if (this.delay++ % 20 == 0) {
            IItemHandler inv = ThaumcraftApiHelper.getItemHandlerAt(world, seal.getSealPos().pos, seal.getSealPos().face);
            if (inv == null) {
                return;
            }
            ItemStack stack = InventoryUtils.findFirstMatchFromFilter(this.getInv(this.filterInc), this.isBlacklist(), inv, seal.getSealPos().face,
                    invFilter(this.props), this.props[5].getValue());
            if (!stack.isEmpty()) {
                Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
                task.setPriority(seal.getPriority());
                task.setLifespan((short) 5);
                TaskHandler.addTask(world.dimension(), task);
                this.cache.put(task.getId(), stack.copy());
            }
        }
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        ItemStack stack = this.cache.get(task.getId());
        BlockPos pos = task.getSealPos().pos;
        Direction face = task.getSealPos().face;
        if (stack != null && !stack.isEmpty()) {
            int sa = InventoryUtils.countStackIn(world, pos, face, stack, invFilter(this.props));
            if (this.props[5].getValue() && sa <= stack.getCount()) {
                stack = stack.copy();
                stack.setCount(sa - 1);
            }
        }
        if (stack != null && !stack.isEmpty() && golem.canCarry(stack, true)) {
            ItemStack s = golem.holdItem(InventoryUtils.removeStackFrom(world, pos, face, stack.copy(), invFilter(this.props), false));
            if (!s.isEmpty()) {
                InventoryUtils.ejectStackAt(world, pos, face, s);
            }
            golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F, ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            golem.swingArm();
        }
        this.cache.remove(task.getId());
        this.filterInc++;
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        ItemStack stack = this.cache.get(task.getId());
        return stack != null && !stack.isEmpty() && golem.canCarry(stack, true);
    }

    @Override
    public boolean canPlaceAt(Level world, BlockPos pos, Direction side) {
        return ThaumcraftApiHelper.getItemHandlerAt(world, pos, side) != null;
    }

    public NonNullList<ItemStack> getInv(int c) {
        return super.getInv();
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
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
        this.cache.remove(task.getId());
    }

    @Override
    public void onRemoval(Level world, BlockPos pos, Direction side) {
    }
}
