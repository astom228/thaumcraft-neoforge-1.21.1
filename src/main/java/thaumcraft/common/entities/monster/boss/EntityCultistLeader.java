package thaumcraft.common.entities.monster.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.ai.combat.AICultistHurtByTarget;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.monster.cult.EntityCultist;
import thaumcraft.common.entities.monster.cult.EntityCultistKnight;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.lib.SoundsTC;

/**
 * Багровый претор (порт EntityCultistLeader), босс: 150 здоровья, урон 5, латы претора и багровый клинок (на лёгкой —
 * пустотный меч). Культисты в 8 блоках от него регенерируют. Издалека (от 16 блоков) — красные электрические сферы.
 * Выпадает только редкий мешок с добычей.
 */
public class EntityCultistLeader extends EntityThaumcraftBoss implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> NAME = SynchedEntityData.defineId(EntityCultistLeader.class, EntityDataSerializers.BYTE);
    static final String[] TITLES = {"Alberic", "Anselm", "Bastian", "Beturian", "Chabier", "Chorache", "Chuse", "Dodorol", "Ebardo", "Ferrando", "Fertus",
            "Guillen", "Larpe", "Obano", "Zelipe"};

    private boolean pendingSpawnFx;

    public EntityCultistLeader(EntityType<? extends EntityCultistLeader> type, Level world) {
        super(type, world);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.MAX_HEALTH, 150.0).add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AILongRangeAttack(this, 16.0, 1.0, 30, 40, 24.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new AICultistHurtByTarget(this, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NAME, (byte) 0);
    }

    @Override
    public void generateName() {
        Integer t = ThaumcraftApiHelper.getChampionMod(this);
        if (t != null && t >= 0 && t < ChampionModifier.mods.length) {
            this.setCustomName(Component.translatable("entity.thaumcraft.cultistleader.custom", this.getTitle(), ChampionModifier.mods[t].getModName()));
        }
    }

    private String getTitle() {
        return TITLES[Math.floorMod(this.entityData.get(NAME), TITLES.length)];
    }

    private void setTitle(int title) {
        this.entityData.set(NAME, (byte) title);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putByte("title", this.entityData.get(NAME));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setTitle(nbt.getByte("title"));
    }

    private void equip(DifficultyInstance diff) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemsTC.crimsonPraetorHelm.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemsTC.crimsonPraetorChest.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ItemsTC.crimsonPraetorLegs.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemsTC.crimsonBoots.get()));
        if (this.level().getDifficulty() == Difficulty.EASY) {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemsTC.voidSword.get()));
        } else {
            this.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ItemsTC.crimsonBlade.get()));
        }
        float f = diff.getSpecialMultiplier();
        if (!this.getMainHandItem().isEmpty() && this.random.nextFloat() < 0.5F * f) {
            EntityCultistKnight.enchant(this, this.getMainHandItem(), (int) (7.0F + f * this.random.nextInt(22)));
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        this.equip(diff);
        this.setTitle(this.random.nextInt(TITLES.length));
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override
    public boolean isAlliedTo(Entity el) {
        return el instanceof EntityCultist || el instanceof EntityCultistLeader || super.isAlliedTo(el);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof EntityCultist) && !(target instanceof EntityCultistLeader) && super.canAttack(target);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        this.spawnAtLocation(new ItemStack(ItemsTC.lootBagRare.get()), 1.5F);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        for (EntityCultist e : this.level().getEntitiesOfClass(EntityCultist.class, this.getBoundingBox().inflate(8.0), e -> e.distanceToSqr(this) <= 64.0)) {
            if (!e.hasEffect(MobEffects.REGENERATION)) {
                e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
            }
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (this.hasLineOfSight(target)) {
            this.swing(InteractionHand.MAIN_HAND);
            this.getLookControl().setLookAt(target.getX(), target.getBoundingBox().minY + target.getBbHeight() / 2.0F, target.getZ(), 30.0F, 30.0F);
            EntityGolemOrb blast = new EntityGolemOrb(this.level(), this, target, true);
            Vec3 bm = blast.getDeltaMovement();
            blast.setPos(blast.getX() + bm.x / 2.0, blast.getY(), blast.getZ() + bm.z / 2.0);
            double d0 = target.getX() - this.getX();
            double d1 = target.getBoundingBox().minY + target.getBbHeight() / 2.0F - (this.getY() + this.getBbHeight() / 2.0F);
            double d2 = target.getZ() - this.getZ();
            blast.shoot(d0, d1 + 2.0, d2, 0.66F, 3.0F);
            this.playSound(SoundsTC.egattack.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.1F);
            this.level().addFreshEntity(blast);
        }
    }

    @Override
    public void spawnAnim() {
        if (this.level().isClientSide) {
            EntityCultist.cultistSpawnFX(this);
        } else {
            this.pendingSpawnFx = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.pendingSpawnFx && !this.level().isClientSide && this.tickCount >= 2) {
            this.pendingSpawnFx = false;
            this.level().broadcastEntityEvent(this, (byte) 20);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 20) {
            EntityCultist.cultistSpawnFX(this);
        } else {
            super.handleEntityEvent(id);
        }
    }
}
