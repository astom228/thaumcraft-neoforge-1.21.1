package thaumcraft.common.entities.construct.golem.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.tasks.Task;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;
import thaumcraft.common.entities.construct.golem.tasks.TaskHandler;

/** Задача-блок (порт AIGotoBlock): ближайшая доступная задача, подход к свободной соседней клетке. */
public class AIGotoBlock extends AIGoto {
    public AIGotoBlock(EntityThaumcraftGolem g) {
        super(g, (byte) 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.golem.getTask() != null) {
            BlockPos p = this.golem.getTask().getPos();
            this.golem.getLookControl().setLookAt(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 10.0F, this.golem.getMaxHeadXRot());
        }
    }

    @Override
    protected void moveTo() {
        BlockPos p = this.targetBlock != null ? this.targetBlock : this.golem.getTask().getPos();
        this.golem.getNavigation().moveTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, this.golem.getGolemMoveSpeed());
    }

    @Override
    protected boolean findDestination() {
        for (Task ticket : TaskHandler.getBlockTasksSorted(this.golem.level().dimension(), this.golem.getUUID(), this.golem)) {
            if (this.areGolemTagsValidForTask(ticket) && ticket.canGolemPerformTask(this.golem) && this.golem.isWithinRestriction(ticket.getPos())
                    && this.isValidDestination(this.golem.level(), ticket.getPos()) && this.canEasilyReach(ticket.getPos())) {
                this.targetBlock = this.getAdjacentSpace(ticket.getPos());
                this.golem.setTask(ticket);
                this.golem.getTask().setReserved(true);
                this.golem.level().broadcastEntityEvent(this.golem, (byte) 5);
                return true;
            }
        }
        return false;
    }

    private BlockPos getAdjacentSpace(BlockPos pos) {
        double d = Double.MAX_VALUE;
        BlockPos closest = null;
        for (Direction face : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(face);
            if (!this.golem.level().getBlockState(side).blocksMotion()) {
                double dist = Vec3.atCenterOf(side).distanceToSqr(this.golem.getX(), this.golem.getY(), this.golem.getZ());
                if (dist < d) {
                    closest = side;
                    d = dist;
                }
            }
        }
        return closest;
    }

    private boolean canEasilyReach(BlockPos pos) {
        if (this.golem.distanceToSqr(Vec3.atCenterOf(pos)) < this.minDist) {
            return true;
        }
        Path path = this.golem.getNavigation().createPath(pos, 0);
        if (path == null) {
            return false;
        }
        Node node = path.getEndNode();
        if (node == null) {
            return false;
        }
        int i = node.x - pos.getX();
        int j = node.z - pos.getZ();
        int k = node.y - pos.getY();
        if (i == 0 && j == 0 && k == 2) {
            k--;
        }
        return i * i + j * j + k * k < 2.25;
    }
}
