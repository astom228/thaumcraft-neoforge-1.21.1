package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Пустой модификатор (порт ChampionModDummy). */
public class ChampionModDummy implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {

    }
}
