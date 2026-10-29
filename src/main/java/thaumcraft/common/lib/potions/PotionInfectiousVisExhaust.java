package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.common.config.ConfigPotions;

/**
 * Выбросный фаг (порт PotionInfectiousVisExhaust): раз в 2 с заражает всех в радиусе 4 — уровнем ниже, а с последнего уровня
 * передаёт обычную «Лихорадку Выброса». Штраф к скидке Вис — как у лихорадки (CasterManager).
 */
public class PotionInfectiousVisExhaust extends MobEffect {
    public PotionInfectiousVisExhaust() {
        super(MobEffectCategory.HARMFUL, 6706551);
    }

    @Override
    public boolean applyEffectTick(LivingEntity target, int amp) {
        if (target.level().isClientSide) {
            return true;
        }
        for (LivingEntity e : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4.0))) {
            if (!e.hasEffect(ConfigPotions.INFECTIOUS_VIS_EXHAUST)) {
                if (amp > 0) {
                    e.addEffect(new MobEffectInstance(ConfigPotions.INFECTIOUS_VIS_EXHAUST, 6000, amp - 1, false, true));
                } else {
                    e.addEffect(new MobEffectInstance(ConfigPotions.VIS_EXHAUST, 6000, 0, false, true));
                }
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
        return duration % 40 == 0;
    }
}
