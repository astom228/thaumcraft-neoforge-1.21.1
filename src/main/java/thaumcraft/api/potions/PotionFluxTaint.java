package thaumcraft.api.potions;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.damagesource.DamageSourceThaumcraft;

/**
 * «Отравление скверной» (порт PotionFluxTaint): раз в 40 &gt;&gt; уровень тиков — 1 урона скверной (мимо брони) всем, кроме нежити.
 * TODO этапы 15–16: заражённые существа (ITaintedMob) и чемпионы-«заражённые» (CHAMPION_MOD = 13) вместо урона лечатся.
 */
public class PotionFluxTaint extends MobEffect {

    public PotionFluxTaint() {
        super(MobEffectCategory.HARMFUL, 6697847);
    }

    @Override
    public boolean applyEffectTick(LivingEntity target, int strength) {
        if (!target.isInvertedHealAndHarm() && !(target instanceof Player)) {
            target.hurt(DamageSourceThaumcraft.taint(target.level()), 1.0F);
        } else if (!target.isInvertedHealAndHarm() && (target.getMaxHealth() > 1.0F || target instanceof Player)) {
            target.hurt(DamageSourceThaumcraft.taint(target.level()), 1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        int k = 40 >> amplifier;
        return k <= 0 || duration % k == 0;
    }
}
