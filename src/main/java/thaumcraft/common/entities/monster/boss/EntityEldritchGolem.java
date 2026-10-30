package thaumcraft.common.entities.monster.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Потусторонний голем (порт EntityEldritchGolem), босс: 400 здоровья, броня +6, урон 10 (×0.75 за удар), не горит.
 * Появляется неподвижным (таймер 5 с, лечится по 2). Смертельный удар вместо смерти срывает с него голову (взрыв без
 * разрушений) — без головы он снова неуязвим 5 с, бьёт с отбрасыванием и стреляет самонаводящимися сферами, пока не
 * разрядится, затем 150 тиков заряжается, искрясь разрядами в землю. Ломает под собой хрупкие блоки (твёрдость до 0.15).
 */
public class EntityEldritchGolem extends EntityThaumcraftBoss implements IEldritchMob, RangedAttackMob {
    private static final EntityDataAccessor<Boolean> HEADLESS = SynchedEntityData.defineId(EntityEldritchGolem.class, EntityDataSerializers.BOOLEAN);
    int beamCharge = 0;
    boolean chargingBeam = false;
    int arcing = 0;
    int ax = 0;
    int ay = 0;
    int az = 0;
    private int attackTimer;
    private boolean rangedAdded;

    public EntityEldritchGolem(EntityType<? extends EntityEldritchGolem> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.MAX_HEALTH, 400.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void generateName() {
        Integer t = ThaumcraftApiHelper.getChampionMod(this);
        if (t != null && t >= 0 && t < ChampionModifier.mods.length) {
            this.setCustomName(Component.translatable("entity.thaumcraft.eldritchgolem.custom", ChampionModifier.mods[t].getModName()));
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEADLESS, false);
    }

    public boolean isHeadless() {
        return this.entityData.get(HEADLESS);
    }

    public void setHeadless(boolean headless) {
        this.entityData.set(HEADLESS, headless);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (HEADLESS.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("headless", this.isHeadless());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setHeadless(nbt.getBoolean("headless"));
        if (this.isHeadless()) {
            this.makeHeadless();
        }
    }

    /** getEyeHeight: 3.0, без головы — 3.33. */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(this.isHeadless() ? 3.33F : 3.0F);
    }

    @Override
    public int getArmorValue() {
        return super.getArmorValue() + 6;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.IRON_GOLEM_STEP, 1.0F, 1.0F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        this.spawnTimer = 100;
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.attackTimer > 0) {
            this.attackTimer--;
        }
        Vec3 m = this.getDeltaMovement();
        if (m.x * m.x + m.z * m.z > 2.5000003E-7F && this.random.nextInt(5) == 0) {
            BlockState bs = this.level().getBlockState(this.blockPosition());
            if (!bs.isAir()) {
                this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, bs), this.getX() + (this.random.nextFloat() - 0.5) * this.getBbWidth(),
                        this.getBoundingBox().minY + 0.1, this.getZ() + (this.random.nextFloat() - 0.5) * this.getBbWidth(), 4.0 * (this.random.nextFloat() - 0.5), 0.5,
                        (this.random.nextFloat() - 0.5) * 4.0);
            }
            if (!this.level().isClientSide && bs.getBlock() instanceof thaumcraft.common.blocks.world.BlockLoot) {
                this.level().destroyBlock(this.blockPosition(), true);
            }
        }
        if (!this.level().isClientSide) {
            BlockPos pos = this.blockPosition();
            float h = this.level().getBlockState(pos).getDestroySpeed(this.level(), pos);
            if (h >= 0.0F && h <= 0.15F && !this.level().getBlockState(pos).isAir()) {
                this.level().destroyBlock(pos, true);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!this.level().isClientSide && damage > this.getHealth() && !this.isHeadless()) {
            this.setHeadless(true);
            this.spawnTimer = 100;
            double xx = Mth.cos(this.getYRot() % 360.0F / 180.0F * (float) Math.PI) * 0.75F;
            double zz = Mth.sin(this.getYRot() % 360.0F / 180.0F * (float) Math.PI) * 0.75F;
            this.level().explode(this, this.getX() + xx, this.getY() + this.getEyeHeight(), this.getZ() + zz, 2.0F, Level.ExplosionInteraction.NONE);
            this.makeHeadless();
            return false;
        }
        return super.hurt(source, damage);
    }

    void makeHeadless() {
        if (!this.rangedAdded) {
            this.rangedAdded = true;
            this.goalSelector.addGoal(2, new AILongRangeAttack(this, 3.0, 1.0, 5, 5, 24.0F));
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.attackTimer > 0) {
            return false;
        }
        this.attackTimer = 10;
        this.level().broadcastEntityEvent(this, (byte) 4);
        boolean flag = target.hurt(this.damageSources().mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75F);
        if (flag) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.2F, 0.0));
            if (this.isHeadless()) {
                target.push(-Mth.sin(this.getYRot() * (float) Math.PI / 180.0F) * 1.5F, 0.1, Mth.cos(this.getYRot() * (float) Math.PI / 180.0F) * 1.5F);
            }
        }
        return flag;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (this.hasLineOfSight(target) && !this.chargingBeam && this.beamCharge > 0) {
            this.beamCharge = this.beamCharge - (15 + this.random.nextInt(5));
            this.getLookControl().setLookAt(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0F, target.getZ(), 30.0F, 30.0F);
            Vec3 v = this.getViewVector(1.0F);
            EntityGolemOrb blast = new EntityGolemOrb(this.level(), this, target, false);
            blast.setPos(blast.getX() + v.x, blast.getY(), blast.getZ() + v.z);
            Vec3 tm = target.getDeltaMovement();
            double d0 = target.getX() + tm.x - this.getX();
            double d1 = target.getY() - this.getY() - target.getBbHeight() / 2.0F;
            double d2 = target.getZ() + tm.z - this.getZ();
            blast.shoot(d0, d1, d2, 0.66F, 5.0F);
            this.playSound(SoundsTC.egattack.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.1F);
            this.level().addFreshEntity(blast);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.attackTimer = 10;
            this.playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 1.0F);
        } else if (id == 18) {
            this.spawnTimer = 150;
        } else if (id == 19) {
            if (this.arcing == 0) {
                float radius = 2.0F + this.random.nextFloat() * 2.0F;
                double radians = Math.toRadians(this.random.nextInt(360));
                double deltaX = radius * Math.cos(radians);
                double deltaZ = radius * Math.sin(radians);
                int bx = Mth.floor(this.getX() + deltaX);
                int by = Mth.floor(this.getY());
                int bz = Mth.floor(this.getZ() + deltaZ);
                BlockPos bp = new BlockPos(bx, by, bz);
                // как в оригинале: bp не пересчитывается, цикл лишь сдвигает by
                for (int c = 0; c < 5 && this.level().isEmptyBlock(bp); by--) {
                    c++;
                }
                if (this.level().isEmptyBlock(bp.above()) && !this.level().isEmptyBlock(bp)) {
                    this.ax = bx;
                    this.ay = by;
                    this.az = bz;
                    this.arcing = 8 + this.random.nextInt(5);
                    this.playSound(SoundsTC.jacobs.get(), 0.8F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.05F);
                }
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        if (this.getSpawnTimer() == 150) {
            this.level().broadcastEntityEvent(this, (byte) 18);
        }
        if (this.getSpawnTimer() > 0) {
            this.heal(2.0F);
        }
        super.tick();
        if (this.level().isClientSide) {
            if (this.isHeadless()) {
                this.setXRot(0.0F);
                float f1 = Mth.cos(-this.yBodyRot * ((float) Math.PI / 180.0F) - (float) Math.PI);
                float f2 = Mth.sin(-this.yBodyRot * ((float) Math.PI / 180.0F) - (float) Math.PI);
                float f3 = -Mth.cos(-this.getXRot() * ((float) Math.PI / 180.0F));
                float f4 = Mth.sin(-this.getXRot() * ((float) Math.PI / 180.0F));
                Vec3 v = new Vec3(f2 * f3, f4, f1 * f3);
                if (this.random.nextInt(20) == 0) {
                    float a = (this.random.nextFloat() - this.random.nextFloat()) / 3.0F;
                    float b = (this.random.nextFloat() - this.random.nextFloat()) / 3.0F;
                    IFXDispatcher.get().spark((float) (this.getX() + v.x + a), (float) this.getY() + this.getEyeHeight() - 0.75F, (float) (this.getZ() + v.z + b), 3.0F,
                            0.65F + this.random.nextFloat() * 0.1F, 1.0F, 1.0F, 0.8F);
                }
                IFXDispatcher.get().drawVentParticles((float) this.getX() + v.x * 0.4, (float) this.getY() + this.getEyeHeight() - 1.25F, (float) this.getZ() + v.z * 0.4,
                        0.0, 0.001, 0.0, 5592405, 4.0F);
                if (this.arcing > 0) {
                    IFXDispatcher.get().arcLightning(this.getX(), this.getY() + this.getBbHeight() / 2.0F, this.getZ(), this.ax + 0.5, this.ay + 1, this.az + 0.5,
                            0.65F + this.random.nextFloat() * 0.1F, 1.0F, 1.0F, 1.0F - this.arcing / 10.0F);
                    this.arcing--;
                }
            }
        } else {
            if (this.isHeadless() && this.beamCharge <= 0) {
                this.chargingBeam = true;
            }
            if (this.isHeadless() && this.chargingBeam) {
                this.beamCharge++;
                this.level().broadcastEntityEvent(this, (byte) 19);
                if (this.beamCharge == 150) {
                    this.chargingBeam = false;
                }
            }
        }
    }

    public int getAttackTimer() {
        return this.attackTimer;
    }
}
