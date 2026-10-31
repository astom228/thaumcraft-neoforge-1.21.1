package thaumcraft.common.entities.construct.golem.ai;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import thaumcraft.common.entities.construct.EntityOwnedConstruct;

/** Следование за хозяином (порт AIFollowOwner): отстав на 12+ блоков без пути — телепорт к хозяину. */
public class AIFollowOwner extends Goal {
    private final EntityOwnedConstruct thePet;
    private LivingEntity theOwner;
    private final Level theWorld;
    private final double followSpeed;
    private int timeToRecalcPath;
    private final float maxDist;
    private final float minDist;
    private float oldWaterCost;

    public AIFollowOwner(EntityOwnedConstruct pet, double followSpeed, float minDist, float maxDist) {
        this.thePet = pet;
        this.theWorld = pet.level();
        this.followSpeed = followSpeed;
        this.minDist = minDist;
        this.maxDist = maxDist;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity owner = this.thePet.getOwnerEntity();
        if (owner == null || this.thePet.distanceToSqr(owner) < this.minDist * this.minDist) {
            return false;
        }
        this.theOwner = owner;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.thePet.getNavigation().isDone() && this.thePet.distanceToSqr(this.theOwner) > this.maxDist * this.maxDist;
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.thePet.getPathfindingMalus(PathType.WATER);
        this.thePet.setPathfindingMalus(PathType.WATER, 0.0F);
    }

    @Override
    public void stop() {
        this.theOwner = null;
        this.thePet.getNavigation().stop();
        this.thePet.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
    }

    private boolean isPassable(BlockPos pos) {
        BlockState state = this.theWorld.getBlockState(pos);
        return state.isAir() || !state.isCollisionShapeFullBlock(this.theWorld, pos);
    }

    @Override
    public void tick() {
        this.thePet.getLookControl().setLookAt(this.theOwner, 10.0F, this.thePet.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.thePet.getNavigation().moveTo(this.theOwner, this.followSpeed) && !this.thePet.isLeashed()
                    && this.thePet.distanceToSqr(this.theOwner) >= 144.0) {
                int i = (int) Math.floor(this.theOwner.getX()) - 2;
                int j = (int) Math.floor(this.theOwner.getZ()) - 2;
                int k = (int) Math.floor(this.theOwner.getBoundingBox().minY);
                for (int l = 0; l <= 4; l++) {
                    for (int i1 = 0; i1 <= 4; i1++) {
                        BlockPos below = new BlockPos(i + l, k - 1, j + i1);
                        if ((l < 1 || i1 < 1 || l > 3 || i1 > 3) && this.theWorld.getBlockState(below).isCollisionShapeFullBlock(this.theWorld, below)
                                && this.isPassable(new BlockPos(i + l, k, j + i1)) && this.isPassable(new BlockPos(i + l, k + 1, j + i1))) {
                            this.thePet.moveTo(i + l + 0.5F, k, j + i1 + 0.5F, this.thePet.getYRot(), this.thePet.getXRot());
                            this.thePet.getNavigation().stop();
                            return;
                        }
                    }
                }
            }
        }
    }
}
