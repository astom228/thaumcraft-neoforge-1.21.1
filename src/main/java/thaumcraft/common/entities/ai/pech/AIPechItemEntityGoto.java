package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import thaumcraft.common.entities.monster.EntityPech;

/**
 * Пех идёт к ближайшему предмету в 16 блоках, который может подобрать (кроме выброшенных из окна торговли), и
 * забирает его, подойдя вплотную (порт AIPechItemEntityGoto).
 */
public class AIPechItemEntityGoto extends Goal {
    private final EntityPech pech;
    private ItemEntity targetEntity;
    private final float maxTargetDistance = 16.0F;
    private int count;
    private int failedPathFindingPenalty;

    public AIPechItemEntityGoto(EntityPech pech) {
        this.pech = pech;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (--this.count > 0) {
            return false;
        }
        double range = Double.MAX_VALUE;
        this.targetEntity = null;
        for (ItemEntity e : this.pech.level().getEntitiesOfClass(ItemEntity.class, this.pech.getBoundingBox().inflate(this.maxTargetDistance))) {
            if (this.pech.canPickup(e.getItem()) && !e.getPersistentData().getBoolean(EntityPech.PECH_DROP)) {
                double distance = e.distanceToSqr(this.pech);
                if (distance < range && distance <= this.maxTargetDistance * this.maxTargetDistance) {
                    range = distance;
                    this.targetEntity = e;
                }
            }
        }
        return this.targetEntity != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.targetEntity != null && this.targetEntity.isAlive() && !this.pech.getNavigation().isDone()
                && this.targetEntity.distanceToSqr(this.pech) < this.maxTargetDistance * this.maxTargetDistance;
    }

    @Override
    public void stop() {
        this.targetEntity = null;
    }

    private double speed() {
        return this.pech.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.5;
    }

    @Override
    public void start() {
        this.pech.getNavigation().moveTo(this.targetEntity, this.speed());
        this.count = 0;
    }

    @Override
    public void tick() {
        this.pech.getLookControl().setLookAt(this.targetEntity, 30.0F, 30.0F);
        if (this.pech.getSensing().hasLineOfSight(this.targetEntity) && --this.count <= 0) {
            this.count = this.failedPathFindingPenalty + 4 + this.pech.getRandom().nextInt(4);
            this.pech.getNavigation().moveTo(this.targetEntity, this.speed());
            Path path = this.pech.getNavigation().getPath();
            Node end = path != null ? path.getEndNode() : null;
            if (end != null && this.targetEntity.distanceToSqr(end.x, end.y, end.z) < 1.0) {
                this.failedPathFindingPenalty = 0;
            } else {
                this.failedPathFindingPenalty += 10;
            }
        }
        double distance = this.pech.distanceToSqr(this.targetEntity.getX(), this.targetEntity.getBoundingBox().minY, this.targetEntity.getZ());
        if (distance <= 1.5) {
            this.count = 0;
            int am = this.targetEntity.getItem().getCount();
            ItemStack is = this.pech.pickupItem(this.targetEntity.getItem().copy());
            if (!is.isEmpty()) {
                this.targetEntity.setItem(is);
            } else {
                this.targetEntity.discard();
            }
            if (is.isEmpty() || is.getCount() != am) {
                this.targetEntity.level().playSound(null, this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), SoundEvents.ITEM_PICKUP,
                        SoundSource.NEUTRAL, 0.2F, ((this.pech.getRandom().nextFloat() - this.pech.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F);
            }
        }
    }
}
