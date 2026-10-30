package thaumcraft.common.entities.monster.cult;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.entities.ai.combat.AICultistHurtByTarget;
import thaumcraft.common.entities.ai.combat.AILongRangeAttack;
import thaumcraft.common.entities.ai.misc.AIAltarFocus;
import thaumcraft.common.entities.projectile.EntityGolemOrb;
import thaumcraft.common.lib.SoundsTC;

/**
 * Клирик Багрового культа (порт EntityCultistCleric): 24 здоровья, роба культа. Издалека (от 2 блоков) — красная
 * электрическая сфера (34%) или три огненных шара. Ритуалист стоит у алтаря и смотрит на него (связь — лентой);
 * удар или ярость (5 зовов на помощь) прерывают ритуал. Ритуалисты не исчезают вдали.
 */
public class EntityCultistCleric extends EntityCultist implements RangedAttackMob, IEntityWithComplexSpawn {
    public int rage = 0;
    private static final EntityDataAccessor<Boolean> RITUALIST = SynchedEntityData.defineId(EntityCultistCleric.class, EntityDataSerializers.BOOLEAN);

    public EntityCultistCleric(EntityType<? extends EntityCultistCleric> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCultist.createAttributes().add(Attributes.MAX_HEALTH, 24.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AIAltarFocus(this));
        this.goalSelector.addGoal(2, new AILongRangeAttack(this, 2.0, 1.0, 20, 40, 24.0F));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new AICultistHurtByTarget(this, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void setLoot(DifficultyInstance diff) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemsTC.crimsonRobeHelm.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ItemsTC.crimsonRobeChest.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ItemsTC.crimsonRobeLegs.get()));
        if (this.random.nextFloat() < (this.level().getDifficulty() == Difficulty.HARD ? 0.3F : 0.1F)) {
            this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ItemsTC.crimsonBoots.get()));
        }
    }

    @Override
    public void performRangedAttack(LivingEntity target, float f) {
        double d0 = target.getX() - this.getX();
        double d1 = target.getBoundingBox().minY + target.getBbHeight() / 2.0F - (this.getY() + this.getBbHeight() / 2.0F);
        double d2 = target.getZ() - this.getZ();
        this.swing(InteractionHand.MAIN_HAND);
        if (this.random.nextFloat() > 0.66F) {
            EntityGolemOrb blast = new EntityGolemOrb(this.level(), this, target, true);
            Vec3 bm = blast.getDeltaMovement();
            blast.setPos(blast.getX() + bm.x / 2.0, blast.getY(), blast.getZ() + bm.z / 2.0);
            blast.shoot(d0, d1 + 2.0, d2, 0.66F, 3.0F);
            this.playSound(SoundsTC.egattack.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.1F);
            this.level().addFreshEntity(blast);
        } else {
            float f1 = Mth.sqrt(f) * 0.5F;
            this.level().levelEvent(null, 1009, this.blockPosition(), 0);
            for (int i = 0; i < 3; i++) {
                SmallFireball ball = new SmallFireball(this.level(), this,
                        new Vec3(d0 + this.random.nextGaussian() * f1, d1, d2 + this.random.nextGaussian() * f1).normalize());
                ball.setPos(ball.getX(), this.getY() + this.getBbHeight() / 2.0F + 0.5, ball.getZ());
                this.level().addFreshEntity(ball);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return !this.getIsRitualist();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RITUALIST, false);
    }

    public boolean getIsRitualist() {
        return this.entityData.get(RITUALIST);
    }

    public void setIsRitualist(boolean ritualist) {
        this.entityData.set(RITUALIST, ritualist);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        this.setIsRitualist(false);
        return super.hurt(source, amount);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setIsRitualist(nbt.getBoolean("ritualist"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("ritualist", this.getIsRitualist());
    }

    /** Дом (алтарь) нужен клиенту — к нему тянется лента и обращён взгляд. */
    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf data) {
        BlockPos home = this.getRestrictCenter();
        data.writeBoolean(this.hasRestriction());
        data.writeInt(home.getX());
        data.writeInt(home.getY());
        data.writeInt(home.getZ());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf data) {
        boolean has = data.readBoolean();
        BlockPos home = new BlockPos(data.readInt(), data.readInt(), data.readInt());
        if (has) {
            this.restrictTo(home, 8);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.getIsRitualist() && this.hasRestriction()) {
            BlockPos home = this.getRestrictCenter();
            double d0 = home.getX() + 0.5 - this.getX();
            double d1 = home.getY() + 1.5 - (this.getY() + this.getEyeHeight());
            double d2 = home.getZ() + 0.5 - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            float f = (float) (Math.atan2(d2, d0) * 180.0 / Math.PI) - 90.0F;
            float f1 = (float) (-(Math.atan2(d1, d3) * 180.0 / Math.PI));
            this.setXRot(updateRotation(this.getXRot(), f1, 10.0F));
            this.yHeadRot = updateRotation(this.yHeadRot, f, this.getMaxHeadXRot());
        }
        if (!this.level().isClientSide && this.getIsRitualist() && this.rage >= 5) {
            this.setIsRitualist(false);
        }
    }

    private static float updateRotation(float from, float to, float max) {
        float f3 = Mth.wrapDegrees(to - from);
        return from + Mth.clamp(f3, -max, max);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundsTC.chant.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 500;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 19) {
            for (int i = 0; i < 3; i++) {
                double d0 = this.random.nextGaussian() * 0.02;
                double d1 = this.random.nextGaussian() * 0.02;
                double d2 = this.random.nextGaussian() * 0.02;
                this.level().addParticle(ParticleTypes.ANGRY_VILLAGER, this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                        this.getY() + 0.5 + this.random.nextFloat() * this.getBbHeight(), this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth(),
                        d0, d1, d2);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }
}
