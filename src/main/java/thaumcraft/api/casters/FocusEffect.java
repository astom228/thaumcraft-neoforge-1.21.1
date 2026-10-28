package thaumcraft.api.casters;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Эффект фокуса (порт FocusEffect): действует на цель; частицы — на клиенте. */
public abstract class FocusEffect extends FocusNode {
    @Override
    public EnumUnitType getType() {
        return EnumUnitType.EFFECT;
    }

    @Override
    public final EnumSupplyType[] mustBeSupplied() {
        return new EnumSupplyType[] {EnumSupplyType.TARGET};
    }

    @Override
    public EnumSupplyType[] willSupply() {
        return null;
    }

    public abstract boolean execute(HitResult target, @Nullable Trajectory trajectory, float finalPower, int num);

    public float getDamageForDisplay(float finalPower) {
        return 0.0F;
    }

    public abstract void renderParticleFX(Level world, double posX, double posY, double posZ, double motionX, double motionY, double motionZ);

    public void onCast(Entity caster) {
    }
}
