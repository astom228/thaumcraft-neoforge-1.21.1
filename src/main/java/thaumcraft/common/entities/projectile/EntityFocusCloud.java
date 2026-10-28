package thaumcraft.common.entities.projectile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
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
import thaumcraft.common.lib.fx.IFXDispatcher;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Облако фокуса (порт EntityFocusCloud): висит duration секунд и каждые 5 тиков применяет остаток пакета к существам в радиусе
 * и к случайным блокам вокруг (блок — не чаще раза в 2 с). Соседние облака сталкиваются. Пропадает без заклинателя.
 * Как в оригинале, задержка для существ не действует (таблица с ключами Long проверялась по Integer) — DECISIONS 98.
 */
public class EntityFocusCloud extends Entity implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(EntityFocusCloud.class, EntityDataSerializers.FLOAT);
    /** Задержка повторного действия по блоку (позиция → время). Только сервер; очищается при остановке. */
    static final Map<Long, Long> cooldownMap = new HashMap<>();
    FocusPackage focusPackage;
    private LivingEntity owner;
    private UUID ownerUniqueId;
    private int duration;
    FocusEffect[] effects = null;

    public EntityFocusCloud(EntityType<? extends EntityFocusCloud> type, Level level) {
        super(type, level);
    }

    public EntityFocusCloud(FocusPackage pack, Trajectory trajectory, float rad, int dur) {
        this(ConfigEntities.FOCUS_CLOUD.get(), pack.world);
        this.focusPackage = pack;
        this.setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        this.setOwner(pack.getCaster());
        this.setRadius(rad);
        this.setDuration(dur);
    }

    public static void clearServer() {
        cooldownMap.clear();
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int durationIn) {
        this.duration = durationIn;
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(RADIUS, 0.5F);
    }

    public void setRadius(float radiusIn) {
        if (!this.level().isClientSide) {
            this.entityData.set(RADIUS, radiusIn);
        }
        this.refreshDimensions();
    }

    public float getRadius() {
        return this.entityData.get(RADIUS);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (RADIUS.equals(key)) {
            this.refreshDimensions();
        }
    }

    /** setSize(радиус · 2, 0.5) оригинала. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
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
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("Age", this.tickCount);
        nbt.putInt("Duration", this.duration);
        nbt.putFloat("Radius", this.getRadius());
        if (this.ownerUniqueId != null) {
            nbt.putUUID("OwnerUUID", this.ownerUniqueId);
        }
        if (this.focusPackage != null) {
            nbt.put("pack", this.focusPackage.serialize());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.tickCount = nbt.getInt("Age");
        this.duration = nbt.getInt("Duration");
        this.setRadius(nbt.getFloat("Radius"));
        this.ownerUniqueId = nbt.hasUUID("OwnerUUID") ? nbt.getUUID("OwnerUUID") : null;
        try {
            this.focusPackage = new FocusPackage();
            this.focusPackage.deserialize(nbt.getCompound("pack"));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void tick() {
        super.tick();
        float rad = this.getRadius();
        int dur = this.getDuration();
        if (!this.level().isClientSide && (this.tickCount > dur * 20 || this.getOwner() == null)) {
            this.discard();
        }
        if (!this.isAlive()) {
            return;
        }
        var rand = this.random;
        if (this.level().isClientSide) {
            if (this.effects == null && this.focusPackage != null) {
                this.effects = this.focusPackage.getFocusEffects();
            }
            if (this.effects != null && this.effects.length > 0) {
                for (int a = 0; a < rad; a++) {
                    FocusEffect eff = this.effects[rand.nextInt(this.effects.length)];
                    IFXDispatcher.get().drawFocusCloudParticle(this.getX() + rand.nextGaussian() * rad / 2.0 * 0.85,
                            this.getY() + rand.nextGaussian() * rad / 2.0 * 0.85, this.getZ() + rand.nextGaussian() * rad / 2.0 * 0.85,
                            rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, rand.nextGaussian() * 0.01, FocusEngine.getElementColor(eff.getKey()));
                    eff.renderParticleFX(this.level(), this.getX() + rand.nextGaussian() * rad / 2.0, this.getY() + rand.nextGaussian() * rad / 2.0,
                            this.getZ() + rand.nextGaussian() * rad / 2.0, rand.nextGaussian() * 0.01F, rand.nextGaussian() * 0.01F,
                            rand.nextGaussian() * 0.01F);
                }
            }
        } else if (this.tickCount % 5 == 0 && this.focusPackage != null) {
            long t = System.currentTimeMillis();
            final ArrayList<Trajectory> trajectories = new ArrayList<>();
            final ArrayList<HitResult> targets = new ArrayList<>();
            for (Entity e : EntityUtils.getEntitiesInRange(this.level(), this.getX(), this.getY(), this.getZ(), this, Entity.class, rad)) {
                if (e.isRemoved()) {
                    continue;
                }
                if (e instanceof EntityFocusCloud other) {
                    Vec3 v = e.position().subtract(this.position());
                    e.move(MoverType.SELF, new Vec3(v.x / 50.0, v.y / 50.0, v.z / 50.0));
                    other.moveTowardsClosestSpace(this.getX(), this.getY(), this.getZ());
                }
                if (e instanceof LivingEntity) {
                    // в оригинале cooldownMap.containsKey(Integer) у таблицы с ключами Long всегда false — задержки для существ нет
                    cooldownMap.put((long) e.getId(), t + 2000L);
                    Vec3 hitVec = e.position().add(0.0, e.getBbHeight() / 2.0F, 0.0);
                    targets.add(new EntityHitResult(e, hitVec));
                    trajectories.add(new Trajectory(this.position(), hitVec.subtract(this.position())));
                }
            }
            for (int a = 0; a < rad; a++) {
                Vec3 dV = new Vec3(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian()).normalize();
                BlockHitResult br = EntityUtils.rayTraceBlocks(this.level(), this.position(), this.position().add(dV.scale(rad)));
                long bl = 0L;
                if (br != null) {
                    bl = br.getBlockPos().asLong();
                    Long until = cooldownMap.get(bl);
                    if (until != null) {
                        if (until <= t) {
                            cooldownMap.remove(bl);
                        } else {
                            br = null;
                        }
                    }
                }
                if (br != null) {
                    targets.add(br);
                    trajectories.add(new Trajectory(this.position(), dV));
                    cooldownMap.put(bl, t + 2000L);
                }
            }
            if (!targets.isEmpty()) {
                final FocusPackage pack = this.focusPackage;
                final LivingEntity caster = this.getOwner();
                ServerEvents.addRunnableServer(this.level(), () -> {
                    if (caster != null) {
                        FocusEngine.runFocusPackage(pack.copy(caster), trajectories.toArray(new Trajectory[0]), targets.toArray(new HitResult[0]));
                    }
                }, 0);
            }
        }
    }
}
