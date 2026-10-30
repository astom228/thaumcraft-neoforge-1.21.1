package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Защищённый (порт ChampionModWarded): копит поглощение до половины базового здоровья (+1 раз в 25 тиков). */
public class ChampionModWarded implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss.invulnerableTime <= 0 && boss.tickCount % 25 == 0) {
            int bh = (int) boss.getAttributeBaseValue(Attributes.MAX_HEALTH) / 2;
            if (boss.getAbsorptionAmount() < bh) {
                boss.setAbsorptionAmount(boss.getAbsorptionAmount() + 1.0F);
            }
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!rand.nextBoolean()) {
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.0, 0.0, 0.5F + rand.nextFloat() * 0.1F, 0.5F + rand.nextFloat() * 0.1F, 0.5F + rand.nextFloat() * 0.1F, 0.6F, true, 69, 4, 1, 4 + rand.nextInt(4), 0.8F + rand.nextFloat() * 0.3F, 0.0F, 0);
        }
    }
}
