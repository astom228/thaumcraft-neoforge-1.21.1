package thaumcraft.common.entities.ai.pech;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import thaumcraft.common.entities.monster.EntityPech;

/** Поиск цели пехом (порт AINearestAttackableTargetPech): только пока он зол. */
public class AINearestAttackableTargetPech<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    public AINearestAttackableTargetPech(Mob mob, Class<T> target, boolean checkSight) {
        super(mob, target, checkSight);
    }

    @Override
    public boolean canUse() {
        return (!(this.mob instanceof EntityPech pech) || pech.getAnger() != 0) && super.canUse();
    }
}
