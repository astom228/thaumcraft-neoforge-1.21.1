package thaumcraft.common.entities.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.api.entities.ITaintedMob;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.lib.fx.IFXDispatcher;

/**
 * Брошенная бутылка порчи (порт EntityBottleTaint): разбивается, отравляет скверной всех живых в радиусе 5 (кроме порчи
 * и нежити) и разбрызгивает флюкс-слизь на опоры вокруг. Брызги — событие сущности 3.
 */
public class EntityBottleTaint extends ThrowableItemProjectile {
    public EntityBottleTaint(EntityType<? extends EntityBottleTaint> type, Level level) {
        super(type, level);
    }

    public EntityBottleTaint(Level level, LivingEntity thrower) {
        super(ConfigEntities.BOTTLE_TAINT.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ItemsTC.bottleTaint.get();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            for (int a = 0; a < 100; a++) {
                IFXDispatcher.get().taintsplosionFX(this);
            }
            IFXDispatcher.get().bottleTaintBreak(this.getX(), this.getY(), this.getZ());
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void onHit(HitResult ray) {
        super.onHit(ray);
        Level w = this.level();
        if (w.isClientSide) {
            return;
        }
        for (LivingEntity el : w.getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(5.0))) {
            if (!(el instanceof ITaintedMob) && !el.isInvertedHealAndHarm()) {
                el.addEffect(new MobEffectInstance(ConfigPotions.FLUX_TAINT, 100, 0, false, true));
            }
        }
        for (int a = 0; a < 10; a++) {
            int xx = (int) ((this.random.nextFloat() - this.random.nextFloat()) * 4.0F);
            int zz = (int) ((this.random.nextFloat() - this.random.nextFloat()) * 4.0F);
            BlockPos p = this.blockPosition().offset(xx, 0, zz);
            if (w.random.nextBoolean()) {
                if (this.canGoo(p)) {
                    w.setBlockAndUpdate(p, BlocksTC.fluxGoo.get().defaultBlockState());
                } else {
                    p = p.below();
                    if (this.canGoo(p)) {
                        w.setBlockAndUpdate(p, BlocksTC.fluxGoo.get().defaultBlockState());
                    }
                }
            }
        }
        w.broadcastEntityEvent(this, (byte) 3);
        this.discard();
    }

    /** isBlockNormalCube(ниже) и isReplaceable(клетки) оригинала. */
    private boolean canGoo(BlockPos p) {
        Level w = this.level();
        return w.isLoaded(p) && w.getBlockState(p.below()).isRedstoneConductor(w, p.below()) && w.getBlockState(p).canBeReplaced();
    }
}
