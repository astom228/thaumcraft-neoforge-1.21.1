package thaumcraft.common.entities.construct.golem.ai;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;

/** Защита хозяина (порт AIOwnerHurtByTarget): цель — тот, кто последним ранил хозяина. */
public class AIOwnerHurtByTarget extends TargetGoal {
    private final EntityOwnedConstruct theDefendingTameable;
    private LivingEntity theOwnerAttacker;
    private int timestamp;

    public AIOwnerHurtByTarget(EntityOwnedConstruct construct) {
        super(construct, false);
        this.theDefendingTameable = construct;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!this.theDefendingTameable.isOwned()) {
            return false;
        }
        LivingEntity owner = this.theDefendingTameable.getOwnerEntity();
        if (owner == null) {
            return false;
        }
        this.theOwnerAttacker = owner.getLastHurtByMob();
        int i = owner.getLastHurtByMobTimestamp();
        return i != this.timestamp && this.canAttack(this.theOwnerAttacker, TargetingConditions.DEFAULT);
    }

    @Override
    public void start() {
        this.mob.setTarget(this.theOwnerAttacker);
        LivingEntity owner = this.theDefendingTameable.getOwnerEntity();
        if (owner != null) {
            this.timestamp = owner.getLastHurtByMobTimestamp();
        }
        super.start();
    }
}
