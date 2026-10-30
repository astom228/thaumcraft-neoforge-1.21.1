package thaumcraft.common.entities.monster.boss;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Гигантское щупальце (порт EntityTaintacleGiant), босс: щупальце высотой 6 блоков, 175 здоровья, урон 9; всегда
 * чемпион. Полоса здоровья, лечение 1 ед. раз в 1.5 с, урон больше 35 срезается и злит его (как у EntityThaumcraftBoss).
 * Изначальная жемчужина падает только с последнего гигантского щупальца в радиусе 48 блоков.
 */
public class EntityTaintacleGiant extends EntityTaintacle implements IEldritchMob {
    protected final ServerBossEvent bossInfo = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);
    private static final EntityDataAccessor<Integer> AGGRO = SynchedEntityData.defineId(EntityTaintacleGiant.class, EntityDataSerializers.INT);

    public EntityTaintacleGiant(EntityType<? extends EntityTaintacleGiant> type, Level world) {
        super(type, world);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createGiantAttributes() {
        return EntityTaintacle.createAttributes().add(Attributes.MAX_HEALTH, 175.0).add(Attributes.ATTACK_DAMAGE, 9.0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        EntityUtils.makeChampion(this, true);
        return data;
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossInfo.setName(this.getDisplayName());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getAnger() > 0 && !this.level().isClientSide) {
            this.setAnger(this.getAnger() - 1);
        }
        if (this.level().isClientSide && this.random.nextInt(15) == 0 && this.getAnger() > 0) {
            double d0 = this.random.nextGaussian() * 0.02;
            double d1 = this.random.nextGaussian() * 0.02;
            double d2 = this.random.nextGaussian() * 0.02;
            this.level().addParticle(ParticleTypes.ANGRY_VILLAGER, this.getX() + this.random.nextFloat() * this.getBbWidth() - this.getBbWidth() / 2.0,
                    this.getBoundingBox().minY + this.getBbHeight() + this.random.nextFloat() * 0.5, this.getZ() + this.random.nextFloat() * this.getBbWidth() - this.getBbWidth() / 2.0,
                    d0, d1, d2);
        }
        if (!this.level().isClientSide && this.tickCount % 30 == 0) {
            this.heal(1.0F);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGGRO, 0);
    }

    public int getAnger() {
        return this.entityData.get(AGGRO);
    }

    public void setAnger(int anger) {
        this.entityData.set(AGGRO, anger);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean recentlyHit) {
        var ents = EntityUtils.getEntitiesInRange(level, this.getX(), this.getY(), this.getZ(), this, EntityTaintacleGiant.class, 48.0);
        if (ents == null || ents.isEmpty()) {
            EntityUtils.entityDropSpecialItem(this, new ItemStack(ItemsTC.primordialPearl.get()), this.getBbHeight() / 2.0F);
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!this.level().isClientSide && damage > 35.0F) {
            if (this.getAnger() == 0) {
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, (int) (damage / 15.0F)));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, (int) (damage / 10.0F)));
                this.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, (int) (damage / 40.0F)));
                this.setAnger(200);
                if (source.getEntity() instanceof Player p) {
                    p.displayClientMessage(Component.empty().append(this.getName()).append(" ").append(Component.translatable("tc.boss.enrage")), true);
                }
            }
            damage = 35.0F;
        }
        return super.hurt(source, damage);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossInfo.removePlayer(player);
    }
}
