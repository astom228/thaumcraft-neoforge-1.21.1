package thaumcraft.common.entities.construct.golem.ai;

import java.util.Arrays;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;
import thaumcraft.common.entities.construct.golem.seals.SealHandler;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;

/**
 * Поиск и выполнение задачи печати (порт AIGoto): раз в 5 тиков ищет задачу, идёт к ней; застряв — бредёт в сторону
 * цели; дойдя — выполняет. Невыполненная зарезервированная задача — эмоция «не получилось».
 */
public abstract class AIGoto extends Goal {
    protected EntityThaumcraftGolem golem;
    protected int taskCounter = -1;
    protected byte type = 0;
    protected int cooldown;
    protected double minDist = 4.0;
    private BlockPos prevRamble;
    protected BlockPos targetBlock;
    int pause = 0;

    public AIGoto(EntityThaumcraftGolem g, byte type) {
        this.golem = g;
        this.type = type;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        this.cooldown = 5;
        if (this.golem.getTask() != null && !this.golem.getTask().isSuspended()) {
            return false;
        }
        this.targetBlock = null;
        boolean start = this.findDestination();
        if (start && this.golem.getTask() != null && this.golem.getTask().getSealPos() != null) {
            ISealEntity se = SealHandler.getSealEntity(this.golem.level().dimension(), this.golem.getTask().getSealPos());
            if (se != null) {
                se.getSeal().onTaskStarted(this.golem.level(), this.golem, this.golem.getTask());
            }
        }
        return start;
    }

    @Override
    public void start() {
        this.moveTo();
        this.taskCounter = 0;
    }

    protected abstract void moveTo();

    @Override
    public boolean canContinueToUse() {
        return this.taskCounter >= 0 && this.taskCounter <= 1000 && this.golem.getTask() != null && !this.golem.getTask().isSuspended()
                && this.isValidDestination(this.golem.level(), this.golem.getTask().getPos());
    }

    @Override
    public void tick() {
        Task task = this.golem.getTask();
        if (task == null || this.pause-- > 0) {
            return;
        }
        double dist = task.getType() == 0
                ? this.golem.distanceToSqr(Vec3.atCenterOf(this.targetBlock == null ? task.getPos() : this.targetBlock))
                : this.golem.distanceToSqr(task.getEntity());
        if (dist > this.minDist) {
            task.setCompletion(false);
            this.taskCounter++;
            if (this.taskCounter % 40 == 0) {
                if (this.prevRamble != null && this.prevRamble.equals(this.golem.blockPosition())) {
                    Vec3 vec3 = DefaultRandomPos.getPosTowards(this.golem, 6, 4, Vec3.atLowerCornerOf(task.getPos()), Math.PI / 2);
                    if (vec3 != null) {
                        this.golem.getNavigation().moveTo(vec3.x + 0.5, vec3.y + 0.5, vec3.z + 0.5, this.golem.getGolemMoveSpeed());
                    }
                } else {
                    this.moveTo();
                }
                this.prevRamble = this.golem.blockPosition();
            }
        } else {
            TaskHandler.completeTask(task, this.golem);
            if (this.golem.getTask() != null && this.golem.getTask().isCompleted()) {
                if (this.taskCounter >= 0) {
                    this.taskCounter = 0;
                }
                this.pause = 0;
            } else {
                this.pause = 10;
                this.taskCounter++;
            }
            this.taskCounter--;
        }
    }

    @Override
    public void stop() {
        Task task = this.golem.getTask();
        if (task != null) {
            if (!task.isCompleted() && task.isReserved() && ModConfig.CONFIG_GRAPHICS.showGolemEmotes.get()) {
                this.golem.level().broadcastEntityEvent(this.golem, (byte) 6);
            }
            if (task.isCompleted() && !task.isSuspended()) {
                task.setSuspended(true);
            }
            task.setReserved(false);
        }
    }

    protected abstract boolean findDestination();

    protected boolean isValidDestination(Level world, BlockPos pos) {
        return true;
    }

    /**
     * Подходит ли голем печати: замок (только големы владельца печати), обязательные и запрещённые черты. В оригинале
     * UUID сравнивался со строкой и замок не пускал никого — исправлено (DEVIATIONS).
     */
    protected boolean areGolemTagsValidForTask(Task ticket) {
        ISealEntity se = SealHandler.getSealEntity(this.golem.level().dimension(), ticket.getSealPos());
        if (se == null || se.getSeal() == null) {
            return true;
        }
        if (se.isLocked() && (this.golem.getOwnerId() == null || !this.golem.getOwnerId().toString().equals(se.getOwner()))) {
            return false;
        }
        if (se.getSeal().getRequiredTags() != null && !this.golem.getProperties().getTraits().containsAll(Arrays.asList(se.getSeal().getRequiredTags()))) {
            return false;
        }
        if (se.getSeal().getForbiddenTags() != null) {
            for (EnumGolemTrait tag : se.getSeal().getForbiddenTags()) {
                if (this.golem.getProperties().getTraits().contains(tag)) {
                    return false;
                }
            }
        }
        return true;
    }
}
