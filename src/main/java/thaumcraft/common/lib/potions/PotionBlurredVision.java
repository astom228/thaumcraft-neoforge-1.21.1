package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Размытое зрение (порт PotionBlurredVision): сам ничего не делает (пост-шейдер размытия оригинала не загружался — файла в моде не было). */
public class PotionBlurredVision extends MobEffect {
    public PotionBlurredVision() {
        super(MobEffectCategory.HARMFUL, 8421504);
    }
}
