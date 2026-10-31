package thaumcraft.common.entities.construct.golem.seals;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;
import thaumcraft.common.lib.utils.BlockUtils;

/**
 * Печать разрушения (порт SealBreaker): блоки зоны (по фильтру из одного образца) голем-разрушитель ломает за несколько
 * подходов по твёрдости, с трещинами. Переключатель «метаданные» в 1.21 не влияет (у блоков нет метаданных).
 */
public class SealBreaker extends SealFiltered implements ISealConfigArea, ISealConfigToggles {
    int delay = new Random(System.nanoTime()).nextInt(42);
    final Map<Integer, Long> cache = new HashMap<>();
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_breaker");
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {new ISealConfigToggles.SealToggle(true, "pmeta", "golem.prop.meta")};

    @Override
    public String getKey() {
        return "thaumcraft:breaker";
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay % 100 == 0) {
            this.cache.keySet().removeIf(id -> TaskHandler.getTask(world.dimension(), id) == null);
        }
        this.delay++;
        BlockPos p = GolemHelper.getPosInArea(seal, this.delay);
        if (world.isLoaded(p) && !this.cache.containsValue(p.asLong()) && this.isValidBlock(world, p)) {
            Task task = new Task(seal.getSealPos(), p);
            task.setPriority(seal.getPriority());
            task.setData((int) (world.getBlockState(p).getDestroySpeed(world, p) / 3.0F));
            TaskHandler.addTask(world.dimension(), task);
            this.cache.put(task.getId(), p.asLong());
        }
    }

    private boolean isValidBlock(Level world, BlockPos p) {
        BlockState bs = world.getBlockState(p);
        if (world.isEmptyBlock(p) || bs.getDestroySpeed(world, p) < 0.0F) {
            return false;
        }
        ItemStack ts = this.getFilterSlot(0);
        if (!ts.isEmpty()) {
            boolean match = new ItemStack(bs.getBlock().asItem()).is(ts.getItem());
            return this.isBlacklist() != match;
        }
        return true;
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        BlockPos pos = task.getPos();
        BlockState bs = world.getBlockState(pos);
        if (this.cache.containsKey(task.getId()) && this.isValidBlock(world, pos)) {
            FakePlayer fp = GolemFakePlayer.get(world, golem);
            golem.swingArm();
            if (task.getData() > 0) {
                float bh = bs.getDestroySpeed(world, pos) / 3.0F;
                SoundType st = bs.getSoundType(world, pos, fp);
                world.playSound(null, pos, st.getBreakSound(), SoundSource.BLOCKS, (st.getVolume() + 0.7F) / 8.0F, st.getPitch() * 0.5F);
                world.destroyBlockProgress(golem.getGolemEntity().getId(), pos, (int) (9.0F * (1.0F - task.getData() / bh)));
                task.setLifespan((short) Math.max(task.getLifespan(), 10L));
                task.setData(task.getData() - 1);
                return false;
            }
            world.destroyBlockProgress(golem.getGolemEntity().getId(), pos, -1);
            BlockUtils.harvestBlock(world, fp, pos, true, false, 0, false);
            golem.addRankXp(1);
            this.cache.remove(task.getId());
        }
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        if (this.cache.containsKey(task.getId()) && this.isValidBlock(golem.getGolemWorld(), task.getPos())) {
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
    public int[] getGuiCategories() {
        return new int[] {2, 1, 3, 0, 4};
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] {EnumGolemTrait.BREAKER};
    }

    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
    }

    @Override
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
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
