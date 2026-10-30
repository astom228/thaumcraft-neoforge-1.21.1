package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Огненный (порт ChampionModFire): поджигает на 4 с с шансом 40%; модель розовее. */
public class ChampionModFire implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss.level().random.nextFloat() < 0.4F) {
            target.igniteForSeconds(4.0F);
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.03, 0.0, 0.9F + rand.nextFloat() * 0.1F, 1.0F, 1.0F, 0.7F, false, 640, 10, 1, 8 + rand.nextInt(4), 0.7F + rand.nextFloat() * 0.2F, 0.0F, 0);
    }

    /** glColor4f(1, 0.75, 5) — синий зажимается до 1. */
    @Override
    public int getTint() {
        return 0xFFBFFF;
    }
}
