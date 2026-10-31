package thaumcraft.common.entities.construct.golem.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;

/** Помощь хозяину (порт AIOwnerHurtTarget): цель — тот, кого хозяин атаковал последним. */
public class AIOwnerHurtTarget extends TargetGoal {
    private final EntityOwnedConstruct theEntityTameable;
    private LivingEntity theTarget;
    private int timestamp;

    public AIOwnerHurtTarget(EntityOwnedConstruct construct) {
        super(construct, false);
        this.theEntityTameable = construct;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!this.theEntityTameable.isOwned()) {
            return false;
        }
        LivingEntity owner = this.theEntityTameable.getOwnerEntity();
        if (owner == null) {
            return false;
        }
        this.theTarget = owner.getLastHurtMob();
        int i = owner.getLastHurtMobTimestamp();
        return i != this.timestamp && this.canAttack(this.theTarget, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.theTarget);
        LivingEntity owner = this.theEntityTameable.getOwnerEntity();
        if (owner != null) {
            this.timestamp = owner.getLastHurtMobTimestamp();
        }
        super.start();
    }
}
