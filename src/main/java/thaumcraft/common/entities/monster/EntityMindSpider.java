package thaumcraft.common.entities.monster;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import thaumcraft.api.entities.IEldritchMob;

/**
 * Паук разума (порт EntityMindSpider): маленький призрачный паук из событий Искажения. «Ненастоящий» (harmless) виден
 * только своему зрителю, не кусает, живёт минуту и не даёт опыта. Без добычи.
 */
public class EntityMindSpider extends Spider implements IEldritchMob {
    private static final EntityDataAccessor<Boolean> HARMLESS = SynchedEntityData.defineId(EntityMindSpider.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> VIEWER = SynchedEntityData.defineId(EntityMindSpider.class, EntityDataSerializers.STRING);
    private int lifeSpan = Integer.MAX_VALUE;

    public EntityMindSpider(EntityType<? extends EntityMindSpider> type, Level world) {
        super(type, world);
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 1.0).add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HARMLESS, false).define(VIEWER, "");
    }

    public String getViewer() {
        return this.entityData.get(VIEWER);
    }

    public void setViewer(String player) {
        this.entityData.set(VIEWER, player);
    }

    public boolean isHarmless() {
        return this.entityData.get(HARMLESS);
    }

    public void setHarmless(boolean h) {
        if (h) {
            this.lifeSpan = 1200;
        }
        this.entityData.set(HARMLESS, h);
    }

    @Override
    protected int getBaseExperienceReward() {
        return this.isHarmless() ? 0 : super.getBaseExperienceReward();
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > this.lifeSpan) {
            this.discard();
        }
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        return !this.isHarmless() && super.doHurtTarget(target);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.setHarmless(nbt.getBoolean("harmless"));
        this.setViewer(nbt.getString("viewer"));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("harmless", this.isHarmless());
        nbt.putString("viewer", this.getViewer());
    }

    /** onInitialSpawn оригинала ничего не делает: без жокея-скелета и без случайных эффектов паука. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        return data;
    }
}
