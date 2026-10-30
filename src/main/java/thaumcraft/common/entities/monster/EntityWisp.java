package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.network.fx.PacketFXZap;

import java.util.List;

/**
 * Огонёк (порт EntityWisp): летающий сгусток аспекта (тип — случайный первичный, 1/10 — составной). Бродит, изредка
 * нападает на ближайшего игрока (16 блоков) и бьёт разрядом раз в ~1–2 с: по стоящей цели чаще и сильнее. Выпадает кристалл
 * своего аспекта.
 */
public class EntityWisp extends FlyingMob implements Enemy {
    private static final EntityDataAccessor<String> TYPE = SynchedEntityData.defineId(EntityWisp.class, EntityDataSerializers.STRING);
    private int aggroCooldown = 0;
    public int prevAttackCounter = 0;
    public int attackCounter = 0;
    private BlockPos currentFlightTarget;

    public EntityWisp(EntityType<? extends EntityWisp> type, Level world) {
        super(type, world);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return FlyingMob.createMobAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.ATTACK_DAMAGE, 3.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, "");
    }

    public String getWispType() {
        return this.entityData.get(TYPE);
    }

    public void setWispType(String t) {
        this.entityData.set(TYPE, t);
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.getEntity() instanceof LivingEntity le) {
            this.setTarget(le);
            this.aggroCooldown = 200;
        }
        return super.hurt(src, amount);
    }

    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (this.level().isClientSide) {
            IFXDispatcher.get().burst(this.getX(), this.getY() + 0.45F, this.getZ(), 1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.tickCount <= 1) {
            IFXDispatcher.get().burst(this.getX(), this.getY(), this.getZ(), 10.0F);
        }
        Aspect a = Aspect.getAspect(this.getWispType());
        if (this.level().isClientSide && this.random.nextBoolean() && a != null) {
            IFXDispatcher.get().drawWispParticles(this.getX() + (this.random.nextFloat() - this.random.nextFloat()) * 0.7F,
                    this.getY() + (this.random.nextFloat() - this.random.nextFloat()) * 0.7F,
                    this.getZ() + (this.random.nextFloat() - this.random.nextFloat()) * 0.7F, 0.0, 0.0, 0.0, a.getColor(), 0);
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6F, 1.0));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (Aspect.getAspect(this.getWispType()) == null) {
            List<Aspect> as = this.random.nextInt(10) != 0 ? Aspect.getPrimalAspects() : Aspect.getCompoundAspects();
            this.setWispType(as.get(this.random.nextInt(as.size())).getTag());
        }
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
            return;
        }
        this.prevAttackCounter = this.attackCounter;
        double attackrange = 16.0;
        LivingEntity target = this.getTarget();
        Vec3 m = this.getDeltaMovement();
        if (target != null && this.hasLineOfSight(target)) {
            if (this.distanceToSqr(target) > attackrange * attackrange / 2.0) {
                double var1 = target.getX() - this.getX();
                double var3 = target.getY() + target.getEyeHeight() * 0.66F - this.getY();
                double var5 = target.getZ() - this.getZ();
                m = new Vec3(m.x + (Math.signum(var1) * 0.5 - m.x) * 0.1F, m.y + (Math.signum(var3) * 0.7F - m.y) * 0.1F,
                        m.z + (Math.signum(var5) * 0.5 - m.z) * 0.1F);
                this.setDeltaMovement(m);
                float var7 = (float) (Math.atan2(m.z, m.x) * 180.0 / Math.PI) - 90.0F;
                this.zza = 0.5F;
                this.setYRot(this.getYRot() + Mth.wrapDegrees(var7 - this.getYRot()));
            }
        } else {
            if (this.currentFlightTarget != null && (!this.level().isEmptyBlock(this.currentFlightTarget)
                    || this.currentFlightTarget.getY() < this.level().getMinBuildHeight() + 1
                    || this.currentFlightTarget.getY() > this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, this.currentFlightTarget.getX(),
                    this.currentFlightTarget.getZ()) + 8)) {
                this.currentFlightTarget = null;
            }
            if (this.currentFlightTarget == null || this.random.nextInt(30) == 0
                    || this.currentFlightTarget.distToCenterSqr(this.position()) < 4.0) {
                this.currentFlightTarget = BlockPos.containing(this.getX() + this.random.nextInt(7) - this.random.nextInt(7),
                        this.getY() + this.random.nextInt(6) - 2, this.getZ() + this.random.nextInt(7) - this.random.nextInt(7));
            }
            double var1 = this.currentFlightTarget.getX() + 0.5 - this.getX();
            double var3 = this.currentFlightTarget.getY() + 0.1 - this.getY();
            double var5 = this.currentFlightTarget.getZ() + 0.5 - this.getZ();
            m = new Vec3(m.x + (Math.signum(var1) * 0.5 - m.x) * 0.1F, m.y + (Math.signum(var3) * 0.7F - m.y) * 0.1F,
                    m.z + (Math.signum(var5) * 0.5 - m.z) * 0.1F);
            this.setDeltaMovement(m);
            float var7 = (float) (Math.atan2(m.z, m.x) * 180.0 / Math.PI) - 90.0F;
            this.zza = 0.15F;
            this.setYRot(this.getYRot() + Mth.wrapDegrees(var7 - this.getYRot()));
        }
        if (this.getTarget() instanceof Player p && p.getAbilities().invulnerable) {
            this.setTarget(null);
        }
        if (this.getTarget() != null && !this.getTarget().isAlive()) {
            this.setTarget(null);
        }
        this.aggroCooldown--;
        if (this.random.nextInt(1000) == 0 && (this.getTarget() == null || this.aggroCooldown-- <= 0)) {
            this.setTarget(this.level().getNearestPlayer(this, 16.0));
            if (this.getTarget() != null) {
                this.aggroCooldown = 50;
            }
        }
        target = this.getTarget();
        if (this.isAlive() && target != null && target.distanceToSqr(this) < attackrange * attackrange) {
            double d5 = target.getX() - this.getX();
            double d7 = target.getZ() - this.getZ();
            this.setYRot(-((float) Math.atan2(d5, d7)) * 180.0F / (float) Math.PI);
            this.yBodyRot = this.getYRot();
            if (this.hasLineOfSight(target)) {
                this.attackCounter++;
                if (this.attackCounter == 20) {
                    this.playSound(SoundsTC.zap.get(), 1.0F, 1.1F);
                    Aspect a = Aspect.getAspect(this.getWispType());
                    PacketDistributor.sendToPlayersNear((ServerLevel) this.level(), null, this.getX(), this.getY(), this.getZ(), 32.0,
                            new PacketFXZap(this.position(), target.position(), a != null ? a.getColor() : 0xFFFFFF, 0.6F));
                    float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
                    Vec3 tm = target.getDeltaMovement();
                    if (!(Math.abs(tm.x) > 0.1F) && !(Math.abs(tm.y) > 0.1F) && !(Math.abs(tm.z) > 0.1F)) {
                        if (this.random.nextFloat() < 0.66F) {
                            target.hurt(this.damageSources().mobAttack(this), damage + 1.0F);
                        }
                    } else if (this.random.nextFloat() < 0.4F) {
                        target.hurt(this.damageSources().mobAttack(this), damage);
                    }
                    this.attackCounter = -20 + this.random.nextInt(20);
                }
            } else if (this.attackCounter > 0) {
                this.attackCounter--;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.wisplive.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.LAVA_EXTINGUISH;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.wispdead.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.25F;
    }

    /** dropFewItems: кристалл своего аспекта. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        Aspect a = Aspect.getAspect(this.getWispType());
        if (a != null) {
            this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(a), 0.0F);
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return true;
    }

    /** getCanSpawnHere: не больше 8 огоньков в 16 блоках, не мирно, темно (isValidLightLevel). */
    public static boolean checkWispSpawnRules(EntityType<EntityWisp> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos,
                                              net.minecraft.util.RandomSource rand) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (level.getEntitiesOfClass(EntityWisp.class, new net.minecraft.world.phys.AABB(pos).inflate(16.0)).size() >= 8) {
            return false;
        }
        return isValidLightLevel(level, pos, rand);
    }

    private static boolean isValidLightLevel(LevelAccessor level, BlockPos pos, net.minecraft.util.RandomSource rand) {
        if (level.getBrightness(LightLayer.SKY, pos) > rand.nextInt(32)) {
            return false;
        }
        int i = level.getMaxLocalRawBrightness(pos);
        return i <= rand.nextInt(8);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 2;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putString("Type", this.getWispType());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setWispType(nbt.getString("Type"));
    }
}
