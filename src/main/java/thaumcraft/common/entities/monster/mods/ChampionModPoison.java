package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Ядовитый (порт ChampionModPoison): отравление на 5 с с шансом 40%; модель зеленее. */
public class ChampionModPoison implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss.level().random.nextFloat() < 0.4F) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!rand.nextBoolean()) {
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.02, 0.0, 0.2F, 0.6F + rand.nextFloat() * 0.1F, 0.2F + rand.nextFloat() * 0.1F, 0.7F, false, 579, 4, 1, 8 + rand.nextInt(4), 0.5F + rand.nextFloat() * 0.2F, 0.5F, 0);
        }
    }

    @Override
    public int getTint() {
        return 0x99FFB2;
    }
}
