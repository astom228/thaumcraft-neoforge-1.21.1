package thaumcraft.common.entities.construct.golem.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;

/** Возврат к дому (порт AIGotoHome): раз в 50 тиков; далеко (>16) — шаг в сторону дома, ближе — прямо к дому. */
public class AIGotoHome extends Goal {
    protected EntityThaumcraftGolem golem;
    private double movePosX;
    private double movePosY;
    private double movePosZ;
    protected int idleCounter = 10;

    public AIGotoHome(EntityThaumcraftGolem g) {
        this.golem = g;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        if (this.idleCounter > 0) {
            this.idleCounter--;
            return false;
        }
        this.idleCounter = 50;
        BlockPos home = this.golem.getRestrictCenter();
        double dd = this.golem.distanceToSqr(Vec3.atCenterOf(home));
        if (dd < 9.0) {
            return false;
        }
        if (dd > 256.0) {
            Vec3 vec3 = DefaultRandomPos.getPosTowards(this.golem, 16, 7, Vec3.atLowerCornerOf(home), Math.PI / 2);
            if (vec3 == null) {
                return false;
            }
            this.movePosX = vec3.x;
            this.movePosY = vec3.y;
            this.movePosZ = vec3.z;
        } else {
            this.movePosX = home.getX();
            this.movePosY = home.getY();
            this.movePosZ = home.getZ();
        }
        return true;
    }

    @Override
    public void start() {
        this.golem.getNavigation().moveTo(this.movePosX, this.movePosY, this.movePosZ, this.golem.getGolemMoveSpeed());
    }

    @Override
    public boolean canContinueToUse() {
        return this.golem.getTask() != null && !this.golem.getNavigation().isDone()
                && this.golem.distanceToSqr(Vec3.atCenterOf(this.golem.getRestrictCenter())) > 3.0;
    }

    @Override
    public void stop() {
        this.idleCounter = 50;
    }
}
