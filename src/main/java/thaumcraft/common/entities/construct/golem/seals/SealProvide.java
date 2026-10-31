package thaumcraft.common.entities.construct.golem.seals;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.ProvisionRequest;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Печать снабжения (порт SealProvide): берёт запросы снабжения в 64 блоках (от печатей, игроков из окна логистики,
 * печатей запаса), если в её контейнере есть нужный предмет под фильтр; голем забирает его и несёт заказчику.
 */
public class SealProvide extends SealFiltered implements ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(88);
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_provider");
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"),
            new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"),
            new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
            new ISealConfigToggles.SealToggle(false, "psing", "golem.prop.single"),
            new ISealConfigToggles.SealToggle(false, "pleave", "golem.prop.leave")};

    @Override
    public String getKey() {
        return "thaumcraft:provider";
    }

    @Override
    public int getFilterSize() {
        return 9;
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        List<ProvisionRequest> requests = GolemHelper.provisionRequests.get(world.dimension());
        if (this.delay % 100 == 0 && requests != null) {
            requests.removeIf(pr -> pr.isInvalid() || pr.getLinkedTask() == null || pr.getLinkedTask().isSuspended() || pr.getLinkedTask().isCompleted());
        }
        if (this.delay++ % 20 == 0 && requests != null) {
            IItemHandler inv = ThaumcraftApiHelper.getItemHandlerAt(world, seal.getSealPos().pos, seal.getSealPos().face);
            if (inv == null) {
                return;
            }
            BlockPos sp = seal.getSealPos().pos;
            Iterator<ProvisionRequest> it = requests.iterator();
            while (it.hasNext()) {
                ProvisionRequest pr = it.next();
                if (pr.isInvalid()) {
                    it.remove();
                } else if (pr.getLinkedTask() == null && (pr.getSeal() != null && pr.getSeal().getSealPos().pos.distSqr(sp) < 4096.0
                        || pr.getEntity() != null && sp.distToCenterSqr(pr.getEntity().getX(), pr.getEntity().getY(), pr.getEntity().getZ()) < 4096.0
                        || pr.getPos() != null && sp.distSqr(pr.getPos()) < 4096.0)) {
                    NonNullList<ItemStack> stacks = NonNullList.withSize(1, pr.getStack());
                    if (!InventoryUtils.findFirstMatchFromFilter(this.getInv(), this.getSizes(), this.blacklist, stacks, invFilter(this.props)).isEmpty()
                            && InventoryUtils.countStackIn(inv, pr.getStack(), InventoryUtils.InvFilter.STRICT) > (this.props[5].getValue() ? 1 : 0)) {
                        Task task = new Task(seal.getSealPos(), sp);
                        task.setPriority(pr.getSeal() != null ? pr.getSeal().getPriority() : 5);
                        task.setLifespan((short) (pr.getSeal() != null ? 10 : 31000));
                        TaskHandler.addTask(world.dimension(), task);
                        pr.setLinkedTask(task);
                        task.setLinkedProvision(pr);
                        break;
                    }
                }
            }
        }
    }

    public boolean matchesFilters(ItemStack stack) {
        return InventoryUtils.matchesFilters(this.getInv(), this.blacklist, stack, invFilter(this.props));
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        ProvisionRequest pr = task.getLinkedProvision();
        if (pr != null) {
            if (task.getData() == 0) {
                IItemHandler inv = ThaumcraftApiHelper.getItemHandlerAt(world, task.getSealPos().pos, task.getSealPos().face);
                if (inv != null) {
                    ItemStack stack = pr.getStack().copy();
                    if (this.props[4].getValue()) {
                        stack.setCount(1);
                    }
                    int sa;
                    if (!stack.isEmpty() && this.props[5].getValue() && (sa = InventoryUtils.countStackIn(inv, stack, InventoryUtils.InvFilter.STRICT)) <= stack.getCount()) {
                        stack.setCount(sa - 1);
                    }
                    if (!stack.isEmpty() && golem.canCarry(stack, true)) {
                        ItemStack s = golem.holdItem(InventoryUtils.removeStackFrom(inv, stack.copy(), InventoryUtils.InvFilter.STRICT, false));
                        if (s != null && !s.isEmpty()) {
                            InventoryUtils.ejectStackAt(world, task.getSealPos().pos, task.getSealPos().face, s);
                        }
                        golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F,
                                ((world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
                        golem.addRankXp(1);
                        golem.swingArm();
                        if (pr.getEntity() != null || pr.getPos() != null) {
                            Task task2 = pr.getEntity() != null ? new Task(task.getSealPos(), pr.getEntity()) : new Task(task.getSealPos(), pr.getPos());
                            task2.setPriority(task.getPriority());
                            task2.setData(pr.getEntity() != null ? 1 : 2);
                            task2.setLifespan((short) 31000);
                            TaskHandler.addTask(world.dimension(), task2);
                            pr.setLinkedTask(task2);
                            task2.setLinkedProvision(pr);
                        }
                    }
                }
            } else {
                ItemStack cs = pr.getStack();
                ItemStack s = golem.dropItem(cs);
                if (s.getCount() < cs.getCount()) {
                    ItemStack ps = cs.copy();
                    ps.setCount(cs.getCount() - s.getCount());
                    if (task.getData() == 1) {
                        GolemHelper.requestProvisioning(world, pr.getEntity(), ps);
                    } else {
                        GolemHelper.requestProvisioning(world, pr.getPos(), pr.getSide(), ps);
                    }
                }
                if (!s.isEmpty()) {
                    if (task.getData() == 1) {
                        Entity e = pr.getEntity();
                        world.addFreshEntity(new ItemEntity(world, e.getX(), e.getY() + e.getBbHeight() / 2.0, e.getZ(), s));
                    } else {
                        InventoryUtils.ejectStackAt(world, pr.getPos().relative(pr.getSide()), pr.getSide().getOpposite(), s);
                    }
                }
                golem.getGolemEntity().playSound(SoundEvents.ITEM_PICKUP, 0.125F, (world.random.nextFloat() - world.random.nextFloat()) * 0.7F + 1.0F);
                golem.swingArm();
                pr.setInvalid(true);
            }
        }
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        ProvisionRequest pr = task.getLinkedProvision();
        if (pr == null || !(golem.getGolemEntity() instanceof EntityThaumcraftGolem g)) {
            return false;
        }
        boolean b = pr.getSeal() != null && g.isWithinRestriction(pr.getSeal().getSealPos().pos)
                || pr.getEntity() != null && g.isWithinRestriction(pr.getEntity().blockPosition()) || pr.getPos() != null && g.isWithinRestriction(pr.getPos());
        if (!b || !areGolemTagsValidForTask(pr.getSeal(), golem) || pr.getStack().isEmpty()) {
            return false;
        }
        return task.getData() == 0 ? !golem.isCarrying(pr.getStack()) && golem.canCarry(pr.getStack(), true) : golem.isCarrying(pr.getStack());
    }

    /** Подходит ли голем печати-заказчику (замок — по владельцу: в оригинале UUID сравнивался со строкой). */
    private static boolean areGolemTagsValidForTask(ISealEntity se, IGolemAPI golem) {
        if (se == null) {
            return true;
        }
        if (se.isLocked() && (!(golem.getGolemEntity() instanceof EntityOwnedConstruct oc) || oc.getOwnerId() == null
                || !oc.getOwnerId().toString().equals(se.getOwner()))) {
            return false;
        }
        if (se.getSeal().getRequiredTags() != null && !golem.getProperties().getTraits().containsAll(Arrays.asList(se.getSeal().getRequiredTags()))) {
            return false;
        }
        if (se.getSeal().getForbiddenTags() != null) {
            for (EnumGolemTrait tag : se.getSeal().getForbiddenTags()) {
                if (golem.getProperties().getTraits().contains(tag)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
        if (task.getLinkedProvision() != null) {
            task.getLinkedProvision().setLinkedTask(null);
        }
        task.setLinkedProvision(null);
    }

    @Override
    public boolean canPlaceAt(Level world, BlockPos pos, Direction side) {
        return ThaumcraftApiHelper.getItemHandlerAt(world, pos, side) != null;
    }

    @Override
    public ResourceLocation getSealIcon() {
        return this.icon;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] {1, 3, 0, 4};
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
    public void onRemoval(Level world, BlockPos pos, Direction side) {
    }

    @Override
    public SealToggle[] getToggles() {
        return this.props;
    }

    @Override
    public void setToggle(int indx, boolean value) {
        this.props[indx].setValue(value);
    }
}
