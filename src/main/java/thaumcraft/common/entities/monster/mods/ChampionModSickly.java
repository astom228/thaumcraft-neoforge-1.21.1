package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Хворый (порт ChampionModSickly): голод на 25 с с шансом 40%. */
public class ChampionModSickly implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss.level().random.nextFloat() < 0.4F) {
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 500));
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!rand.nextBoolean()) {
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, -0.02, 0.0, 0.2F, 0.6F + rand.nextFloat() * 0.1F, 0.2F + rand.nextFloat() * 0.1F, 0.5F, false, 1, 4, 2, 5 + rand.nextInt(4), 0.9F + rand.nextFloat() * 0.3F, 0.0F, 0);
        }
    }
}
