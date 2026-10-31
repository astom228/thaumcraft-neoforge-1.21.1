package thaumcraft.common.entities.construct.golem.seals;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

/**
 * Печать охраны (порт SealGuard): раз в секунду — задачи напасть на подходящих существ в зоне (монстры; у улучшенной —
 * ещё животные и игроки, если разрешено PvP). Союзников голема (команда хозяина) не трогает; игроков в творческом
 * режиме и наблюдателей — тоже (в оригинале голем мог гнаться за ними без толку).
 */
public class SealGuard implements ISeal, ISealGui, ISealConfigArea {
    int delay = new Random(System.nanoTime()).nextInt(22);
    protected ISealConfigToggles.SealToggle[] props = new ISealConfigToggles.SealToggle[] {
            new ISealConfigToggles.SealToggle(true, "pmob", "golem.prop.mob"),
            new ISealConfigToggles.SealToggle(false, "panimal", "golem.prop.animal"),
            new ISealConfigToggles.SealToggle(false, "pplayer", "golem.prop.player")};
    ResourceLocation icon = ResourceLocation.fromNamespaceAndPath("thaumcraft", "items/seals/seal_guard");

    @Override
    public String getKey() {
        return "thaumcraft:guard";
    }

    @Override
    public void tickSeal(Level world, ISealEntity seal) {
        if (this.delay++ % 20 == 0) {
            for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, GolemHelper.getBoundsForArea(seal))) {
                if (this.isValidTarget(world, target)) {
                    Task task = new Task(seal.getSealPos(), target);
                    task.setPriority(seal.getPriority());
                    task.setLifespan((short) 10);
                    TaskHandler.addTask(world.dimension(), task);
                }
            }
        }
    }

    private boolean isValidTarget(Level world, LivingEntity target) {
        boolean valid = false;
        if (this.props[0].getValue() && target instanceof Enemy) {
            valid = true;
        }
        if (this.props[1].getValue() && target instanceof Animal) {
            valid = true;
        }
        if (this.props[2].getValue() && world.getServer() != null && world.getServer().isPvpAllowed() && target instanceof Player p && !p.isCreative()
                && !p.isSpectator()) {
            valid = true;
        }
        return valid;
    }

    @Override
    public void onTaskStarted(Level world, IGolemAPI golem, Task task) {
        if (task.getEntity() instanceof LivingEntity le && this.isValidTarget(world, le)) {
            ((Mob) golem.getGolemEntity()).setTarget(le);
            golem.addRankXp(1);
        }
        task.setSuspended(true);
    }

    @Override
    public boolean onTaskCompletion(Level world, IGolemAPI golem, Task task) {
        task.setSuspended(true);
        return true;
    }

    @Override
    public boolean canGolemPerformTask(IGolemAPI golem, Task task) {
        return task.getEntity() != null && !golem.getGolemEntity().isAlliedTo(task.getEntity());
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
        return new EnumGolemTrait[] {EnumGolemTrait.FIGHTER};
    }

    @Override
    public EnumGolemTrait[] getForbiddenTags() {
        return null;
    }

    @Override
    public void onTaskSuspension(Level world, Task task) {
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, HolderLookup.Provider registries) {
    }

    @Override
    public void onRemoval(Level world, BlockPos pos, Direction side) {
    }

    @Override
    public Object returnContainer(Level world, Player player, BlockPos pos, Direction side, ISealEntity seal) {
        return null;
    }
}
