package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Вампирский (порт ChampionModVampire): лечится на половину нанесённого урона (не меньше 2); модель краснее. */
public class ChampionModVampire implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        boss.heal(Math.max(2.0F, amount / 2.0F));
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!(rand.nextFloat() > 0.2F)) {
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.0, 0.0, 0.9F + rand.nextFloat() * 0.1F, 0.0F, 0.0F, 0.9F, false, 579, 4, 1, 4 + rand.nextInt(4), 0.5F + rand.nextFloat() * 0.2F, 0.0F, 0);
        }
    }

    @Override
    public int getTint() {
        return 0xFFB2B2;
    }
}
