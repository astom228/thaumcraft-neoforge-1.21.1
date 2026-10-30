package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Неумирающий (порт ChampionModUndying): лечится на 1 раз в секунду. */
public class ChampionModUndying implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss.tickCount % 20 == 0) {
            boss.heal(1.0F);
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!rand.nextBoolean()) {
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.03, 0.0, 0.1F + rand.nextFloat() * 0.1F, 0.8F + rand.nextFloat() * 0.2F, 0.1F + rand.nextFloat() * 0.1F, 0.9F, true, 69, 4, 1, 4 + rand.nextInt(4), 0.5F + rand.nextFloat() * 0.2F, 0.0F, 0);
        }
    }
}
