package thaumcraft.common.entities.construct.golem.ai;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;

/**
 * Дальняя атака (порт AIArrowAttack): в отличие от ванильной, стреляет на ходу — останавливается, только видя цель в
 * радиусе 20+ тиков; без цели доходит по текущему пути.
 */
public class AIArrowAttack extends RangedAttackGoal {
    private final Mob entityHost;
    private final RangedAttackMob rangedAttackEntityHost;
    private int rangedAttackTime = -1;
    private final double entityMoveSpeed;
    private int seeTime;
    private final int attackIntervalMin;
    private final int maxRangedAttackTime;
    private final float attackRadius;
    private final float maxAttackDistance;

    public AIArrowAttack(RangedAttackMob attacker, double movespeed, int minAttackTime, int maxAttackTime, float maxAttackDistanceIn) {
        super(attacker, movespeed, minAttackTime, maxAttackTime, maxAttackDistanceIn);
        this.rangedAttackEntityHost = attacker;
        this.entityHost = (Mob) attacker;
        this.entityMoveSpeed = movespeed;
        this.attackIntervalMin = minAttackTime;
        this.maxRangedAttackTime = maxAttackTime;
        this.attackRadius = maxAttackDistanceIn;
        this.maxAttackDistance = maxAttackDistanceIn * maxAttackDistanceIn;
    }

    @Override
    public boolean canUse() {
        return this.entityHost.getTarget() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() || !this.entityHost.getNavigation().isDone();
    }

    @Override
    public void stop() {
        this.seeTime = 0;
        this.rangedAttackTime = -1;
    }

    @Override
    public void tick() {
        LivingEntity target = this.entityHost.getTarget();
        if (target == null) {
            return;
        }
        double d0 = this.entityHost.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ());
        boolean flag = this.entityHost.getSensing().hasLineOfSight(target);
        if (flag) {
            this.seeTime++;
        } else {
            this.seeTime = 0;
        }
        if (d0 <= this.maxAttackDistance && this.seeTime >= 20) {
            this.entityHost.getNavigation().stop();
        } else {
            this.entityHost.getNavigation().moveTo(target, this.entityMoveSpeed);
        }
        this.entityHost.getLookControl().setLookAt(target, 10.0F, 30.0F);
        if (--this.rangedAttackTime == 0) {
            if (d0 > this.maxAttackDistance || !flag) {
                return;
            }
            float f = (float) Math.sqrt(d0) / this.attackRadius;
            float power = Mth.clamp(f, 0.1F, 1.0F);
            this.rangedAttackEntityHost.performRangedAttack(target, power);
            this.rangedAttackTime = Mth.floor(f * (this.maxRangedAttackTime - this.attackIntervalMin) + this.attackIntervalMin);
        } else if (this.rangedAttackTime < 0) {
            float f2 = (float) Math.sqrt(d0) / this.attackRadius;
            this.rangedAttackTime = Mth.floor(f2 * (this.maxRangedAttackTime - this.attackIntervalMin) + this.attackIntervalMin);
        }
    }
}
