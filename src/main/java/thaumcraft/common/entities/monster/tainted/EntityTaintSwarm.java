package thaumcraft.common.entities.monster.tainted;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Заражённый рой (порт EntityTaintSwarm): облако мошкары скверны (видна только частицами), кружит у семени заражения,
 * нападает на игрока в 8 блоках: кусает, ослабляет. Призванный рой (summoned) теряет здоровье без цели. 1/2 — кристалл Выброса.
 */
public class EntityTaintSwarm extends Monster implements ITaintedMob {
    private static final EntityDataAccessor<Boolean> SUMMONED = SynchedEntityData.defineId(EntityTaintSwarm.class, EntityDataSerializers.BOOLEAN);
    private BlockPos currentFlightTarget;
    public int damBonus = 0;
    /** Только клиент: живые частицы роя этой сущности. */
    public final List<Object> swarm = new ArrayList<>();
    private int attackTime;

    public EntityTaintSwarm(EntityType<? extends EntityTaintSwarm> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SUMMONED, false);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof ITaintedMob) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof ITaintedMob || super.isAlliedTo(other);
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.1F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.swarm.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundsTC.swarmattack.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.swarmattack.get();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public boolean getIsSummoned() {
        return this.entityData.get(SUMMONED);
    }

    public void setIsSummoned(boolean b) {
        this.entityData.set(SUMMONED, b);
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6F, 1.0));
        if (this.level().isClientSide) {
            for (int a = 0; a < this.swarm.size(); a++) {
                if (!IFXDispatcher.get().isParticleAlive(this.swarm.get(a))) {
                    this.swarm.remove(a);
                    break;
                }
            }
            if (this.swarm.size() < 30) {
                Object p = IFXDispatcher.get().swarmParticleFX(this, 0.22F, 15.0F, 0.08F);
                if (p != null) {
                    this.swarm.add(p);
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.getTarget() == null) {
            if (this.getIsSummoned()) {
                this.hurt(this.damageSources().generic(), 5.0F);
            }
            BlockPos ft = this.currentFlightTarget;
            if (ft != null && (!this.level().isEmptyBlock(ft) || ft.getY() < this.level().getMinBuildHeight() + 1
                    || ft.getY() > this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, ft.getX(), ft.getZ()) + 2
                    || !TaintHelper.isNearTaintSeed(this.level(), ft))) {
                this.currentFlightTarget = null;
            }
            if (this.currentFlightTarget == null || this.random.nextInt(30) == 0 || this.currentFlightTarget.distToCenterSqr(this.position()) < 4.0) {
                this.currentFlightTarget = BlockPos.containing(this.getX() + this.random.nextInt(7) - this.random.nextInt(7),
                        this.getY() + this.random.nextInt(6) - 2, this.getZ() + this.random.nextInt(7) - this.random.nextInt(7));
            }
            this.fly(this.currentFlightTarget.getX() + 0.5, this.currentFlightTarget.getY() + 0.1, this.currentFlightTarget.getZ() + 0.5, 0.015F, 0.015F);
        } else {
            LivingEntity t = this.getTarget();
            this.fly(t.getX(), t.getY() + t.getEyeHeight(), t.getZ(), 0.025F, 0.025F);
        }
        if (this.getTarget() == null) {
            this.setTarget(this.getIsSummoned() ? null : this.level().getNearestPlayer(this, 8.0));
        } else if (this.getTarget().isAlive()) {
            float f = this.getTarget().distanceTo(this);
            if (this.hasLineOfSight(this.getTarget())) {
                this.attackEntity(this.getTarget(), f);
            }
        } else {
            this.setTarget(null);
        }
        if (this.getTarget() instanceof Player p && p.getAbilities().invulnerable) {
            this.setTarget(null);
        }
        if (this.attackTime > 0) {
            this.attackTime--;
        }
    }

    private void fly(double x, double y, double z, float kx, float kz) {
        Vec3 m = this.getDeltaMovement();
        m = new Vec3(m.x + (Math.signum(x - this.getX()) * 0.5 - m.x) * kx, m.y + (Math.signum(y - this.getY()) * 0.7F - m.y) * 0.1F,
                m.z + (Math.signum(z - this.getZ()) * 0.5 - m.z) * kz);
        this.setDeltaMovement(m);
        float var7 = (float) (Math.atan2(m.z, m.x) * 180.0 / Math.PI) - 90.0F;
        this.zza = 0.1F;
        this.setYRot(this.getYRot() + Mth.wrapDegrees(var7 - this.getYRot()));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    /** Укус: не отбрасывает (движение цели восстанавливается), ослабляет на 5 с. */
    protected void attackEntity(LivingEntity target, float dist) {
        if (this.attackTime <= 0 && dist < 3.0F && target.getBoundingBox().maxY > this.getBoundingBox().minY
                && target.getBoundingBox().minY < this.getBoundingBox().maxY) {
            this.attackTime = 10 + this.random.nextInt(5);
            Vec3 m = target.getDeltaMovement();
            if (this.doHurtTarget(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            }
            target.hasImpulse = false;
            target.setDeltaMovement(m);
            this.playSound(SoundsTC.swarmattack.get(), 0.3F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setIsSummoned(nbt.getBoolean("summoned"));
        this.damBonus = nbt.getByte("damBonus");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("summoned", this.getIsSummoned());
        nbt.putByte("damBonus", (byte) this.damBonus);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        if (this.random.nextBoolean()) {
            this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(Aspect.FLUX), this.getBbHeight() / 2.0F);
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return true;
    }
}
