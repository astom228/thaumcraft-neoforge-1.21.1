package thaumcraft.common.entities.ai.combat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;

/**
 * Ответ на удар с зовом на помощь (порт AICultistHurtByTarget): все культисты рядом без цели нападают на обидчика.
 * Клирик-ритуалист злится (событие 19) и с шансом 1/3 бросает ритуал. В оригинале цель бралась из текущей цели
 * атаки, а не из обидчика — исправлено (DEVIATIONS 57).
 */
public class AICultistHurtByTarget extends HurtByTargetGoal {
    private final boolean callsForHelp;

    public AICultistHurtByTarget(PathfinderMob mob, boolean callsForHelp) {
        super(mob);
        this.callsForHelp = callsForHelp;
    }

    @Override
    public void start() {
        super.start();
        LivingEntity target = this.mob.getTarget();
        if (!this.callsForHelp || target == null) {
            return;
        }
        double d0 = this.getFollowDistance() * 2.0;
        AABB box = new AABB(this.mob.getX(), this.mob.getY(), this.mob.getZ(), this.mob.getX() + 1.0, this.mob.getY() + 1.0, this.mob.getZ() + 1.0).inflate(d0, 10.0, d0);
        for (EntityCultist other : this.mob.level().getEntitiesOfClass(EntityCultist.class, box)) {
            if (other != this.mob && other.getTarget() == null && !other.isAlliedTo(target)) {
                if (other instanceof EntityCultistCleric cleric && cleric.getIsRitualist()) {
                    cleric.rage++;
                    this.mob.level().broadcastEntityEvent(cleric, (byte) 19);
                    if (this.mob.getRandom().nextInt(3) == 0) {
                        cleric.setIsRitualist(false);
                        cleric.setTarget(target);
                    }
                } else {
                    other.setTarget(target);
                }
            }
        }
    }
}
