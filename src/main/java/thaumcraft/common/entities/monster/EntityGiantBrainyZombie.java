package thaumcraft.common.entities.monster;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.level.Level;

/**
 * Яростный зомби (порт EntityGiantBrainyZombie): от ударов злится (до 2) и растёт (×(1 + злость)), удар 7 + 5·(злость − 1);
 * злость медленно спадает. Прыгает на цель. 60 здоровья.
 */
public class EntityGiantBrainyZombie extends EntityBrainyZombie {
    private static final EntityDataAccessor<Float> ANGER = SynchedEntityData.defineId(EntityGiantBrainyZombie.class, EntityDataSerializers.FLOAT);

    public EntityGiantBrainyZombie(EntityType<? extends EntityGiantBrainyZombie> type, Level world) {
        super(type, world);
        this.xpReward = 15;
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.4F));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityBrainyZombie.createAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ATTACK_DAMAGE, 7.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ANGER, 0.0F);
    }

    public float getAnger() {
        return this.entityData.get(ANGER);
    }

    public void setAnger(float a) {
        this.entityData.set(ANGER, a);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ANGER.equals(key)) {
            this.refreshDimensions();
        }
    }

    /** setSize(0.6 + злость·0.6, 1.95 + злость·1.95) оригинала (пока злость > 1). */
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        float a = this.getAnger();
        return a > 1.0F ? super.getDefaultDimensions(pose).scale(1.0F + a) : super.getDefaultDimensions(pose);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.getAnger() > 1.0F) {
            this.setAnger(this.getAnger() - 0.002F);
        }
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7.0F + (this.getAnger() - 1.0F) * 5.0F);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        this.setAnger(Math.min(2.0F, this.getAnger() + 0.1F));
        return super.hurt(src, amount);
    }
}
