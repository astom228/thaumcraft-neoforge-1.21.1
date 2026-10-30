package thaumcraft.common.entities.projectile;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import thaumcraft.common.config.ConfigEntities;

/**
 * Потусторонняя сфера (порт EntityEldritchOrb): летит без гравитации до 5 с; при попадании — магический урон (⅔ удара
 * стрелка) и слабость всем живым в радиусе 2, кроме нежити и стрелка.
 */
public class EntityEldritchOrb extends ThrowableProjectile {
    public EntityEldritchOrb(EntityType<? extends EntityEldritchOrb> type, Level world) {
        super(type, world);
    }

    public EntityEldritchOrb(Level world, LivingEntity shooter) {
        super(ConfigEntities.ELDRITCH_ORB.get(), shooter, world);
        this.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot(), -5.0F, 0.75F, 0.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 100) {
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        Entity owner = this.getOwner();
        if (!this.level().isClientSide && owner instanceof LivingEntity shooter) {
            for (Entity e : this.level().getEntities(shooter, this.getBoundingBox().inflate(2.0))) {
                if (e instanceof LivingEntity le && !le.isInvertedHealAndHarm()) {
                    le.hurt(this.damageSources().indirectMagic(this, shooter), (float) shooter.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.666F);
                    le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
                }
            }
            this.playSound(SoundEvents.LAVA_EXTINGUISH, 0.5F, 2.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.8F);
            this.discard();
        }
    }
}
