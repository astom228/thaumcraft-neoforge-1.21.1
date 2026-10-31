package thaumcraft.common.entities.construct.golem.ai;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;

/** Задача-сущность (порт AIGotoEntity): подойти вплотную к сущности задачи (дистанция зависит от её ширины). */
public class AIGotoEntity extends AIGoto {
    public AIGotoEntity(EntityThaumcraftGolem g) {
        super(g, (byte) 1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.golem.getTask() != null && this.golem.getTask().getEntity() != null) {
            this.golem.getLookControl().setLookAt(this.golem.getTask().getEntity(), 10.0F, this.golem.getMaxHeadXRot());
        }
    }

    @Override
    protected void moveTo() {
        this.golem.getNavigation().moveTo(this.golem.getTask().getEntity(), this.golem.getGolemMoveSpeed());
    }

    @Override
    protected boolean findDestination() {
        for (Task ticket : TaskHandler.getEntityTasksSorted(this.golem.level().dimension(), this.golem.getUUID(), this.golem)) {
            if (this.areGolemTagsValidForTask(ticket) && ticket.canGolemPerformTask(this.golem)
                    && this.golem.isWithinRestriction(ticket.getEntity().blockPosition())
                    && this.isValidDestination(this.golem.level(), ticket.getEntity().blockPosition()) && this.canEasilyReach(ticket.getEntity())) {
                this.golem.setTask(ticket);
                this.golem.getTask().setReserved(true);
                float hw = this.golem.getTask().getEntity().getBbWidth() / 2.0F;
                this.minDist = 3.5 + hw * hw;
                if (ModConfig.CONFIG_GRAPHICS.showGolemEmotes.get()) {
                    this.golem.level().broadcastEntityEvent(this.golem, (byte) 5);
                }
                return true;
            }
        }
        return false;
    }

    private boolean canEasilyReach(Entity e) {
        if (this.golem.distanceToSqr(e) < this.minDist) {
            return true;
        }
        Path path = this.golem.getNavigation().createPath(e, 0);
        if (path == null) {
            return false;
        }
        Node node = path.getEndNode();
        if (node == null) {
            return false;
        }
        int i = node.x - (int) Math.floor(e.getX());
        int j = node.z - (int) Math.floor(e.getZ());
        return i * i + j * j < this.minDist;
    }
}
