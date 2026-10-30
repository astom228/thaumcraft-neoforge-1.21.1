package thaumcraft.common.entities.ai.pech;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.EntityPech;

/** Торговля (порт AIPechTradePlayer): пока у игрока открыто окно, приручённый пех стоит на месте. */
public class AIPechTradePlayer extends Goal {
    private final EntityPech pech;

    public AIPechTradePlayer(EntityPech pech) {
        this.pech = pech;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.pech.isAlive() && !this.pech.isInWater() && this.pech.isTamed() && this.pech.onGround() && !this.pech.hurtMarked && this.pech.trading;
    }

    @Override
    public void start() {
        this.pech.getNavigation().stop();
    }

    @Override
    public void stop() {
        this.pech.trading = false;
    }
}
