package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Могучий (порт ChampionModMighty): сильнее бьёт (при появлении). */
public class ChampionModMighty implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        return 0.0F;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!(rand.nextFloat() > 0.3F)) {
            int p = 704 + rand.nextInt(4) * 3;
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.0, 0.0, 0.8F + rand.nextFloat() * 0.2F, 0.8F + rand.nextFloat() * 0.2F, 0.8F + rand.nextFloat() * 0.2F, 0.7F, false, p, 3, 1, 4 + rand.nextInt(3), 1.0F + rand.nextFloat() * 0.3F, 0.0F, 0);
        }
    }
}
