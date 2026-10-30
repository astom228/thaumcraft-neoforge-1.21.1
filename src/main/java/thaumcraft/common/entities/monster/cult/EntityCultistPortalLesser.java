package thaumcraft.common.entities.monster.cult;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.SoundsTC;

/**
 * Малый портал культистов (порт EntityCultistPortalLesser): неподвижный, 100 здоровья, броня 4. Когда в 32 блоках есть
 * игрок — открывается и раз в 2.5–5 с, пока видит игрока, выпускает рыцаря (67%) или клирика, пока культистов рядом
 * меньше 2/4/6 (по сложности); каждый выпуск стоит ему 5–9 здоровья. Касание — 4 магического урона. Гибель — взрыв без
 * разрушений.
 */
public class EntityCultistPortalLesser extends Monster {
    private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(EntityCultistPortalLesser.class, EntityDataSerializers.BOOLEAN);
    int stagecounter = 100;
    public int activeCounter = 0;
    public int pulse = 0;

    public EntityCultistPortalLesser(EntityType<? extends EntityCultistPortalLesser> type, Level world) {
        super(type, world);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 100.0).add(Attributes.ATTACK_DAMAGE, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    public int getArmorValue() {
        return 4;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ACTIVE, false);
    }

    public boolean isActive() {
        return this.entityData.get(ACTIVE);
    }

    public void setActive(boolean active) {
        this.entityData.set(ACTIVE, active);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("active", this.isActive());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setActive(nbt.getBoolean("active"));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 motion) {
    }

    @Override
    public void aiStep() {
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 4096.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isActive()) {
            this.activeCounter++;
        }
        if (!this.level().isClientSide) {
            if (!this.isActive()) {
                if (this.tickCount % 10 == 0 && this.level().getNearestPlayer(this, 32.0) != null) {
                    this.setActive(true);
                    this.playSound(SoundsTC.craftstart.get(), 1.0F, 1.0F);
                }
            } else if (this.stagecounter-- <= 0) {
                Player p = this.level().getNearestPlayer(this, 32.0);
                if (p != null && this.hasLineOfSight(p)) {
                    Difficulty d = this.level().getDifficulty();
                    int count = d == Difficulty.HARD ? 6 : (d == Difficulty.NORMAL ? 4 : 2);
                    count -= this.level().getEntitiesOfClass(EntityCultist.class, this.getBoundingBox().inflate(32.0)).size();
                    if (count > 0) {
                        this.level().broadcastEntityEvent(this, (byte) 16);
                        this.spawnMinions();
                    }
                }
                this.stagecounter = 50 + this.random.nextInt(50);
            }
        }
        if (this.pulse > 0) {
            this.pulse--;
        }
    }

    void spawnMinions() {
        EntityType<? extends EntityCultist> type = this.random.nextFloat() > 0.33 ? ConfigEntities.CULTIST_KNIGHT.get() : ConfigEntities.CULTIST_CLERIC.get();
        EntityCultist cultist = type.create(this.level());
        if (cultist == null) {
            return;
        }
        cultist.setPos(this.getX() + this.random.nextFloat() - this.random.nextFloat(), this.getY() + 0.25, this.getZ() + this.random.nextFloat() - this.random.nextFloat());
        ServerLevel sl = (ServerLevel) this.level();
        cultist.finalizeSpawn(sl, sl.getCurrentDifficultyAt(cultist.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        this.level().addFreshEntity(cultist);
        cultist.spawnAnim();
        cultist.playSound(SoundsTC.wandfail.get(), 1.0F, 1.0F);
        this.hurt(this.damageSources().fellOutOfWorld(), 5 + this.random.nextInt(5));
    }

    @Override
    public void playerTouch(Player p) {
        if (this.distanceToSqr(p) < 3.0 && p.hurt(this.damageSources().indirectMagic(this, this), 4.0F)) {
            this.playSound(SoundsTC.zap.get(), 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.1F + 1.0F);
        }
    }

    @Override
    protected float getSoundVolume() {
        return 0.75F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 540;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.monolith.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundsTC.zap.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundsTC.shock.get();
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
    }

    @Override
    public void handleEntityEvent(byte msg) {
        if (msg == 16) {
            this.pulse = 10;
        } else {
            super.handleEntityEvent(msg);
        }
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float mult, DamageSource source) {
        return false;
    }

    @Override
    public void die(DamageSource cause) {
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), 1.5F, Level.ExplosionInteraction.NONE);
        }
        super.die(cause);
    }
}
