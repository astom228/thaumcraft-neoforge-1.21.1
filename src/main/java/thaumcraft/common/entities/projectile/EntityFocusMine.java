package thaumcraft.common.entities.projectile;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusPackage;
import thaumcraft.api.casters.Trajectory;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Мина фокуса (порт EntityFocusMine): падает, при касании взводится и через 2 с срабатывает на первых врагов (или союзников)
 * в 1 блоке — остаток пакета применяется к каждому (по тику на цель), мина исчезает. Живёт до 60 с.
 */
public class EntityFocusMine extends ThrowableProjectile implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Boolean> ARMED = SynchedEntityData.defineId(EntityFocusMine.class, EntityDataSerializers.BOOLEAN);
    FocusPackage focusPackage;
    boolean friendly = false;
    public int counter = 40;
    FocusEffect[] effects = null;

    public EntityFocusMine(EntityType<? extends EntityFocusMine> type, Level level) {
        super(type, level);
    }

    public EntityFocusMine(FocusPackage pack, Trajectory trajectory, boolean friendly) {
        super(ConfigEntities.FOCUS_MINE.get(), pack.getCaster(), pack.world);
        this.focusPackage = pack;
        this.friendly = friendly;
        this.setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        this.shoot(trajectory.direction.x, trajectory.direction.y, trajectory.direction.z, 0.0F, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ARMED, false);
    }

    public boolean getIsArmed() {
        return this.entityData.get(ARMED);
    }

    public void setIsArmed(boolean armed) {
        this.entityData.set(ARMED, armed);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.01F;
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

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("armed", this.getIsArmed());
        if (this.focusPackage != null) {
            nbt.put("pack", this.focusPackage.serialize());
        }
        nbt.putBoolean("friendly", this.friendly);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.friendly = nbt.getBoolean("friendly");
        this.setIsArmed(nbt.getBoolean("armed"));
        if (this.getIsArmed()) {
            this.counter = 0;
        }
        try {
            this.focusPackage = new FocusPackage();
            this.focusPackage.deserialize(nbt.getCompound("pack"));
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onHit(HitResult mop) {
        if (this.getOwner() != null) {
            this.setIsArmed(true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        // pushOutOfBlocks 1.12: внутри блока — выталкивается и замедляется
        if (!this.level().noCollision(this, this.getBoundingBox().deflate(1.0E-7))) {
            this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.25));
        }
        if (this.tickCount > 1200 || !this.level().isClientSide && this.getOwner() == null) {
            this.discard();
        }
        if (!this.isAlive() || !this.getIsArmed()) {
            return;
        }
        if (this.counter > 0) {
            this.counter--;
        }
        if (this.counter > 0 || this.tickCount % 5 != 0) {
            return;
        }
        if (this.level().isClientSide) {
            if (this.effects == null && this.focusPackage != null) {
                this.effects = this.focusPackage.getFocusEffects();
            }
            if (this.effects != null && this.effects.length > 0) {
                var rand = this.random;
                FocusEffect eff = this.effects[rand.nextInt(this.effects.length)];
                eff.renderParticleFX(this.level(), this.getX() + rand.nextGaussian() * 0.1, this.getY() + rand.nextGaussian() * 0.1,
                        this.getZ() + rand.nextGaussian() * 0.1, rand.nextGaussian() * 0.01F, rand.nextGaussian() * 0.01F, rand.nextGaussian() * 0.01F);
            }
            return;
        }
        if (this.focusPackage == null) {
            return;
        }
        List<LivingEntity> list2 = EntityUtils.getEntitiesInRange(this.level(), this.getX(), this.getY(), this.getZ(), this, LivingEntity.class, 1.0);
        int d = 0;
        for (final LivingEntity e : list2) {
            if (e.isAlive() && (this.friendly == EntityUtils.isFriendly(this.focusPackage.getCaster(), e))) {
                final Vec3 epv = e.position().add(0.0, e.getBbHeight() / 2.0F, 0.0);
                final Vec3 mine = this.position();
                final FocusPackage pack = this.focusPackage;
                final Entity thrower = this.getOwner();
                ServerEvents.addRunnableServer(this.level(), () -> {
                    if (thrower instanceof LivingEntity caster) {
                        HitResult ray = new EntityHitResult(e, e.position().add(0.0, e.getBbHeight() / 2.0F, 0.0));
                        FocusEngine.runFocusPackage(pack.copy(caster), new Trajectory[] {new Trajectory(mine, epv.subtract(mine).normalize())},
                                new HitResult[] {ray});
                    }
                }, d++);
            }
        }
        if (d > 0) {
            this.discard();
        }
    }
}
