package thaumcraft.common.entities.monster.tainted;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.common.blocks.world.taint.TaintHelper;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Заражённое щупальце (порт EntityTaintacle): неподвижно, растёт из скверны и вянет вне её (1 урона в секунду). Бьёт
 * подошедших; цель в 4–16 блоках на скверне получает малое щупальце под ноги. Кристалл Выброса.
 */
public class EntityTaintacle extends Monster implements ITaintedMob {
    public float flailIntensity = 1.0F;

    public EntityTaintacle(EntityType<? extends EntityTaintacle> type, Level world) {
        super(type, world);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof ITaintedMob) && super.canAttack(target);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof ITaintedMob || super.isAlliedTo(other);
    }

    /** На скверне (в клетке или под ней). */
    public static boolean onTaint(Level world, BlockPos pos) {
        return TaintHelper.isTaint(world.getBlockState(pos)) || TaintHelper.isTaint(world.getBlockState(pos.below()));
    }

    /** getCanSpawnHere: только на скверне и не в мирном режиме. */
    public static boolean checkTaintacleSpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource rand) {
        return level.getDifficulty() != Difficulty.PEACEFUL && (TaintHelper.isTaint(level.getBlockState(pos)) || TaintHelper.isTaint(level.getBlockState(pos.below())));
    }

    @Override
    public void move(MoverType type, Vec3 pos) {
        super.move(type, new Vec3(0.0, Math.min(0.0, pos.y), 0.0));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount % 20 == 0) {
            if (!onTaint(this.level(), this.blockPosition())) {
                this.hurt(this.damageSources().starve(), 1.0F);
            }
            LivingEntity t = this.getTarget();
            if (!(this instanceof EntityTaintacleSmall) && this.tickCount % 40 == 0 && t != null && this.distanceToSqr(t) > 16.0
                    && this.distanceToSqr(t) < 256.0 && this.getSensing().hasLineOfSight(t)) {
                spawnTentacles(this, t);
            }
        }
        if (this.level().isClientSide) {
            if (this.flailIntensity > 1.0F) {
                this.flailIntensity -= 0.01F;
            }
            if (this.tickCount < this.getBbHeight() * 10.0F && this.onGround()) {
                IFXDispatcher.get().tentacleAriseFX(this);
            }
        }
    }

    /** Малое щупальце под цель, если она стоит на скверне (в 1.12 — ещё в биоме Потустороннего, этап 17). */
    public static void spawnTentacles(Mob source, Entity entity) {
        Level w = source.level();
        if (onTaint(w, entity.blockPosition())) {
            EntityTaintacleSmall taintlet = ConfigEntities.TAINTACLE_SMALL.get().create(w);
            if (taintlet != null) {
                taintlet.moveTo(entity.getX() + w.random.nextFloat() - w.random.nextFloat(), entity.getY(),
                        entity.getZ() + w.random.nextFloat() - w.random.nextFloat(), 0.0F, 0.0F);
                w.addFreshEntity(taintlet);
                source.playSound(SoundsTC.tentacle.get(), 1.0F, 1.0F);
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

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        this.spawnAtLocation(ThaumcraftApiHelper.makeCrystal(Aspect.FLUX), this.getBbHeight() / 2.0F);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 16) {
            this.flailIntensity = 3.0F;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.level().broadcastEntityEvent(this, (byte) 16);
        this.playSound(SoundsTC.tentacle.get(), this.getSoundVolume(), this.getVoicePitch());
        return super.doHurtTarget(target);
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
