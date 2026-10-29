package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Неестественный голод (порт PotionUnnaturalHunger): каждый тик истощает игрока (0.025 × уровень). Обычная еда не помогает,
 * гнилая плоть и мозг зомби ослабляют (PlayerEvents.finishedUsingItem).
 */
public class PotionUnnaturalHunger extends MobEffect {
    public PotionUnnaturalHunger() {
        super(MobEffectCategory.HARMFUL, 4482611);
    }

    @Override
    public boolean applyEffectTick(LivingEntity target, int amp) {
        if (!target.level().isClientSide && target instanceof Player p) {
            p.causeFoodExhaustion(0.025F * (amp + 1));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
        return true;
    }
}
