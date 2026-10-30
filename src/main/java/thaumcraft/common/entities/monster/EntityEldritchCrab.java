package thaumcraft.common.entities.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.SoundsTC;

/**
 * Потусторонний краб (порт EntityEldritchCrab): с шлемом (броня 5, медленнее; на высокой сложности — всегда) или без.
 * Без шлема в прыжке запрыгивает на голову цели и грызёт её. Шлем слетает при половине здоровья. Не отравляется.
 * Иногда роняет жемчуг Края (только если убит игроком).
 */
public class EntityEldritchCrab extends Monster implements IEldritchMob {
    private static final EntityDataAccessor<Boolean> HELM = SynchedEntityData.defineId(EntityEldritchCrab.class, EntityDataSerializers.BOOLEAN);
    private int attackTime = 0;

    public EntityEldritchCrab(EntityType<? extends EntityEldritchCrab> type, Level world) {
        super(type, world);
        this.xpReward = 6;
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.63F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, e -> e.getType().is(thaumcraft.common.lib.TCTags.EntityTypes.CULTISTS)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HELM, false);
    }

    public boolean hasHelm() {
        return this.entityData.get(HELM);
    }

    /** Шлем: броня 5 и скорость 0.275; без него — 0 и 0.3. */
    public void setHelm(boolean b) {
        this.entityData.set(HELM, b);
        if (!this.level().isClientSide) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(b ? 0.275 : 0.3);
            this.getAttribute(Attributes.ARMOR).setBaseValue(b ? 5.0 : 0.0);
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        this.setHelm(level.getDifficulty() == Difficulty.HARD || this.random.nextFloat() < 0.33F);
        if (data == null) {
            data = new Spider.SpiderEffectsGroupData();
            if (level.getDifficulty() == Difficulty.HARD && this.random.nextFloat() < 0.1F * diff.getSpecialMultiplier()) {
                ((Spider.SpiderEffectsGroupData) data).setRandomEffect(this.random);
            }
        }
        if (data instanceof Spider.SpiderEffectsGroupData sd && sd.effect != null) {
            this.addEffect(new MobEffectInstance(sd.effect, -1));
        }
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override
    public void tick() {
        super.tick();
        this.attackTime--;
        if (this.tickCount < 20) {
            this.fallDistance = 0.0F;
        }
        LivingEntity t = this.getTarget();
        if (this.getVehicle() == null && t != null && t.getVehicle() == null && !this.onGround() && !this.hasHelm() && t.isAlive()
                && this.getY() - t.getY() >= t.getBbHeight() / 2.0F && this.distanceToSqr(t) < 4.0) {
            this.startRiding(t, true);
        }
        if (!this.level().isClientSide && this.getVehicle() != null && this.attackTime <= 0) {
            this.attackTime = 10 + this.random.nextInt(10);
            this.doHurtTarget(this.getVehicle());
            if (this.getVehicle() != null && this.random.nextFloat() < 0.2) {
                this.stopRiding();
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, src, recentlyHit);
        int looting = src.getEntity() instanceof LivingEntity le
                ? net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(
                level.registryAccess().holderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), le) : 0;
        if (recentlyHit && (this.random.nextInt(3) == 0 || this.random.nextInt(1 + looting) > 0)) {
            this.spawnAtLocation(Items.ENDER_PEARL);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (super.doHurtTarget(target)) {
            this.playSound(SoundsTC.crabclaw.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        boolean b = super.hurt(src, amount);
        if (this.hasHelm() && this.getHealth() / this.getMaxHealth() <= 0.5F) {
            this.setHelm(false);
            // renderBrokenItemStack оригинала: звук поломки и осколки лат культа
            this.playSound(SoundEvents.ITEM_BREAK, 0.8F, 0.8F + this.random.nextFloat() * 0.4F);
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,
                        new ItemStack(ItemsTC.crimsonPlateChest.get())), this.getX(), this.getY() + this.getBbHeight() / 2.0, this.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
            }
        }
        return b;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setHelm(nbt.getBoolean("helm"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("helm", this.hasHelm());
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.crabtalk.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.GENERIC_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.crabdeath.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 1.0F);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.POISON) && super.canBeAffected(effect);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof EntityEldritchCrab;
    }

    /** Сидя на голове цели — чуть выше (getYOffset оригинала 0.5). */
    @Override
    public net.minecraft.world.phys.Vec3 getVehicleAttachmentPoint(Entity vehicle) {
        return super.getVehicleAttachmentPoint(vehicle).add(0.0, -0.5, 0.0);
    }
}
