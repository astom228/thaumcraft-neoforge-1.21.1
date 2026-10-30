package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageTypes;

/** Шипастый (порт ChampionModSpined): ударивший получает 1–3 урона шипами. */
public class ChampionModSpined implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (target != null && !source.is(DamageTypes.THORNS)) {
            target.hurt(boss.damageSources().thorns(boss), 1 + boss.level().random.nextInt(3));
            target.playSound(SoundEvents.THORNS_HIT, 0.5F, 1.0F);
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!rand.nextBoolean()) {
            int p = 704 + rand.nextInt(4) * 3;
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.0, 0.0, 0.5F + rand.nextFloat() * 0.2F, 0.1F + rand.nextFloat() * 0.2F, 0.1F + rand.nextFloat() * 0.2F, 0.7F, false, p, 3, 1, 3, 1.2F + rand.nextFloat() * 0.3F, 0.0F, 0);
        }
    }
}
