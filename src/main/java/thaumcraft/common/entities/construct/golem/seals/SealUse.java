package thaumcraft.common.entities.construct.golem.seals;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Печать использования (порт SealUse): голем «щёлкает» по блоку печати несомым предметом (по фильтру) — правой или левой
 * кнопкой, крадучись, пустой рукой, по воздуху; всё, что оказалось у заместителя, забирает или бросает. Умный ловкий.
 */
public class SealUse extends SealFiltered implements ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(49);
    int watchedTask = Integer.MIN_VALUE;
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_use");
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {
            new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta"),
            new ISealConfigToggles.SealToggle(true, "pnbt", "golem.prop.nbt"),
            new ISealConfigToggles.SealToggle(false, "pore", "golem.prop.ore"),
            new ISealConfigToggles.SealToggle(false, "pmod", "golem.prop.mod"),
            new ISealConfigToggles.SealToggle(false, "pleft", "golem.prop.left"),
            new ISealConfigToggles.SealToggle(false, "pempty", "golem.prop.empty"),
            new ISealConfigToggles.SealToggle(false, "pemptyhand", "golem.prop.emptyhand"),
            new ISealConfigToggles.SealToggle(false, "psneak", "golem.prop.sneak"),
            new ISealConfigToggles.SealToggle(false, "ppro", "golem.prop.provision.wl")};

    @Override
    public String getKey() {
        return "thaumcraft:use";
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay++ % 5 == 0) {
            Task oldTask = TaskHandler.getTask(world.dimension(), this.watchedTask);
            if (oldTask == null || oldTask.isSuspended() || oldTask.isCompleted()) {
                if (this.getToggles()[5].getValue() != world.isEmptyBlock(seal.getSealPos().pos)) {
                    return;
                }
                Task task = new Task(seal.getSealPos(), seal.getSealPos().pos);
                task.setPriority(seal.getPriority());
                TaskHandler.addTask(world.dimension(), task);
                this.watchedTask = task.getId();
            }
        }
    }

    @Override
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
    }

    /** Можно ли поставить блок: его форма в этой клетке ни с кем не пересекается. */
    public boolean mayPlace(Level world, Block blockIn, BlockPos pos, Direction side) {
        VoxelShape shape = blockIn.defaultBlockState().getCollisionShape(world, pos);
        return shape.isEmpty() || world.isUnobstructed(null, shape.move(pos.getX(), pos.getY(), pos.getZ()));
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        if (this.getToggles()[5].getValue() == world.isEmptyBlock(task.getPos())) {
            LivingEntity ge = golem.getGolemEntity();
            FakePlayer fp = GolemFakePlayer.get(world, golem);
            fp.getInventory().clearContent();
            fp.setYRot(ge.getYRot());
            fp.setXRot(ge.getXRot());
            ItemStack clickStack = golem.getCarrying().get(0);
            if (!this.filter.get(0).isEmpty()) {
                clickStack = InventoryUtils.findFirstMatchFromFilter(this.filter, this.filterSize, this.blacklist, golem.getCarrying(), invFilter(this.props));
            }
            if (!clickStack.isEmpty() || this.props[6].getValue()) {
                ItemStack ss = ItemStack.EMPTY;
                if (!clickStack.isEmpty()) {
                    ss = clickStack.copy();
                    golem.dropItem(clickStack.copy());
                }
                fp.setItemInHand(InteractionHand.MAIN_HAND, ss);
                // в оригинале крадучись включал переключатель «пустой рукой» (индекс 6 вместо 7)
                fp.setShiftKeyDown(this.props[7].getValue());
                Direction face = task.getSealPos().face;
                if (this.getToggles()[4].getValue()) {
                    try {
                        fp.gameMode.handleBlockBreakAction(task.getPos(), ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, face, world.getMaxBuildHeight(), 0);
                    } catch (Exception ignored) {
                    }
                } else {
                    if (fp.getMainHandItem().getItem() instanceof BlockItem bi && !this.mayPlace(world, bi.getBlock(), task.getPos(), face)) {
                        ge.setPos(ge.getX() + face.getStepX(), ge.getY() + face.getStepY(), ge.getZ() + face.getStepZ());
                    }
                    try {
                        fp.gameMode.useItemOn(fp, world, fp.getMainHandItem(), InteractionHand.MAIN_HAND,
                                new BlockHitResult(Vec3.atCenterOf(task.getPos()), face, task.getPos(), false));
                    } catch (Exception ignored) {
                    }
                }
                golem.addRankXp(1);
                this.dropSomeItems(fp, golem);
                fp.setShiftKeyDown(false);
                golem.swingArm();
            }
        }
        task.setSuspended(true);
        return true;
    }

    /** Всё из рук и инвентаря заместителя — голему (что влезет), остальное — на землю у голема. */
    private void dropSomeItems(FakePlayer fp, IGolemAPI golem) {
        dropList(fp.getInventory().items, golem);
        dropList(fp.getInventory().armor, golem);
        dropList(fp.getInventory().offhand, golem);
    }

    private static void dropList(NonNullList<ItemStack> list, IGolemAPI golem) {
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = list.get(i);
            if (s.isEmpty()) {
                continue;
            }
            if (golem.canCarry(s, true)) {
                s = golem.holdItem(s);
            }
            if (s != null && !s.isEmpty()) {
                LivingEntity e = golem.getGolemEntity();
                ItemEntity ie = new ItemEntity(golem.getGolemWorld(), e.getX(), e.getY() + e.getBbHeight() / 2.0, e.getZ(), s);
                golem.getGolemWorld().addFreshEntity(ie);
            }
            list.set(i, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        if (this.props[6].getValue()) {
            return true;
        }
        boolean found = !InventoryUtils.findFirstMatchFromFilter(this.filter, this.filterSize, this.blacklist, golem.getCarrying(), invFilter(this.props)).isEmpty();
        if (!found && this.getToggles()[8].getValue() && !this.blacklist && !this.getInv().get(0).isEmpty()) {
            ISealEntity se = SealHandler.getSealEntity(golem.getGolemWorld().dimension(), task.getSealPos());
            if (se != null) {
                GolemHelper.requestProvisioning(golem.getGolemWorld(), se, this.getInv().get(0).copy());
            }
        }
        return found;
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
    }

    @Override
    public boolean canPlaceAt(Level world, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    public ResourceLocation getSealIcon() {
        return this.icon;
    }

    @Override
    public void onRemoval(Level world, BlockPos pos, Direction side) {
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] {1, 3, 0, 4};
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] {EnumGolemTrait.DEFT, EnumGolemTrait.SMART};
    }

    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
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
