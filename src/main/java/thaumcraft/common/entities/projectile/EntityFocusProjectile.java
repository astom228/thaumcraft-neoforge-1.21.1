package thaumcraft.common.entities.projectile;

import java.awt.Color;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Снаряд фокуса (порт EntityFocusProjectile): летит с лёгкой гравитацией и несёт остаток пакета; при попадании пакет
 * выполняется (на следующем серверном тике). special: 0 — обычный, 1 — отскакивает от блоков, 2/3 — наводится на врагов /
 * союзников в поле зрения 16 блоков. Живёт до 60 с; без заклинателя исчезает.
 */
public class EntityFocusProjectile extends ThrowableProjectile implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Integer> SPECIAL = SynchedEntityData.defineId(EntityFocusProjectile.class, EntityDataSerializers.INT);
    FocusPackage focusPackage;
    private Entity target;
    boolean firstParticle = false;
    public float lastRenderTick = 0.0F;
    FocusEffect[] effects = null;

    public EntityFocusProjectile(EntityType<? extends EntityFocusProjectile> type, Level level) {
        super(type, level);
    }

    public EntityFocusProjectile(FocusPackage pack, float speed, Trajectory trajectory, int special) {
        super(ConfigEntities.FOCUS_PROJECTILE.get(), pack.getCaster(), pack.world);
        this.focusPackage = pack;
        LivingEntity caster = pack.getCaster();
        this.setPos(trajectory.source.x + trajectory.direction.x * caster.getBbWidth() * 2.1,
                trajectory.source.y + trajectory.direction.y * caster.getBbWidth() * 2.1,
                trajectory.source.z + trajectory.direction.z * caster.getBbWidth() * 2.1);
        this.shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, speed, 0.0F);
        this.setSpecial(special);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPECIAL, 0);
    }

    @Override
    protected double getDefaultGravity() {
        return this.getSpecial() > 1 ? 0.005F : 0.01F;
    }

    public void setSpecial(int s) {
        this.entityData.set(SPECIAL, s);
    }

    public int getSpecial() {
        return this.entityData.get(SPECIAL);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf data) {
        data.writeNbt(this.focusPackage == null ? new CompoundTag() : this.focusPackage.serialize());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf data) {
        try {
            this.focusPackage = new FocusPackage();
            this.focusPackage.deserialize(data.readNbt());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.focusPackage != null) {
            nbt.put("pack", this.focusPackage.serialize());
        }
        nbt.putInt("special", this.getSpecial());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setSpecial(nbt.getInt("special"));
        try {
            this.focusPackage = new FocusPackage();
            this.focusPackage.deserialize(nbt.getCompound("pack"));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onHit(HitResult mop) {
        if (this.getSpecial() == 1 && mop instanceof BlockHitResult bh && mop.getType() == HitResult.Type.BLOCK) {
            // отскок: назад на шаг, отражение по грани, потеря 10% скорости
            Vec3 m = this.getDeltaMovement();
            this.setPos(this.getX() - m.x, this.getY() - m.y, this.getZ() - m.z);
            Direction side = bh.getDirection();
            double mx = m.x;
            double my = m.y;
            double mz = m.z;
            if (side.getStepZ() != 0) {
                mz *= -1.0;
            }
            if (side.getStepX() != 0) {
                mx *= -1.0;
            }
            if (side.getStepY() != 0) {
                my *= -0.9;
            }
            mx *= 0.9;
            my *= 0.9;
            mz *= 0.9;
            this.setDeltaMovement(mx, my, mz);
            float var20 = Mth.sqrt((float) (mx * mx + my * my + mz * mz));
            this.setPos(this.getX() - mx / var20 * 0.05F, this.getY() - my / var20 * 0.05F, this.getZ() - mz / var20 * 0.05F);
            if (!this.level().isClientSide) {
                this.playSound(SoundEvents.LEASH_KNOT_PLACE, 0.25F, 1.0F);
                if (new Vec3(mx, my, mz).length() < 0.2) {
                    this.discard();
                }
            }
            return;
        }
        if (!this.level().isClientSide && this.focusPackage != null) {
            final HitResult hit = mop instanceof EntityHitResult eh ? new EntityHitResult(eh.getEntity(), this.position()) : mop;
            final Vec3 pv = new Vec3(this.xo, this.yo, this.zo);
            final Vec3 vf = this.getDeltaMovement();
            final FocusPackage pack = this.focusPackage;
            ServerEvents.addRunnableServer(this.level(),
                    () -> FocusEngine.runFocusPackage(pack, new Trajectory[] {new Trajectory(pv, vf.normalize())}, new HitResult[] {hit}), 0);
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 1200 || !this.level().isClientSide && this.getOwner() == null) {
            this.discard();
        }
        this.firstParticle = true;
        if (this.target == null && this.tickCount % 5 == 0 && this.getSpecial() > 1) {
            Entity thrower = this.getOwner();
            for (LivingEntity pt : EntityUtils.getEntitiesInRangeSorted(this.level(), this, LivingEntity.class, 16.0)) {
                if (pt.isAlive() && EntityUtils.isVisibleTo(1.75F, this, pt, 16.0F) && EntityUtils.canEntityBeSeen(this, pt)) {
                    boolean f = EntityUtils.isFriendly(thrower, pt);
                    if (f && this.getSpecial() == 3) {
                        this.target = pt;
                        break;
                    }
                    if (!f && this.getSpecial() == 2) {
                        this.target = pt;
                        break;
                    }
                }
            }
        }
        if (this.target != null) {
            double dx = this.target.getX() - this.getX();
            double dy = this.target.getBoundingBox().minY + this.target.getBbHeight() * 0.6 - this.getY();
            double dz = this.target.getZ() - this.getZ();
            Vec3 v = new Vec3(dx, dy, dz).normalize();
            Vec3 mv = this.getDeltaMovement();
            double lv = mv.length();
            mv = mv.normalize().add(v.scale(0.275));
            mv = mv.normalize().scale(lv);
            this.setDeltaMovement(mv);
            if (this.tickCount % 5 == 0
                    && (!this.target.isAlive() || !EntityUtils.isVisibleTo(1.75F, this, this.target, 16.0F) || !EntityUtils.canEntityBeSeen(this, this.target))) {
                this.target = null;
            }
        }
    }

    /** Взгляд снаряда — по скорости (для наведения: конус обзора). */
    @Override
    public Vec3 getLookAngle() {
        return this.getDeltaMovement().normalize();
    }

    /** Клиент: огонёк цвета случайного эффекта и раз в тик — частица самого эффекта (вызывает рендер). */
    public void renderParticle(float coeff) {
        this.lastRenderTick = coeff;
        if (this.effects == null && this.focusPackage != null) {
            this.effects = this.focusPackage.getFocusEffects();
        }
        if (this.effects != null && this.effects.length > 0) {
            FocusEffect eff = this.effects[this.random.nextInt(this.effects.length)];
            float scale = 1.0F;
            Color c1 = new Color(FocusEngine.getElementColor(eff.getKey()));
            IFXDispatcher.get().drawFireMote((float) Mth.lerp(coeff, this.xo, this.getX()), (float) Mth.lerp(coeff, this.yo, this.getY()) + this.getBbHeight() / 2.0F,
                    (float) Mth.lerp(coeff, this.zo, this.getZ()), 0.0125F * (this.random.nextFloat() - 0.5F) * scale,
                    0.0125F * (this.random.nextFloat() - 0.5F) * scale, 0.0125F * (this.random.nextFloat() - 0.5F) * scale, c1.getRed() / 255.0F,
                    c1.getGreen() / 255.0F, c1.getBlue() / 255.0F, 0.5F, 7.0F * scale);
            if (this.firstParticle) {
                this.firstParticle = false;
                eff.renderParticleFX(this.level(), Mth.lerp(coeff, this.xo, this.getX()) + this.random.nextGaussian() * 0.1F,
                        Mth.lerp(coeff, this.yo, this.getY()) + this.getBbHeight() / 2.0F + this.random.nextGaussian() * 0.1F,
                        Mth.lerp(coeff, this.zo, this.getZ()) + this.random.nextGaussian() * 0.1F, this.random.nextGaussian() * 0.01F,
                        this.random.nextGaussian() * 0.01F, this.random.nextGaussian() * 0.01F);
            }
        }
    }
}
