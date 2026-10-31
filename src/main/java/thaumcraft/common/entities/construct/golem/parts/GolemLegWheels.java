package thaumcraft.common.entities.construct.golem.parts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.GolemLeg;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;

/**
 * Колесо (порт GolemLegWheels): на клиенте крутит колесо по пройденному пути и пылит блоком под собой. Угол поворота
 * хранится в поле голема (в оригинале — статическая карта по id сущности, общая для всех миров).
 */
public class GolemLegWheels implements GolemLeg.ILegFunction {
    @Override
    public void onUpdateTick(IGolemAPI golem) {
        if (!golem.getGolemWorld().isClientSide || !(golem.getGolemEntity() instanceof EntityThaumcraftGolem g)) {
            return;
        }
        LivingEntity e = golem.getGolemEntity();
        double d0 = e.getX() - e.xo;
        double d1 = e.getY() - e.yo;
        double d2 = e.getZ() - e.zo;
        double dist = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
        float lastRot = g.wheelRot;
        float dirTravel = (float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
        double dir = 360.0F - (e.getYRot() - dirTravel);
        lastRot = (float) (lastRot + dist / 1.571 * dir);
        if (lastRot > 360.0F) {
            lastRot -= 360.0F;
        }
        g.wheelRot = lastRot;
        if (e.onGround() && !e.isInWater() && dist > 0.25) {
            BlockPos blockpos = BlockPos.containing(e.getX(), e.getY() - 0.2F, e.getZ());
            BlockState state = e.level().getBlockState(blockpos);
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                Vec3 motion = e.getDeltaMovement();
                e.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state),
                        e.getX() + (e.getRandom().nextFloat() - 0.5) * e.getBbWidth(), e.getBoundingBox().minY + 0.1,
                        e.getZ() + (e.getRandom().nextFloat() - 0.5) * e.getBbWidth(), -motion.x * 4.0, 1.5, -motion.z * 4.0);
            }
        }
    }
}
