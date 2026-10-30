package thaumcraft.common.entities.projectile;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Электрическая сфера (порт EntityGolemOrb): медленно летит к цели (ускорение 0.2 / квадрат расстояния, скорость до 0.25
 * по каждой оси), живёт 8 с (красная — 12). Удар — магия: 0.6 урона стрелявшего (красная — 1.0). Удар по сфере
 * разворачивает её туда, куда смотрит ударивший.
 */
public class EntityGolemOrb extends ThrowableProjectile implements IEntityWithComplexSpawn {
    LivingEntity target;
    public boolean red = false;

    public EntityGolemOrb(EntityType<? extends EntityGolemOrb> type, Level world) {
        super(type, world);
    }

    public EntityGolemOrb(Level world, LivingEntity shooter, LivingEntity target, boolean red) {
        super(ConfigEntities.GOLEM_ORB.get(), shooter, world);
        this.target = target;
        this.red = red;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf data) {
        data.writeInt(this.target != null ? this.target.getId() : -1);
        data.writeBoolean(this.red);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf data) {
        int id = data.readInt();
        if (id >= 0 && this.level().getEntity(id) instanceof LivingEntity le) {
            this.target = le;
        }
        this.red = data.readBoolean();
    }

    @Override
    protected void onHit(HitResult mop) {
        super.onHit(mop);
        if (!this.level().isClientSide && this.getOwner() instanceof LivingEntity thrower && mop instanceof EntityHitResult eh) {
            eh.getEntity().hurt(this.damageSources().indirectMagic(this, thrower), (float) thrower.getAttributeValue(Attributes.ATTACK_DAMAGE) * (this.red ? 1.0F : 0.6F));
        }
        this.playSound(SoundsTC.shock.get(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        if (this.level().isClientSide) {
            IFXDispatcher.get().burst(this.getX(), this.getY(), this.getZ(), 1.0F);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > (this.red ? 240 : 160)) {
            this.discard();
        }
        if (this.target != null) {
            double d = this.distanceToSqr(this.target);
            double dx = (this.target.getX() - this.getX()) / d;
            double dy = (this.target.getBoundingBox().minY + this.target.getBbHeight() * 0.6 - this.getY()) / d;
            double dz = (this.target.getZ() - this.getZ()) / d;
            double d13 = 0.2;
            Vec3 m = this.getDeltaMovement().add(dx * d13, dy * d13, dz * d13);
            this.setDeltaMovement(Mth.clamp((float) m.x, -0.25F, 0.25F), Mth.clamp((float) m.y, -0.25F, 0.25F), Mth.clamp((float) m.z, -0.25F, 0.25F));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        Entity e = source.getEntity();
        if (e != null) {
            Vec3 vec3 = e.getLookAngle();
            this.setDeltaMovement(vec3.scale(0.9));
            this.playSound(SoundsTC.zap.get(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            return true;
        }
        return false;
    }
}
