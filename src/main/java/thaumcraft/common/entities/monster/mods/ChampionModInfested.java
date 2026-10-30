package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.common.config.ConfigEntities;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.fx.IFXDispatcher;

/** Кишащий (порт ChampionModInfested): при ударе по нему с шансом 40% выпускает заражённого ползуна. */
public class ChampionModInfested implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss.level().random.nextFloat() < 0.4F && !boss.level().isClientSide) {
            var spiderling = ConfigEntities.TAINT_CRAWLER.get().create(boss.level());
            if (spiderling != null) {
                spiderling.moveTo(boss.getX(), boss.getY() + boss.getBbHeight() / 2.0F, boss.getZ(), boss.level().random.nextFloat() * 360.0F, 0.0F);
                boss.level().addFreshEntity(spiderling);
                boss.playSound(SoundsTC.gore.get(), 0.5F, 1.0F);
            }
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        if (boss.level().random.nextBoolean()) {
            IFXDispatcher.get().slimeJumpFX(boss, 0);
        }
    }
}
