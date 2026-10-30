package thaumcraft.common.entities.monster.boss;

import java.util.ArrayList;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Основа боссов (порт EntityThaumcraftBoss): полоса здоровья (фиолетовая, затемняет небо), дом в 24 блоках от места
 * появления, лечение 1 ед. раз в 1.5 с, неуязвимость пока идёт таймер появления, не тонет и не вязнет в паутине.
 * Урон больше 35 срезается до 35 и злит босса на 10 с (регенерация, сила и спешка). Учёт урона по атакующим
 * (aggro) и усиление за число игроков перенесены как в оригинале, включая его странности: смена цели фактически
 * не происходит, а «игроки» считаются по текущей цели.
 * Выпадение: изначальная жемчужина и редкий мешок с добычей.
 */
public class EntityThaumcraftBoss extends Monster {
    protected final ServerBossEvent bossInfo = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS).setDarkenScreen(true);
    private static final EntityDataAccessor<Integer> AGGRO = SynchedEntityData.defineId(EntityThaumcraftBoss.class, EntityDataSerializers.INT);
    final HashMap<Integer, Integer> aggro = new HashMap<>();
    protected int spawnTimer = 0;

    public EntityThaumcraftBoss(EntityType<? extends EntityThaumcraftBoss> type, Level world) {
        super(type, world);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.KNOCKBACK_RESISTANCE, 0.95).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGGRO, 0);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        if (nbt.contains("HomeD")) {
            this.restrictTo(new BlockPos(nbt.getInt("HomeX"), nbt.getInt("HomeY"), nbt.getInt("HomeZ")), nbt.getInt("HomeD"));
        }
        if (this.hasCustomName()) {
            this.bossInfo.setName(this.getDisplayName());
        }
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
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossInfo.setName(this.getDisplayName());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.getTarget() != null && !this.getTarget().isAlive()) {
            this.setTarget(null);
        }
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

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        this.restrictTo(this.blockPosition(), 24);
        this.generateName();
        this.bossInfo.setName(this.getDisplayName());
        return data;
    }

    public int getAnger() {
        return this.entityData.get(AGGRO);
    }

    public void setAnger(int anger) {
        this.entityData.set(AGGRO, anger);
    }

    public int getSpawnTimer() {
        return this.spawnTimer;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getSpawnTimer() > 0) {
            this.spawnTimer--;
        }
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
        if (!this.level().isClientSide) {
            if (this.tickCount % 30 == 0) {
                this.heal(1.0F);
            }
            if (this.getTarget() != null && this.tickCount % 20 == 0) {
                this.updateAggro();
            }
        }
    }

    /** Перенос оригинала как есть: newTarget берётся по id текущей цели, ld получает id существа, а не урон. */
    private void updateAggro() {
        ArrayList<Integer> dl = new ArrayList<>();
        int players = 0;
        int hei = this.getTarget().getId();
        int ad = this.aggro.getOrDefault(hei, 0);
        int ld = ad;
        Entity newTarget = null;
        for (Integer ei : this.aggro.keySet()) {
            int ca = this.aggro.get(ei);
            if (ca > ad + 25 && ca > ad * 1.1 && ca > ld) {
                newTarget = this.level().getEntity(hei);
                if (newTarget != null && newTarget.isAlive() && !(this.distanceToSqr(newTarget) > 16384.0)) {
                    hei = ei;
                    ld = ei;
                    if (newTarget instanceof Player) {
                        players++;
                    }
                } else {
                    dl.add(ei);
                }
            }
        }
        for (Integer ei : dl) {
            this.aggro.remove(ei);
        }
        if (newTarget instanceof LivingEntity le && hei != this.getTarget().getId()) {
            this.setTarget(le);
        }
        float om = this.getMaxHealth();
        var hp = this.getAttribute(Attributes.MAX_HEALTH);
        var dmg = this.getAttribute(Attributes.ATTACK_DAMAGE);
        for (int a = 0; a < 5; a++) {
            if (dmg != null) {
                dmg.removeModifier(EntityUtils.DMGBUFF[a].id());
            }
            hp.removeModifier(EntityUtils.HPBUFF[a].id());
        }
        for (int a = 0; a < Math.min(5, players - 1); a++) {
            hp.addPermanentModifier(EntityUtils.HPBUFF[a]);
            if (dmg != null) {
                dmg.addPermanentModifier(EntityUtils.DMGBUFF[a]);
            }
        }
        double mm = this.getMaxHealth() / om;
        this.setHealth((float) (this.getHealth() * mm));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource ds) {
        return super.isInvulnerableTo(ds) || this.getSpawnTimer() > 0;
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && this.getSpawnTimer() <= 0;
    }

    @Override
    protected int decreaseAirSupply(int air) {
        return air;
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motion) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, motion);
        }
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level, RandomSource rand, DifficultyInstance diff) {
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof IEldritchMob;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        EntityUtils.entityDropSpecialItem(this, new ItemStack(ItemsTC.primordialPearl.get()), this.getBbHeight() / 2.0F);
        this.spawnAtLocation(new ItemStack(ItemsTC.lootBagRare.get()), 1.5F);
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!this.level().isClientSide) {
            if (source.getEntity() instanceof LivingEntity) {
                int target = source.getEntity().getId();
                int ad = (int) damage;
                if (this.aggro.containsKey(target)) {
                    ad += this.aggro.get(target);
                }
                this.aggro.put(target, ad);
            }
            if (damage > 35.0F) {
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
        }
        return super.hurt(source, damage);
    }

    /** Имя с модификатором чемпиона (переопределяют боссы). */
    public void generateName() {
    }
}
