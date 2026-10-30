package thaumcraft.common.entities.ai.misc;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;

/**
 * Ритуал у алтаря (порт AIAltarFocus): клирик-ритуалист стоит у своего дома (алтаря) и ничего больше не делает; раз в
 * 2 с бросает ритуал, если ушёл дальше 4 блоков. Проверку «дом — потусторонний алтарь» (BlocksTC.eldritch) оригинал делал
 * с незарегистрированным в BETA17 блоком (null) — ритуал всегда прерывался; здесь остаётся только расстояние (DECISIONS 135).
 */
public class AIAltarFocus extends Goal {
    private final EntityCultistCleric entity;

    public AIAltarFocus(EntityCultistCleric entity) {
        this.entity = entity;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.entity.getIsRitualist() && this.entity.hasRestriction();
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse();
    }

    @Override
    public void tick() {
        if (this.entity.hasRestriction() && this.entity.tickCount % 40 == 0
                && this.entity.getRestrictCenter().distToCenterSqr(this.entity.getX(), this.entity.getY(), this.entity.getZ()) > 16.0) {
            this.entity.setIsRitualist(false);
        }
    }
}
