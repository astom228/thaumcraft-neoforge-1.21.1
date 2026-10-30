package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.DamageTypeTags;

/** Бронированный (порт ChampionModArmored): урон, который не обходит броню, × 19/25. */
public class ChampionModArmored implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            amount = amount * 19.0F / 25.0F;
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (rand.nextInt(4) == 0) {
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.0, 0.0, 0.9F, 0.9F, 0.9F + rand.nextFloat() * 0.1F, 0.7F, false, 448, 9, 1, 5 + rand.nextInt(4), 0.6F + rand.nextFloat() * 0.2F, 0.0F, 0);
        }
    }
}
