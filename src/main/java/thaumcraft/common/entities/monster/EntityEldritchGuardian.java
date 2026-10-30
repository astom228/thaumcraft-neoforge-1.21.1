package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
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
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCTags;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.network.fx.PacketFXSonic;
import thaumcraft.common.lib.network.misc.PacketMiscEvent;

/**
 * Потусторонний страж (порт EntityEldritchGuardian): нежить в плаще из тёмной дымки, броня 4, магия наносит ему половину.
 * Издалека (8–24 блока) стреляет потусторонними сферами (90%) или вопит (10%): иссушение и временное Искажение. Вблизи
 * бьёт рукой. Рядом с ним у игроков сгущается туман (кроме лёгкой сложности). Не больше одного в 32 блоках.
 * Поглощение в измерении Внешних земель не переносится — в BETA17 его нет (DECISIONS 135).
 */
public class EntityEldritchGuardian extends Monster implements RangedAttackMob, IEldritchMob, IArmLifting {
    public float armLiftL = 0.0F;
    public float armLiftR = 0.0F;
    boolean lastBlast = false;

    public EntityEldritchGuardian(EntityType<? extends EntityEldritchGuardian> type, Level world) {
        super(type, world);
        this.xpReward = 20;
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0).add(Attributes.FOLLOW_RANGE, 40.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 7.0).add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AILongRangeAttack(this, 8.0, 1.0, 20, 40, 24.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, e -> e.getType().is(TCTags.EntityTypes.CULTISTS)));
    }

    @Override
    public float getArmLiftL() {
        return this.armLiftL;
    }

    @Override
    public float getArmLiftR() {
        return this.armLiftR;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(net.minecraft.tags.DamageTypeTags.WITCH_RESISTANT_TO)) {
            amount /= 2.0F;
        }
        return super.hurt(src, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.armLiftL > 0.0F) {
                this.armLiftL -= 0.05F;
            }
            if (this.armLiftR > 0.0F) {
                this.armLiftR -= 0.05F;
            }
            float x = (float) (this.getX() + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            float z = (float) (this.getZ() + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
            IFXDispatcher.get().wispFXEG(x, (float) (this.getY() + 0.22 * this.getBbHeight()), z, this);
        } else if ((this.tickCount == 0 || this.tickCount % 100 == 0) && this.level().getDifficulty() != Difficulty.EASY) {
            double d6 = this.level().getDifficulty() == Difficulty.HARD ? 576.0 : 256.0;
            for (Player p : this.level().players()) {
                if (p.isAlive() && p instanceof ServerPlayer sp && p.distanceToSqr(this.getX(), this.getY(), this.getZ()) < d6) {
                    PacketDistributor.sendToPlayer(sp, new PacketMiscEvent(PacketMiscEvent.MIST_EVENT_SHORT));
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean flag = super.doHurtTarget(target);
        if (flag) {
            int i = this.level().getDifficulty().getId();
            if (this.getMainHandItem().isEmpty() && this.isOnFire() && this.random.nextFloat() < i * 0.3F) {
                target.igniteForSeconds(2 * i);
            }
        }
        return flag;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.egidle.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.egdeath.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.hasRestriction() && this.getRestrictRadius() > 0.0F) {
            nbt.putInt("HomeD", (int) this.getRestrictRadius());
            nbt.putInt("HomeX", this.getRestrictCenter().getX());
            nbt.putInt("HomeY", this.getRestrictCenter().getY());
            nbt.putInt("HomeZ", this.getRestrictCenter().getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("HomeD")) {
            this.restrictTo(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")), nbt.getInt("HomeD"));
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 15) {
            this.armLiftL = 0.5F;
        } else if (id == 16) {
            this.armLiftR = 0.5F;
        } else if (id == 17) {
            this.armLiftL = 0.9F;
            this.armLiftR = 0.9F;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return !this.hasRestriction();
    }

    /** getCanSpawnHere: в 32×16×32 нет другого стража; свет не важен. */
    public static boolean checkGuardianSpawnRules(EntityType<EntityEldritchGuardian> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource rand) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getEntitiesOfClass(EntityEldritchGuardian.class, new AABB(pos).inflate(32.0, 16.0, 32.0)).isEmpty()
                && checkMobSpawnRules(type, level, reason, pos, rand);
    }

    /** Сфера (90%; поочерёдно из левой и правой руки) или вопль (10%, если видит цель): иссушение и временное Искажение 1–3. */
    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (this.random.nextFloat() > 0.1F) {
            EntityEldritchOrb blast = new EntityEldritchOrb(this.level(), this);
            this.lastBlast = !this.lastBlast;
            this.level().broadcastEntityEvent(this, (byte) (this.lastBlast ? 16 : 15));
            int rr = this.lastBlast ? 90 : 180;
            double xx = Mth.cos((this.getYRot() + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
            double zz = Mth.sin((this.getYRot() + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
            blast.setPos(blast.getX() - xx, blast.getY(), blast.getZ() - zz);
            Vec3 v = this.getLookAngle();
            blast.shoot(v.x, v.y, v.z, 1.0F, 2.0F);
            this.playSound(SoundsTC.egattack.get(), 2.0F, 1.0F + this.random.nextFloat() * 0.1F);
            this.level().addFreshEntity(blast);
        } else if (this.hasLineOfSight(target)) {
            PacketDistributor.sendToPlayersNear((ServerLevel) this.level(), null, this.getX(), this.getY(), this.getZ(), 32.0, new PacketFXSonic(this.getId()));
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
            if (target instanceof ServerPlayer p) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(p, 1 + this.random.nextInt(3), IPlayerWarp.EnumWarpType.TEMPORARY);
            }
            this.playSound(SoundsTC.egscreech.get(), 3.0F, 1.0F + this.random.nextFloat() * 0.1F);
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof IEldritchMob;
    }
}
