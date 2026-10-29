package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Отвергнутый солнцем (порт PotionSunScorned): раз в 2 с на ярком свету под открытым небом может поджечь (на 4 с),
 * в темноте — подлечить на 1.
 */
public class PotionSunScorned extends MobEffect {
    public PotionSunScorned() {
        super(MobEffectCategory.HARMFUL, 16308330);
    }

    @Override
    public boolean applyEffectTick(LivingEntity target, int amp) {
        if (!target.level().isClientSide) {
            float f = target.getLightLevelDependentMagicValue();
            if (f > 0.5F && target.getRandom().nextFloat() * 30.0F < (f - 0.4F) * 2.0F && target.level().canSeeSky(target.blockPosition())) {
                target.igniteForSeconds(4.0F);
            } else if (f < 0.25F && target.getRandom().nextFloat() > f * 2.0F) {
                target.heal(1.0F);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
        return duration % 40 == 0;
    }
}
