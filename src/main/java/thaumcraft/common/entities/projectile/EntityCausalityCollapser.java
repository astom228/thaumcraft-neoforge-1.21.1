package thaumcraft.common.entities.projectile;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.lib.fx.GenPart;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Брошенный коллапсер (порт EntityCausalityCollapser): скорость 0.8, красный след без модели;
 * при столкновении — взрыв силы 2 и схлопывание разломов в области ±3 блока, как в оригинале.
 */
public class EntityCausalityCollapser extends ThrowableProjectile {
    public EntityCausalityCollapser(EntityType<? extends EntityCausalityCollapser> type, Level level) {
        super(type, level);
    }

    public EntityCausalityCollapser(Level level, LivingEntity thrower) {
        super(ConfigEntities.CAUSALITY_COLLAPSER.get(), thrower, level);
    }

    public EntityCausalityCollapser(Level level, double x, double y, double z) {
        super(ConfigEntities.CAUSALITY_COLLAPSER.get(), x, y, z, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, 0.8F, inaccuracy);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            for (int i = 0; i < 3; i++) {
                double coefficient = i / 3.0;
                IFXDispatcher.get().drawAlumentum((float) (this.xo + (this.getX() - this.xo) * coefficient),
                        (float) (this.yo + (this.getY() - this.yo) * coefficient) + this.getBbHeight() / 2.0F,
                        (float) (this.zo + (this.getZ() - this.zo) * coefficient),
                        0.0125F * (this.random.nextFloat() - 0.5F), 0.0125F * (this.random.nextFloat() - 0.5F),
                        0.0125F * (this.random.nextFloat() - 0.5F), 0.8F + this.random.nextFloat() * 0.2F,
                        0.3F + this.random.nextFloat() * 0.1F, this.random.nextFloat() * 0.1F, 0.5F, 4.0F);
                GenPart particle = new GenPart();
                particle.age = 8;
                particle.alpha = new float[] {0.7F};
                particle.partStart = 448;
                particle.partNum = 8;
                particle.partInc = 1;
                particle.scale = new float[] {0.3F};
                particle.layer = 1;
                IFXDispatcher.get().drawGenericParticles(this.getX() + this.random.nextGaussian() * 0.2F,
                        this.getY() + this.random.nextGaussian() * 0.2F, this.getZ() + this.random.nextGaussian() * 0.2F,
                        0.0, 0.0, 0.0, particle);
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!this.level().isClientSide) {
            // createExplosion(..., 2.0F, true) оригинала: без огня, с разрушением блоков.
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, Level.ExplosionInteraction.TNT);
            AABB area = new AABB(this.position(), this.position()).inflate(3.0);
            for (EntityFluxRift rift : this.level().getEntitiesOfClass(EntityFluxRift.class, area)) {
                rift.setCollapse(true);
            }
            this.discard();
        }
    }
}
