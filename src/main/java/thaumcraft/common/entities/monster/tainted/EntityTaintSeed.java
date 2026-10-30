package thaumcraft.common.entities.monster.tainted;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aura.AuraHelper;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.world.aura.AuraHandler;

/**
 * Семя порчи (порт EntityTaintSeed): неподвижный моб-источник порчи. Регистрирует себя в TaintHelper (раз в минуту
 * обновляет), каждую секунду (или каждый тик при «подкормке» boost) разносит волокна в области 6×2×6 (area = 1) —
 * если в чанке есть Выброс, иначе голодает и отдаёт Выброс. Всем рядом (4·area) — отравление скверной. Бьёт подошедших;
 * щупальца у цели — этап 16. Падает кристалл Выброса.
 */
public class EntityTaintSeed extends Monster implements ITaintedMob {
    public int boost = 0;
    boolean firstRun = false;
    public float attackAnim = 0.0F;

    public EntityTaintSeed(EntityType<? extends EntityTaintSeed> type, Level world) {
        super(type, world);
        this.xpReward = 8;
    }

    protected int getArea() {
        return 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 75.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.boost = nbt.getInt("boost");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("boost", this.boost);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.level().broadcastEntityEvent(this, (byte) 16);
        this.playSound(SoundsTC.tentacle.get(), this.getSoundVolume(), this.getVoicePitch());
        return super.doHurtTarget(target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            this.attackAnim = 0.5F;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof ITaintedMob) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof ITaintedMob || super.isAlliedTo(other);
    }

    /** getCanSpawnHere оригинала: не мирно, не в жидкости, без столкновений и без другого семени в 0.8 области порчи. */
    public boolean canSpawnHere() {
        double r = ModConfig.CONFIG_WORLD.taintSpreadArea.get() * 0.8;
        return this.level().getDifficulty() != Difficulty.PEACEFUL && !this.level().containsAnyLiquid(this.getBoundingBox())
                && this.level().noCollision(this, this.getBoundingBox()) && this.level().isUnobstructed(this)
                && this.level().getEntitiesOfClass(EntityTaintSeed.class, new AABB(this.blockPosition()).inflate(r)).isEmpty();
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide) {
            TaintHelper.removeTaintSeed(this.level(), this.blockPosition());
        }
        super.die(cause);
    }

    @Override
    public void tick() {
        super.tick();
        Level w = this.level();
        if (!w.isClientSide) {
            if (!this.firstRun || this.tickCount % 1200 == 0) {
                TaintHelper.removeTaintSeed(w, this.blockPosition());
                TaintHelper.addTaintSeed(w, this.blockPosition());
                this.firstRun = true;
            }
            boolean tickFlag = this.tickCount % 20 == 0;
            if (this.boost > 0 || tickFlag) {
                float mod = this.boost > 0 ? 1.0F : AuraHandler.getFluxSaturation(w, this.blockPosition());
                if (this.boost > 0) {
                    this.boost--;
                }
                if (mod <= 0.0F) {
                    this.hurt(this.damageSources().starve(), 0.5F);
                    AuraHelper.polluteAura(w, this.blockPosition(), 0.1F, false);
                } else {
                    int a = this.getArea();
                    TaintHelper.spreadFibres(w, this.blockPosition().offset(Mth.nextInt(this.random, -a * 3, a * 3), Mth.nextInt(this.random, -a, a),
                            Mth.nextInt(this.random, -a * 3, a * 3)), true);
                }
            }
            if (tickFlag) {
                LivingEntity target = this.getTarget();
                if (target != null && this.distanceToSqr(target) < this.getArea() * 256 && this.getSensing().hasLineOfSight(target)) {
                    // TODO этап 16: малые щупальца порчи (EntityTaintacleSmall) у цели на порче
                }
                for (LivingEntity elb : w.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(this.getArea() * 4), e -> e != this)) {
                    elb.addEffect(new MobEffectInstance(ConfigPotions.FLUX_TAINT, 100, this.getArea() - 1, false, true));
                }
            }
        } else {
            if (this.attackAnim > 0.0F) {
                this.attackAnim *= 0.75F;
            }
            if (this.attackAnim < 0.001) {
                this.attackAnim = 0.0F;
            }
            float xx = Mth.sin(this.tickCount * 0.05F - 0.5F) / 5.0F;
            float zz = Mth.sin(this.tickCount * 0.06F - 0.5F) / 5.0F + this.hurtTime / 200.0F + this.attackAnim;
            if (this.random.nextFloat() < 0.033) {
                IFXDispatcher.get().drawLightningFlash((float) this.getX() + xx, (float) this.getY() + this.getBbHeight() + 0.25F, (float) this.getZ() + zz,
                        0.7F, 0.1F, 0.9F, 0.5F, 1.5F + this.random.nextFloat());
            } else {
                IFXDispatcher.get().drawTaintParticles((float) this.getX() + xx, (float) this.getY() + this.getBbHeight() + 0.25F, (float) this.getZ() + zz,
                        (float) this.random.nextGaussian() * 0.05F, 0.1F + 0.01F * this.random.nextFloat(), (float) this.random.nextGaussian() * 0.05F, 2.0F);
            }
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CHORUS_FLOWER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F - this.getBbHeight() / 10.0F;
    }

    @Override
    protected float getSoundVolume() {
        return this.getBbHeight() / 8.0F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundsTC.tentacle.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.tentacle.get();
    }

    /** dropFewItems оригинала: кристалл Выброса. */
    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource src, boolean recentlyHit) {
        this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(Aspect.FLUX), this.getBbHeight() / 2.0F);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public void moveRelative(float amount, Vec3 relative) {
    }

    /** Не двигается вбок и вверх — только падает. */
    @Override
    public void move(MoverType type, Vec3 pos) {
        super.move(type, new Vec3(0.0, Math.min(0.0, pos.y), 0.0));
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }
}
