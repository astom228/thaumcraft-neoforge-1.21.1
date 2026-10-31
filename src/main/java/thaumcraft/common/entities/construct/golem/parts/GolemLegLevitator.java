package thaumcraft.common.entities.construct.golem.parts;

import net.minecraft.world.entity.LivingEntity;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.common.lib.fx.IFXDispatcher;

/** Левитатор (порт GolemLegLevitator): шлейф частиц под летающим големом (только клиент). */
public class GolemLegLevitator implements GolemLeg.ILegFunction {
    @Override
    public void onUpdateTick(IGolemAPI golem) {
        LivingEntity e = golem.getGolemEntity();
        if (golem.getGolemWorld().isClientSide && (!e.onGround() || e.tickCount % 5 == 0)) {
            IFXDispatcher.get().drawGolemFlyParticles(e.getX(), e.getY() + 0.1, e.getZ(), golem.getGolemWorld().random.nextGaussian() / 100.0, -0.1,
                    golem.getGolemWorld().random.nextGaussian() / 100.0);
        }
    }
}
