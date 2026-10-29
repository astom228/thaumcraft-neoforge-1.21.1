package thaumcraft.common.lib.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Смертоносный взгляд (порт PotionDeathGaze): действие — WarpEvents.checkDeathGaze раз в секунду (иссушение тех, на кого смотрит игрок), виньетка на экране. */
public class PotionDeathGaze extends MobEffect {
    public PotionDeathGaze() {
        super(MobEffectCategory.HARMFUL, 6702131);
    }
}
