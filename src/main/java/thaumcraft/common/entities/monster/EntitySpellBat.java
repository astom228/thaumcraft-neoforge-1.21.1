package thaumcraft.common.entities.monster;

import java.awt.Color;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Летучая мышь заклинания (порт EntitySpellBat): 30 с летает рядом, ищет ближайшего врага (или союзника) в 12 блоках и
 * при касании применяет к нему остаток пакета (раз в 2 с, теряя 1 здоровья). Светится, без добычи. Пропадает без заклинателя.
 */
public class EntitySpellBat extends Monster implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Boolean> FRIENDLY = SynchedEntityData.defineId(EntitySpellBat.class, EntityDataSerializers.BOOLEAN);
    private BlockPos currentFlightTarget;
    public LivingEntity owner = null;
    FocusPackage focusPackage;
    private UUID ownerUniqueId;
    public int damBonus = 0;
    private int attackTime;
    FocusEffect[] effects = null;
    /** Только клиент: средний цвет эффектов для рендера. */
    public int color = 0xFFFFFF;

    public EntitySpellBat(EntityType<? extends EntitySpellBat> type, Level world) {
        super(type, world);
    }

    public EntitySpellBat(FocusPackage pac, boolean friendly) {
        this(ConfigEntities.SPELL_BAT.get(), pac.world);
        this.focusPackage = pac;
        this.setOwner(pac.getCaster());
        this.setIsFriendly(friendly);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FRIENDLY, false);
    }

    public boolean getIsFriendly() {
        return this.entityData.get(FRIENDLY);
    }

    public void setIsFriendly(boolean friendly) {
        this.entityData.set(FRIENDLY, friendly);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf data) {
        data.writeNbt(this.focusPackage == null ? new CompoundTag() : this.focusPackage.serialize());
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf data) {
        try {
            this.focusPackage = new FocusPackage();
            this.focusPackage.deserialize(data.readNbt());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOwner(@Nullable LivingEntity ownerIn) {
        this.owner = ownerIn;
        this.ownerUniqueId = ownerIn == null ? null : ownerIn.getUUID();
    }

    @Nullable
    public LivingEntity getOwner() {
        if (this.owner == null && this.ownerUniqueId != null && this.level() instanceof ServerLevel sl
                && sl.getEntity(this.ownerUniqueId) instanceof LivingEntity e) {
            this.owner = e;
        }
        return this.owner;
    }

    @Override
    protected float getSoundVolume() {
        return 0.1F;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.95F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BAT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BAT_DEATH;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    @Nullable
    public PlayerTeam getTeam() {
        LivingEntity o = this.getOwner();
        return o != null ? o.getTeam() : super.getTeam();
    }

    @Override
    public boolean isAlliedTo(Entity otherEntity) {
        LivingEntity o = this.getOwner();
        if (otherEntity == o) {
            return true;
        }
        return o == null ? super.isAlliedTo(otherEntity) : o.isAlliedTo(otherEntity) || otherEntity.isAlliedTo(o);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && (this.tickCount > 600 || this.getOwner() == null)) {
            this.discard();
        }
        this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, 0.6F, 1.0));
        if (this.isAlive() && this.level().isClientSide && this.focusPackage != null) {
            if (this.effects == null) {
                this.effects = this.focusPackage.getFocusEffects();
                if (this.effects.length > 0) {
                    int r = 0;
                    int g = 0;
                    int b = 0;
                    for (FocusEffect ef : this.effects) {
                        Color c = new Color(FocusEngine.getElementColor(ef.getKey()));
                        r += c.getRed();
                        g += c.getGreen();
                        b += c.getBlue();
                    }
                    this.color = new Color(r / this.effects.length, g / this.effects.length, b / this.effects.length).getRGB();
                }
            }
            if (this.effects.length > 0) {
                FocusEffect eff = this.effects[this.random.nextInt(this.effects.length)];
                eff.renderParticleFX(this.level(), this.getX() + this.random.nextGaussian() * 0.125,
                        this.getY() + this.getBbHeight() / 2.0F + this.random.nextGaussian() * 0.125, this.getZ() + this.random.nextGaussian() * 0.125,
                        0.0, 0.0, 0.0);
            }
        }
    }

    /** updateAITasks оригинала: полёт к случайной точке или к цели, атака касанием. */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.attackTime > 0) {
            this.attackTime--;
        }
        LivingEntity target = this.getTarget();
        Vec3 m = this.getDeltaMovement();
        double var1;
        double var3;
        double var5;
        if (target == null) {
            if (this.currentFlightTarget != null && (!this.level().isEmptyBlock(this.currentFlightTarget) || this.currentFlightTarget.getY() < 1)) {
                this.currentFlightTarget = null;
            }
            if (this.currentFlightTarget == null || this.random.nextInt(30) == 0 || this.currentFlightTarget.distToCenterSqr(this.position()) < 4.0) {
                this.currentFlightTarget = new BlockPos((int) this.getX() + this.random.nextInt(7) - this.random.nextInt(7),
                        (int) this.getY() + this.random.nextInt(6) - 2, (int) this.getZ() + this.random.nextInt(7) - this.random.nextInt(7));
            }
            var1 = this.currentFlightTarget.getX() + 0.5 - this.getX();
            var3 = this.currentFlightTarget.getY() + 0.1 - this.getY();
            var5 = this.currentFlightTarget.getZ() + 0.5 - this.getZ();
        } else {
            var1 = target.getX() - this.getX();
            var3 = target.getY() + target.getEyeHeight() * 0.66F - this.getY();
            var5 = target.getZ() - this.getZ();
        }
        double mx = m.x + (Math.signum(var1) * 0.5 - m.x) * 0.1F;
        double my = m.y + (Math.signum(var3) * 0.7F - m.y) * 0.1F;
        double mz = m.z + (Math.signum(var5) * 0.5 - m.z) * 0.1F;
        this.setDeltaMovement(mx, my, mz);
        float var7 = (float) (Math.atan2(mz, mx) * 180.0 / Math.PI) - 90.0F;
        float var8 = Mth.wrapDegrees(var7 - this.getYRot());
        this.zza = 0.5F;
        this.setYRot(this.getYRot() + var8);
        if (this.getTarget() == null) {
            this.setTarget(this.findTargetToAttack());
        } else if (this.getTarget().isAlive()) {
            float f = this.getTarget().distanceTo(this);
            if (this.hasLineOfSight(this.getTarget())) {
                this.attackEntity(this.getTarget(), f);
            }
        } else {
            this.setTarget(null);
        }
        if (!this.getIsFriendly() && this.getTarget() instanceof Player p && p.getAbilities().invulnerable) {
            this.setTarget(null);
        }
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    protected void attackEntity(Entity target, float dist) {
        if (this.attackTime <= 0 && dist < Math.max(2.5F, target.getBbWidth() * 1.1F) && target.getBoundingBox().maxY > this.getBoundingBox().minY
                && target.getBoundingBox().minY < this.getBoundingBox().maxY) {
            this.attackTime = 40;
            if (!this.level().isClientSide && this.focusPackage != null && this.getOwner() != null) {
                Vec3 hitVec = target.position().add(0.0, target.getBbHeight() / 2.0F, 0.0);
                Trajectory tra = new Trajectory(this.position(), hitVec.subtract(this.position()));
                FocusEngine.runFocusPackage(this.focusPackage.copy(this.getOwner()), new Trajectory[] {tra}, new HitResult[] {new EntityHitResult(target, hitVec)});
                this.setHealth(this.getHealth() - 1.0F);
            }
            this.playSound(SoundEvents.BAT_HURT, 0.5F, 0.9F + this.random.nextFloat() * 0.2F);
        }
    }

    @Override
    protected void doPush(Entity entityIn) {
        if (!this.getIsFriendly()) {
            super.doPush(entityIn);
        }
    }

    protected LivingEntity findTargetToAttack() {
        List<LivingEntity> list = EntityUtils.getEntitiesInRange(this.level(), this.getX(), this.getY(), this.getZ(), this, LivingEntity.class, 12.0);
        double d = Double.MAX_VALUE;
        LivingEntity ret = null;
        for (LivingEntity e : list) {
            if (e.isAlive() && (this.getIsFriendly() ? EntityUtils.isFriendly(this.getOwner(), e)
                    : !EntityUtils.isFriendly(this.getOwner(), e) && !this.isAlliedTo(e))) {
                double ed = this.distanceToSqr(e);
                if (ed < d) {
                    d = ed;
                    ret = e;
                }
            }
        }
        return ret;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.ownerUniqueId = nbt.hasUUID("OwnerUUID") ? nbt.getUUID("OwnerUUID") : null;
        this.setIsFriendly(nbt.getBoolean("friendly"));
        try {
            this.focusPackage = new FocusPackage();
            this.focusPackage.deserialize(nbt.getCompound("pack"));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        if (this.ownerUniqueId != null) {
            nbt.putUUID("OwnerUUID", this.ownerUniqueId);
        }
        if (this.focusPackage != null) {
            nbt.put("pack", this.focusPackage.serialize());
        }
        nbt.putBoolean("friendly", this.getIsFriendly());
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }
}
