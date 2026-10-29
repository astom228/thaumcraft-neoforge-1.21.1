package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Тауморея (порт PotionThaumarhia): раз в секунду с шансом 1/15 оставляет под собой флюкс-слизь (если там воздух). */
public class PotionThaumarhia extends MobEffect {
    public PotionThaumarhia() {
        super(MobEffectCategory.HARMFUL, 6702199);
    }

    @Override
    public boolean applyEffectTick(LivingEntity target, int amp) {
        if (!target.level().isClientSide && target.getRandom().nextInt(15) == 0 && target.level().isEmptyBlock(target.blockPosition())) {
            // TODO этап 15.3: флюкс-слизь (BlocksTC.fluxGoo)
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amp) {
        return duration % 20 == 0;
    }
}
