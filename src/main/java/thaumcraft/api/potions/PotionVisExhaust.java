package thaumcraft.api.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** «Лихорадка Выброса» (порт PotionVisExhaust): сам по себе ничего не делает; его проверяют другие системы (кастеры — этап 12). */
public class PotionVisExhaust extends MobEffect {

    public PotionVisExhaust() {
        super(MobEffectCategory.HARMFUL, 6702199);
    }
}
