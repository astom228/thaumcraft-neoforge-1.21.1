package thaumcraft.common.entities.construct.golem.seals;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.utils.BlockUtils;
import thaumcraft.common.lib.utils.Utils;

/**
 * Печать лесоруба (порт SealLumber): бревно в зоне — задача; голем рубит дерево с дальнего бревна по одному за
 * подход, пока ствол не кончится (разрушитель; умный).
 */
public class SealLumber implements ISeal, ISealGui, ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(33);
    final Map<Integer, Long> cache = new HashMap<>();
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_lumber");

    @Override
    public String getKey() {
        return "thaumcraft:lumber";
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay % 100 == 0) {
            this.cache.keySet().removeIf(id -> TaskHandler.getTask(world.dimension(), id) == null);
        }
        this.delay++;
        BlockPos p = GolemHelper.getPosInArea(seal, this.delay);
        if (world.isLoaded(p) && !this.cache.containsValue(p.asLong()) && Utils.isWoodLog(world, p)) {
            Task task = new Task(seal.getSealPos(), p);
            task.setPriority(seal.getPriority());
            TaskHandler.addTask(world.dimension(), task);
            this.cache.put(task.getId(), p.asLong());
        }
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        if (this.cache.containsKey(task.getId()) && Utils.isWoodLog(world, task.getPos())) {
            FakePlayer fp = GolemFakePlayer.get(world, golem);
            golem.swingArm();
            if (GolemFakePlayer.canBreak(world, fp, task.getPos()) && BlockUtils.breakFurthestBlock(world, task.getPos(), world.getBlockState(task.getPos()), fp)) {
                task.setLifespan((short) Math.max(task.getLifespan(), 10L));
                golem.addRankXp(1);
                return false;
            }
            this.cache.remove(task.getId());
        }
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        if (this.cache.containsKey(task.getId()) && Utils.isWoodLog(golem.getGolemWorld(), task.getPos())) {
            return true;
        }
        task.setSuspended(true);
        return false;
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
        this.cache.remove(task.getId());
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
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
        return new int[] {2, 0, 4};
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] {EnumGolemTrait.BREAKER, EnumGolemTrait.SMART};
    }

    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
    }

    @Override
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
    }
}
