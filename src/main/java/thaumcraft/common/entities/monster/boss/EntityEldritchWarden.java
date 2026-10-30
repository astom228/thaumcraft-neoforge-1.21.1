package thaumcraft.common.entities.monster.boss;

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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.api.entities.IEldritchMob;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.monster.EntityEldritchGuardian;
import thaumcraft.common.entities.monster.IArmLifting;
import thaumcraft.common.entities.monster.mods.ChampionModifier;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.TCTags;
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.network.fx.PacketFXBlockArc;
import thaumcraft.common.lib.network.fx.PacketFXSonic;

/**
 * Потусторонний надзиратель (порт EntityEldritchWarden), босс: 400 здоровья, броня +4, урон 10, имя-титул из 15.
 * Поднимается из земли 7.5 с, окружён щитом поглощения (66% здоровья, восстанавливается по 1 раз в 25 тиков; вторая
 * синяя полоса). Где ходит — оставляет под собой иссушающий «сок». Когда щит пробит — один раз за жизнь «неистовство»:
 * 7.5 с неуязвим, телепортируется домой и выпускает расходящиеся кольца сока с разрядами. Издалека — сферы (80%) или
 * вопль (иссушение, слабость, отбрасывание, 3–5 временного Искажения). Не нападает на стражей.
 */
public class EntityEldritchWarden extends EntityThaumcraftBoss implements RangedAttackMob, IEldritchMob, IArmLifting {
    protected final ServerBossEvent bossInfo2 = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    static final String[] TITLES = {"Aphoom-Zhah", "Basatan", "Chaugnar Faugn", "Mnomquah", "Nyogtha", "Oorn", "Shaikorth", "Rhan-Tegoth", "Rhogog",
            "Shudde M'ell", "Vulthoom", "Yag-Kosha", "Yibb-Tstll", "Zathog", "Zushakon"};
    private static final EntityDataAccessor<Byte> NAME = SynchedEntityData.defineId(EntityEldritchWarden.class, EntityDataSerializers.BYTE);
    boolean fieldFrenzy = false;
    int fieldFrenzyCounter = 0;
    boolean lastBlast = false;
    public float armLiftL = 0.0F;
    public float armLiftR = 0.0F;

    public EntityEldritchWarden(EntityType<? extends EntityEldritchWarden> type, Level world) {
        super(type, world);
    }

    /** MAX_ABSORPTION — в 1.21 поглощение ограничено атрибутом; 264 = 66% от 400. */
    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes().add(Attributes.MAX_HEALTH, 400.0).add(Attributes.MOVEMENT_SPEED, 0.33).add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MAX_ABSORPTION, 264.0);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossInfo2.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossInfo2.removePlayer(player);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AILongRangeAttack(this, 3.0, 1.0, 20, 40, 24.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, true, false, e -> e.getType().is(TCTags.EntityTypes.CULTISTS)));
    }

    @Override
    public void generateName() {
        Integer t = ThaumcraftApiHelper.getChampionMod(this);
        if (t != null && t >= 0 && t < ChampionModifier.mods.length) {
            this.setCustomName(Component.translatable("entity.thaumcraft.eldritchwarden.custom", this.getTitle(), ChampionModifier.mods[t].getModName()));
        }
    }

    private String getTitle() {
        return TITLES[Math.floorMod(this.entityData.get(NAME), TITLES.length)];
    }

    private void setTitle(int title) {
        this.entityData.set(NAME, (byte) title);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(NAME, (byte) 0);
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

    @Override
    public int getArmorValue() {
        return super.getArmorValue() + 4;
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
    protected void customServerAiStep() {
        if (this.fieldFrenzyCounter == 0) {
            super.customServerAiStep();
        }
        int bh = (int) (this.getAttributeBaseValue(Attributes.MAX_HEALTH) * 0.66);
        if (this.invulnerableTime <= 0 && this.tickCount % 25 == 0 && this.getAbsorptionAmount() < bh) {
            this.setAbsorptionAmount(this.getAbsorptionAmount() + 1.0F);
        }
        this.bossInfo2.setProgress(Mth.clamp(this.getAbsorptionAmount() / bh, 0.0F, 1.0F));
    }

    @Override
    public void tick() {
        if (this.getSpawnTimer() == 150) {
            this.level().broadcastEntityEvent(this, (byte) 18);
        }
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
            IFXDispatcher.get().wispFXEG(x, (float) (this.getY() + 0.25 * this.getBbHeight()), z, this);
            if (this.spawnTimer > 0) {
                float he = Math.max(1.0F, this.getBbHeight() * ((150 - this.spawnTimer) / 150.0F));
                for (int a = 0; a < 33; a++) {
                    IFXDispatcher.get().smokeSpiral(this.getX(), this.getBoundingBox().minY + he / 2.0F, this.getZ(), he, this.random.nextInt(360),
                            Mth.floor(this.getBoundingBox().minY) - 1, 2232623);
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // сок под ногами — на сервере (в оригинале ставился и на клиенте)
        if (!this.level().isClientSide) {
            for (int l = 0; l < 4; l++) {
                int i = Mth.floor(this.getX() + (l % 2 * 2 - 1) * 0.25F);
                int j = Mth.floor(this.getY());
                int k = Mth.floor(this.getZ() + (l / 2 % 2 * 2 - 1) * 0.25F);
                BlockPos bp = new BlockPos(i, j, k);
                if (this.level().isEmptyBlock(bp)) {
                    this.level().setBlockAndUpdate(bp, BlocksTC.effectSap.get().defaultBlockState());
                }
            }
            if (this.fieldFrenzyCounter > 0) {
                if (this.fieldFrenzyCounter == 150) {
                    this.teleportHome();
                }
                this.performFieldFrenzy();
            }
        }
    }

    private void performFieldFrenzy() {
        if (this.fieldFrenzyCounter < 121 && this.fieldFrenzyCounter % 10 == 0) {
            this.level().broadcastEntityEvent(this, (byte) 17);
            double radius = (150 - this.fieldFrenzyCounter) / 8.0;
            int d = 1 + this.fieldFrenzyCounter / 8;
            int i = Mth.floor(this.getX());
            int j = Mth.floor(this.getY());
            int k = Mth.floor(this.getZ());
            for (int q = 0; q < 180 / d; q++) {
                double radians = Math.toRadians(q * 2 * d);
                int deltaX = (int) (radius * Math.cos(radians));
                int deltaZ = (int) (radius * Math.sin(radians));
                BlockPos bp = new BlockPos(i + deltaX, j, k + deltaZ);
                if (!this.level().isLoaded(bp)) {
                    continue;
                }
                BlockPos down = bp.below();
                if (this.level().isEmptyBlock(bp) && this.level().getBlockState(down).isRedstoneConductor(this.level(), down)) {
                    this.level().setBlockAndUpdate(bp, BlocksTC.effectSap.get().defaultBlockState());
                    this.level().scheduleTick(bp, BlocksTC.effectSap.get(), 250 + this.random.nextInt(150));
                    if (this.random.nextFloat() < 0.3F) {
                        PacketDistributor.sendToPlayersNear((ServerLevel) this.level(), null, this.getX(), this.getY(), this.getZ(), 32.0,
                                new PacketFXBlockArc(bp, this, 0.5F + this.random.nextFloat() * 0.2F, 0.0F, 0.5F + this.random.nextFloat() * 0.2F));
                    }
                }
            }
            this.playSound(SoundsTC.zap.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.1F);
        }
        this.fieldFrenzyCounter--;
    }

    /** Телепорт к центру дома (как в оригинале: подходящая опора ищется только под самим центром). */
    protected void teleportHome() {
        if (!this.hasRestriction()) {
            return;
        }
        BlockPos home = this.getRestrictCenter();
        var event = EventHooks.onEnderTeleport(this, home.getX(), home.getY(), home.getZ());
        if (event.isCanceled()) {
            return;
        }
        double d3 = this.getX();
        double d4 = this.getY();
        double d5 = this.getZ();
        double tx = event.getTargetX();
        double ty = event.getTargetY();
        double tz = event.getTargetZ();
        boolean flag = false;
        int i = Mth.floor(tx);
        int j = Mth.floor(ty);
        int k = Mth.floor(tz);
        BlockPos bp = new BlockPos(i, j, k);
        if (this.level().isLoaded(bp)) {
            BlockState block = this.level().getBlockState(bp.below());
            BlockState block2 = this.level().getBlockState(bp);
            boolean flag1 = block.blocksMotion() && !block2.blocksMotion();
            if (!flag1) {
                // 20 попыток в оригинале меняли i, k, но проверяли ту же точку bp — последняя пара и остаётся
                for (int tries = 0; tries < 20; tries++) {
                    i = Mth.floor(tx) + this.random.nextInt(8) - this.random.nextInt(8);
                    k = Mth.floor(tz) + this.random.nextInt(8) - this.random.nextInt(8);
                }
            }
            if (flag1) {
                this.setPos(i + 0.5, j + 0.1, k + 0.5);
                if (this.level().noCollision(this, this.getBoundingBox())) {
                    flag = true;
                }
            }
        }
        if (!flag) {
            this.setPos(d3, d4, d5);
        } else {
            ServerLevel sl = (ServerLevel) this.level();
            for (int l = 0; l < 128; l++) {
                double d6 = l / 127.0;
                float f = (this.random.nextFloat() - 0.5F) * 0.2F;
                float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
                float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
                double d7 = d3 + (this.getX() - d3) * d6 + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
                double d8 = d4 + (this.getY() - d4) * d6 + this.random.nextDouble() * this.getBbHeight();
                double d9 = d5 + (this.getZ() - d5) * d6 + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
                sl.sendParticles(ParticleTypes.PORTAL, d7, d8, d9, 0, f, f1, f2, 1.0);
            }
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean isInvulnerableTo(DamageSource ds) {
        return this.fieldFrenzyCounter > 0 || super.isInvulnerableTo(ds);
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (this.isInvulnerableTo(source) || source.is(DamageTypes.DROWN) || source.is(DamageTypes.WITHER)) {
            return false;
        }
        boolean aef = super.hurt(source, damage);
        if (!this.level().isClientSide && aef && !this.fieldFrenzy && this.getAbsorptionAmount() <= 0.0F) {
            this.fieldFrenzy = true;
            this.fieldFrenzyCounter = 150;
        }
        return aef;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data) {
        this.spawnTimer = 150;
        this.setTitle(this.random.nextInt(TITLES.length));
        this.setAbsorptionAmount((float) (this.getAbsorptionAmount() + this.getAttributeBaseValue(Attributes.MAX_HEALTH) * 0.66));
        return super.finalizeSpawn(level, diff, reason, data);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        if (this.random.nextFloat() > 0.2F) {
            EntityEldritchOrb blast = new EntityEldritchOrb(this.level(), this);
            this.lastBlast = !this.lastBlast;
            this.level().broadcastEntityEvent(this, (byte) (this.lastBlast ? 16 : 15));
            int rr = this.lastBlast ? 90 : 180;
            double xx = Mth.cos((this.getYRot() + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
            double yy = 0.13;
            double zz = Mth.sin((this.getYRot() + rr) % 360.0F / 180.0F * (float) Math.PI) * 0.5F;
            blast.setPos(blast.getX() - xx, blast.getY() - yy, blast.getZ() - zz);
            var tm = target.getDeltaMovement();
            double d0 = target.getX() + tm.x - this.getX();
            double d1 = target.getY() - this.getY() - target.getBbHeight() / 2.0F;
            double d2 = target.getZ() + tm.z - this.getZ();
            blast.shoot(d0, d1, d2, 1.0F, 2.0F);
            this.playSound(SoundsTC.egattack.get(), 2.0F, 1.0F + this.random.nextFloat() * 0.1F);
            this.level().addFreshEntity(blast);
        } else if (this.hasLineOfSight(target)) {
            PacketDistributor.sendToPlayersNear((ServerLevel) this.level(), null, this.getX(), this.getY(), this.getZ(), 32.0, new PacketFXSonic(this.getId()));
            target.push(-Mth.sin(this.getYRot() * (float) Math.PI / 180.0F) * 1.5F, 0.1, Mth.cos(this.getYRot() * (float) Math.PI / 180.0F) * 1.5F);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 400, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 400, 0));
            if (target instanceof ServerPlayer p) {
                ThaumcraftApi.internalMethods.addWarpToPlayer(p, 3 + this.random.nextInt(3), IPlayerWarp.EnumWarpType.TEMPORARY);
            }
            this.playSound(SoundsTC.egscreech.get(), 4.0F, 1.0F + this.random.nextFloat() * 0.1F);
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
        } else if (id == 18) {
            this.spawnTimer = 150;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !(target instanceof EntityEldritchGuardian) && super.canAttack(target);
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
}
