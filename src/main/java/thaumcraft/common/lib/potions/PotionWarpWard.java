package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** «Защита от Искажения» (порт PotionWarpWard): сама ничего не делает; её проверяют события Искажения (этап 15) и очищающая жидкость. */
public class PotionWarpWard extends MobEffect {
    public PotionWarpWard() {
        super(MobEffectCategory.BENEFICIAL, 14742263);
    }
}
