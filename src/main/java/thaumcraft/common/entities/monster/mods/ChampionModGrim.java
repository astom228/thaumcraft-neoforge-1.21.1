package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Мрачный (порт ChampionModGrim): иссушение с шансом 40%; модель темнее. */
public class ChampionModGrim implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss.level().random.nextFloat() < 0.4F) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200));
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!rand.nextBoolean()) {
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, -0.02, 0.0, rand.nextFloat() * 0.2F, rand.nextFloat() * 0.2F, rand.nextFloat() * 0.2F, 0.8F, false, 640, 10, 1, 8 + rand.nextInt(4), 0.6F + rand.nextFloat() * 0.4F, 0.0F, 0);
        }
    }

    @Override
    public int getTint() {
        return 0x999999;
    }
}
