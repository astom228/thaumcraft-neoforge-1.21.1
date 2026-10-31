package thaumcraft.common.entities.construct.golem.parts;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.GolemArm;
import thaumcraft.common.entities.construct.golem.ai.AIArrowAttack;
import thaumcraft.common.entities.projectile.EntityGolemDart;

/** Дротикомёты (порт GolemArmDart): дальняя атака дротиком, урон — треть урона атаки + дальность. */
public class GolemArmDart implements GolemArm.IArmFunction {
    @Override
    public void onMeleeAttack(IGolemAPI golem, Entity target) {
    }

    @Override
    public void onRangedAttack(IGolemAPI golem, LivingEntity target, float range) {
        LivingEntity shooter = golem.getGolemEntity();
        EntityGolemDart dart = new EntityGolemDart(golem.getGolemWorld(), shooter);
        float dmg = (float) shooter.getAttributeValue(Attributes.ATTACK_DAMAGE) / 3.0F;
        dart.setBaseDamage(dmg + range + golem.getGolemWorld().random.nextGaussian() * 0.25);
        double d0 = target.getX() - shooter.getX();
        double d1 = target.getBoundingBox().minY + target.getEyeHeight() + range * range - dart.getY();
        double d2 = target.getZ() - shooter.getZ();
        dart.shoot(d0, d1, d2, 1.6F, 3.0F);
        golem.getGolemWorld().addFreshEntity(dart);
        shooter.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (golem.getGolemWorld().random.nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public RangedAttackGoal getRangedAttackAI(RangedAttackMob golem) {
        return new AIArrowAttack(golem, 1.0, 20, 25, 16.0F);
    }

    @Override
    public void onUpdateTick(IGolemAPI golem) {
    }
}
