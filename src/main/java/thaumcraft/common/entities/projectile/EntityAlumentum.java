package thaumcraft.common.entities.projectile;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.fx.GenPart;
import thaumcraft.common.lib.fx.IFXDispatcher;

/** Брошенный алюментум (порт EntityAlumentum): летит со скоростью 0.75, оставляя огненный след; при ударе — взрыв силы 1.1. */
public class EntityAlumentum extends ThrowableProjectile {
    public EntityAlumentum(EntityType<? extends EntityAlumentum> type, Level level) {
        super(type, level);
    }

    public EntityAlumentum(Level level, LivingEntity thrower) {
        super(ConfigEntities.ALUMENTUM.get(), thrower, level);
    }

    public EntityAlumentum(Level level, double x, double y, double z) {
        super(ConfigEntities.ALUMENTUM.get(), x, y, z, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, 0.75F, inaccuracy);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            for (double i = 0.0; i < 3.0; i++) {
                double coeff = i / 3.0;
                IFXDispatcher.get().drawAlumentum((float) (this.xo + (this.getX() - this.xo) * coeff),
                        (float) (this.yo + (this.getY() - this.yo) * coeff) + this.getBbHeight() / 2.0F,
                        (float) (this.zo + (this.getZ() - this.zo) * coeff), 0.0125F * (this.random.nextFloat() - 0.5F),
                        0.0125F * (this.random.nextFloat() - 0.5F), 0.0125F * (this.random.nextFloat() - 0.5F), this.random.nextFloat() * 0.2F,
                        this.random.nextFloat() * 0.1F, this.random.nextFloat() * 0.1F, 0.5F, 4.0F);
                GenPart pp = new GenPart();
                pp.age = 8;
                pp.alpha = new float[] {0.7F};
                pp.partStart = 448;
                pp.partNum = 8;
                pp.partInc = 1;
                pp.scale = new float[] {0.3F};
                pp.layer = 1;
                IFXDispatcher.get().drawGenericParticles(this.getX() + this.random.nextGaussian() * 0.2F, this.getY() + this.random.nextGaussian() * 0.2F,
                        this.getZ() + this.random.nextGaussian() * 0.2F, 0.0, 0.0, 0.0, pp);
            }
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        if (!this.level().isClientSide) {
            // createExplosion(…, 1.1F, true) 1.12 — без огня, разрушает блоки
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.1F, Level.ExplosionInteraction.TNT);
            this.discard();
        }
    }
}
