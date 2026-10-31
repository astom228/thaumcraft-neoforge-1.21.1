package thaumcraft.common.entities.construct.golem.seals;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.GolemHelper;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.seals.ISeal;
import thaumcraft.api.golems.seals.ISealConfigArea;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.seals.ISealGui;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;

/**
 * Печать забоя (порт SealButcher): раз в 10 с, если в зоне больше двух взрослых неприручённых животных одного вида, —
 * задача напасть на одно из них (бойцу; умному).
 */
public class SealButcher implements ISeal, ISealGui, ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(200);
    boolean wait = false;
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_butcher");

    @Override
    public String getKey() {
        return "thaumcraft:butcher";
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay++ % 200 == 0 && !this.wait) {
            var area = GolemHelper.getBoundsForArea(seal);
            for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, area)) {
                if (this.isValidTarget(target)) {
                    List<? extends LivingEntity> same = world.getEntitiesOfClass(target.getClass(), area);
                    int count = 0;
                    for (LivingEntity o : same) {
                        if (count >= 3) {
                            break;
                        }
                        if (this.isValidTarget(o)) {
                            count++;
                        }
                    }
                    if (count > 2) {
                        Task task = new Task(seal.getSealPos(), target);
                        task.setPriority(seal.getPriority());
                        task.setLifespan((short) 10);
                        TaskHandler.addTask(world.dimension(), task);
                        this.wait = true;
                        break;
                    }
                }
            }
        }
    }

    private boolean isValidTarget(LivingEntity target) {
        return target instanceof Animal a && !(target instanceof Enemy) && !(target instanceof TamableAnimal t && t.isTame())
                && !(target instanceof AbstractGolem) && !a.isBaby();
    }

    @Override
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
        if (task.getEntity() instanceof LivingEntity le && this.isValidTarget(le)) {
            ((Mob) golem.getGolemEntity()).setTarget(le);
            golem.addRankXp(1);
        }
        task.setSuspended(true);
        this.wait = false;
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        task.setSuspended(true);
        this.wait = false;
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
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
        return new int[] {2, 0, 4};
    }

    @Override
    public EnumGolemTrait[] getRequiredTags() {
        return new EnumGolemTrait[] {EnumGolemTrait.FIGHTER, EnumGolemTrait.SMART};
    }

    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
        this.wait = false;
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
    }

    @Override
    public void onRemoval(Level world, BlockPos pos, Direction side) {
        this.wait = false;
    }

    @Override
    public Object returnContainer(Level world, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null;
    }
}
