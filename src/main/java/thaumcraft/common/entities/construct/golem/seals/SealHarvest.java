package thaumcraft.common.entities.construct.golem.seals;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.CropUtils;

/**
 * Печать урожая (порт SealHarvest): обходит зону по клетке раз в 5 тиков; спелый посев — задача собрать (сломать или,
 * для «щелчковых», нажать). С пересадкой — запоминает место и семя, голем с семенем сажает обратно (на земле/траве под
 * пашню — сначала вспахивает); «запрашивать снабжение» — просит семя у печатей снабжения.
 */
public class SealHarvest implements ISeal, ISealGui, ISealConfigArea, ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(33);
    int count = 0;
    final Map<Long, ReplantInfo> replantTasks = new HashMap<>();
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_harvest");
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {
            new ISealConfigToggles.SealToggle(true, "prep", "golem.prop.replant"),
            new ISealConfigToggles.SealToggle(false, "ppro", "golem.prop.provision")};

    @Override
    public String getKey() {
        return "thaumcraft:harvest";
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay % 100 == 0) {
            AABB area = GolemHelper.getBoundsForArea(seal);
            Iterator<Map.Entry<Long, ReplantInfo>> rt = this.replantTasks.entrySet().iterator();
            while (rt.hasNext()) {
                Map.Entry<Long, ReplantInfo> e = rt.next();
                if (!area.contains(Vec3.atCenterOf(BlockPos.of(e.getKey())))) {
                    // в оригинале по ошибке искали задачу по итератору вместо ключа — задача пересадки не приостанавливалась
                    Task tt = TaskHandler.getTask(world.dimension(), e.getValue().taskid);
                    if (tt != null) {
                        tt.setSuspended(true);
                    }
                    rt.remove();
                }
            }
        }
        if (this.delay++ % 5 == 0) {
            BlockPos p = GolemHelper.getPosInArea(seal, this.count++);
            if (!world.isLoaded(p)) {
                return;
            }
            if (CropUtils.isGrownCrop(world, p)) {
                Task task = new Task(seal.getSealPos(), p);
                task.setPriority(seal.getPriority());
                TaskHandler.addTask(world.dimension(), task);
            } else if (this.getToggles()[0].getValue() && this.replantTasks.containsKey(p.asLong()) && world.isEmptyBlock(p)) {
                ReplantInfo ri = this.replantTasks.get(p.asLong());
                if (TaskHandler.getTask(world.dimension(), ri.taskid) == null) {
                    Task tt = new Task(seal.getSealPos(), ri.pos);
                    tt.setPriority(seal.getPriority());
                    TaskHandler.addTask(world.dimension(), tt);
                    ri.taskid = tt.getId();
                }
            }
        }
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        BlockPos pos = task.getPos();
        if (CropUtils.isGrownCrop(world, pos)) {
            FakePlayer fp = GolemFakePlayer.get(world, golem);
            Direction face = Direction.getNearest(golem.getGolemEntity().getX() - pos.getX(), golem.getGolemEntity().getY() - pos.getY(),
                    golem.getGolemEntity().getZ() - pos.getZ());
            BlockState bs = world.getBlockState(pos);
            if (CropUtils.clickableCrops.contains(bs.getBlock())) {
                bs.useWithoutItem(world, fp, new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
                golem.addRankXp(1);
                golem.swingArm();
            } else {
                BlockUtils.harvestBlock(world, fp, pos, true, false, 0, false);
                golem.addRankXp(1);
                golem.swingArm();
                if (this.getToggles()[0].getValue() && world.isEmptyBlock(pos)) {
                    ItemStack seed = ThaumcraftApi.getSeed(bs.getBlock());
                    if (!seed.isEmpty()) {
                        BlockState bb = world.getBlockState(pos.below());
                        Direction rf = null;
                        if (seed.getItem() instanceof net.minecraft.world.item.BlockItem bi
                                && (bb.canSustainPlant(world, pos.below(), Direction.UP, bi.getBlock().defaultBlockState()) != TriState.FALSE
                                        || bb.getBlock() instanceof FarmBlock)) {
                            rf = Direction.DOWN;
                        } else if (bs.hasProperty(BlockStateProperties.FACING)) {
                            rf = bs.getValue(BlockStateProperties.FACING);
                        }
                        if (rf != null) {
                            Task tt = new Task(task.getSealPos(), pos);
                            tt.setPriority(task.getPriority());
                            tt.setLifespan((short) 300);
                            this.replantTasks.put(pos.asLong(), new ReplantInfo(pos, rf, tt.getId(), seed.copy(), bb.getBlock() instanceof FarmBlock));
                            TaskHandler.addTask(world.dimension(), tt);
                        }
                    }
                }
            }
        } else if (this.replantTasks.containsKey(pos.asLong()) && this.replantTasks.get(pos.asLong()).taskid == task.getId() && world.isEmptyBlock(pos)
                && golem.isCarrying(this.replantTasks.get(pos.asLong()).stack)) {
            FakePlayer fp = GolemFakePlayer.get(world, golem);
            BlockState bb = world.getBlockState(pos.below());
            ReplantInfo ri = this.replantTasks.get(pos.asLong());
            if ((bb.is(net.minecraft.world.level.block.Blocks.DIRT) || bb.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) && ri.farmland) {
                ItemStack hoe = new ItemStack(Items.DIAMOND_HOE);
                fp.setItemInHand(InteractionHand.MAIN_HAND, hoe);
                hoe.useOn(new UseOnContext(fp, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, 0.5, 0), Direction.UP,
                        pos.below(), false)));
            }
            ItemStack seed = ri.stack.copy();
            seed.setCount(1);
            fp.setItemInHand(InteractionHand.MAIN_HAND, seed);
            BlockPos on = pos.relative(ri.face);
            InteractionResult r = seed.useOn(new UseOnContext(fp, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(on), ri.face.getOpposite(), on, false)));
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (r.consumesAction()) {
                world.levelEvent(2001, pos, Block.getId(world.getBlockState(pos)));
                ItemStack one = ri.stack.copy();
                one.setCount(1);
                golem.dropItem(one);
                golem.addRankXp(1);
                golem.swingArm();
            }
        }
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        ReplantInfo ri = this.replantTasks.get(task.getPos().asLong());
        if (ri != null && ri.taskid == task.getId()) {
            boolean carry = golem.isCarrying(ri.stack);
            if (!carry && this.getToggles()[1].getValue()) {
                ISealEntity se = SealHandler.getSealEntity(golem.getGolemWorld().dimension(), task.getSealPos());
                if (se != null) {
                    GolemHelper.requestProvisioning(golem.getGolemWorld(), se, ri.stack);
                }
            }
            return carry;
        }
        return true;
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        ListTag list = nbt.getList("replant", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            long loc = t.getLong("taskloc");
            Direction face = Direction.from3DDataValue(t.getByte("taskface"));
            ItemStack stack = ItemStack.parseOptional(registries, t.getCompound("item"));
            this.replantTasks.put(loc, new ReplantInfo(BlockPos.of(loc), face, 0, stack, t.getBoolean("farmland")));
        }
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
        if (this.getToggles()[0].getValue()) {
            ListTag list = new ListTag();
            for (ReplantInfo info : this.replantTasks.values()) {
                if (info.stack.isEmpty()) {
                    continue;
                }
                CompoundTag t = new CompoundTag();
                t.putLong("taskloc", info.pos.asLong());
                t.putByte("taskface", (byte) info.face.get3DDataValue());
                t.putBoolean("farmland", info.farmland);
                t.put("item", info.stack.save(registries));
                list.add(t);
            }
            nbt.put("replant", list);
        }
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
    public void onRemoval(Level world, BlockPos pos, Direction side) {
    }

    @Override
    public Object returnContainer(Level world, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null;
    }

    @Override
    public int[] getGuiCategories() {
        return new int[] {2, 3, 0, 4};
    }

    @Override
    public SealToggle[] getToggles() {
        return this.props;
    }

    @Override
    public void setToggle(int indx, boolean value) {
        this.props[indx].setValue(value);
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
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
    }

    private static class ReplantInfo {
        Direction face;
        BlockPos pos;
        int taskid;
        ItemStack stack;
        boolean farmland;

        ReplantInfo(BlockPos pos, Direction face, int taskid, ItemStack stack, boolean farmland) {
            this.pos = pos;
            this.face = face;
            this.taskid = taskid;
            this.stack = stack;
            this.farmland = farmland;
        }
    }
}
