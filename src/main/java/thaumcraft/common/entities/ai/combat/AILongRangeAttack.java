package thaumcraft.common.entities.ai.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/** Дальняя атака (порт AILongRangeAttack): как ванильная, но только если цель дальше minDistance. */
public class AILongRangeAttack extends RangedAttackGoal {
    private final Mob wielder;
    private final double minDistance;

    public AILongRangeAttack(RangedAttackMob mob, double min, double speed, int minInterval, int maxInterval, float maxDistance) {
        super(mob, speed, minInterval, maxInterval, maxDistance);
        this.minDistance = min;
        this.wielder = (Mob) mob;
    }

    @Override
    public boolean canUse() {
        if (!super.canUse()) {
            return false;
        }
        LivingEntity t = this.wielder.getTarget();
        if (t == null) {
            return false;
        }
        if (!t.isAlive()) {
            this.wielder.setTarget(null);
            return false;
        }
        return this.wielder.distanceToSqr(t.getX(), t.getBoundingBox().minY, t.getZ()) >= this.minDistance * this.minDistance;
    }
}
