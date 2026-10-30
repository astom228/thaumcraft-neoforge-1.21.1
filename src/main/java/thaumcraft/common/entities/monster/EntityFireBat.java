package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Огненная летучая мышь (порт EntityFireBat): висит под потолком, срывается при игроке ближе 4 блоков; летает и атакует
 * ближайшего игрока (12 блоков); подлетев, с шансом 1/10 взрывается (сила 1.5, без огня и разрушений). Огнеупорна, светится,
 * боится воды. Выпадает порох.
 */
public class EntityFireBat extends Monster {
    private static final EntityDataAccessor<Boolean> HANGING = SynchedEntityData.defineId(EntityFireBat.class, EntityDataSerializers.BOOLEAN);
    private BlockPos currentFlightTarget;
    public LivingEntity owner = null;
    public int damBonus = 0;
    private int attackTime;

    public EntityFireBat(EntityType<? extends EntityFireBat> type, Level world) {
        super(type, world);
        this.setIsBatHanging(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HANGING, false);
    }

    public boolean getIsBatHanging() {
        return this.entityData.get(HANGING);
    }

    public void setIsBatHanging(boolean b) {
        this.entityData.set(HANGING, b);
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
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.95F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.getIsBatHanging() && this.random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void aiStep() {
        if (this.isInWaterOrRain()) {
            this.hurt(this.damageSources().drown(), 1.0F);
        }
        super.aiStep();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getIsBatHanging()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setPosRaw(this.getX(), Mth.floor(this.getY()) + 1.0 - this.getBbHeight(), this.getZ());
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6F, 1.0));
        }
    }

    private boolean isNormalCube(BlockPos p) {
        BlockState s = this.level().getBlockState(p);
        return s.isRedstoneConductor(this.level(), p);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.attackTime > 0) {
            this.attackTime--;
        }
        BlockPos blockpos = this.blockPosition();
        BlockPos blockpos1 = blockpos.above();
        if (this.getIsBatHanging()) {
            if (!this.isNormalCube(blockpos1)) {
                this.setIsBatHanging(false);
                this.level().levelEvent(null, 1025, blockpos, 0);
            } else {
                if (this.random.nextInt(200) == 0) {
                    this.yHeadRot = this.random.nextInt(360);
                }
                if (this.level().getNearestPlayer(this, 4.0) != null) {
                    this.setIsBatHanging(false);
                    this.level().levelEvent(null, 1025, blockpos, 0);
                }
            }
        } else if (this.getTarget() == null) {
            if (this.currentFlightTarget != null && (!this.level().isEmptyBlock(this.currentFlightTarget)
                    || this.currentFlightTarget.getY() < this.level().getMinBuildHeight() + 1)) {
                this.currentFlightTarget = null;
            }
            if (this.currentFlightTarget == null || this.random.nextInt(30) == 0 || this.currentFlightTarget.distToCenterSqr(this.position()) < 4.0) {
                this.currentFlightTarget = BlockPos.containing(this.getX() + this.random.nextInt(7) - this.random.nextInt(7),
                        this.getY() + this.random.nextInt(6) - 2, this.getZ() + this.random.nextInt(7) - this.random.nextInt(7));
            }
            this.flyTowards(this.currentFlightTarget.getX() + 0.5, this.currentFlightTarget.getY() + 0.1, this.currentFlightTarget.getZ() + 0.5);
            if (this.random.nextInt(100) == 0 && this.isNormalCube(blockpos1)) {
                this.setIsBatHanging(true);
            }
        } else {
            LivingEntity t = this.getTarget();
            this.flyTowards(t.getX(), t.getY() + t.getEyeHeight() * 0.66F, t.getZ());
        }
        if (this.getTarget() == null) {
            this.setTarget(this.level().getNearestPlayer(this, 12.0));
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
    }

    private void flyTowards(double x, double y, double z) {
        Vec3 m = this.getDeltaMovement();
        double var1 = x - this.getX();
        double var3 = y - this.getY();
        double var5 = z - this.getZ();
        m = new Vec3(m.x + (Math.signum(var1) * 0.5 - m.x) * 0.1F, m.y + (Math.signum(var3) * 0.7F - m.y) * 0.1F, m.z + (Math.signum(var5) * 0.5 - m.z) * 0.1F);
        this.setDeltaMovement(m);
        float var7 = (float) (Math.atan2(m.z, m.x) * 180.0 / Math.PI) - 90.0F;
        this.zza = 0.5F;
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

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (this.isInvulnerableTo(src) || src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        if (!this.level().isClientSide && this.getIsBatHanging()) {
            this.setIsBatHanging(false);
        }
        return super.hurt(src, amount);
    }

    protected void attackEntity(Entity target, float dist) {
        if (this.attackTime <= 0 && dist < Math.max(2.5F, target.getBbWidth() * 1.1F) && target.getBoundingBox().maxY > this.getBoundingBox().minY
                && target.getBoundingBox().minY < this.getBoundingBox().maxY) {
            this.attackTime = 20;
            if (this.random.nextInt(10) == 0 && !this.level().isClientSide) {
                target.invulnerableTime = 0;
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.5F, false, Level.ExplosionInteraction.NONE);
                this.discard();
            }
            this.playSound(SoundEvents.BAT_HURT, 0.5F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setIsBatHanging(nbt.getBoolean("hang"));
        this.damBonus = nbt.getByte("damBonus");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("hang", this.getIsBatHanging());
        nbt.putByte("damBonus", (byte) this.damBonus);
    }

    /** getCanSpawnHere: свет не больше random(7), остальное — как у монстров (кроме проверки темноты). */
    public static boolean checkFireBatSpawnRules(EntityType<EntityFireBat> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource rand) {
        return level.getMaxLocalRawBrightness(pos) <= rand.nextInt(7) && checkMobSpawnRules(type, level, reason, pos, rand);
    }
}
