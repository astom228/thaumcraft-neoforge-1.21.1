package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.common.lib.fx.IFXDispatcher;

/** Смелый (порт ChampionModBold): быстрее (при появлении); оранжевые искры у ног. */
public class ChampionModBold implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        return 0.0F;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!rand.nextBoolean()) {
            float w = rand.nextFloat() * boss.getBbWidth();
            float d = rand.nextFloat() * boss.getBbWidth();
            float h = rand.nextFloat() * boss.getBbHeight() / 3.0F;
            var bb = boss.getBoundingBox();
            IFXDispatcher.get().spark(bb.minX + w, bb.minY + h, bb.minZ + d, 0.2F, 0.3F - rand.nextFloat() * 0.1F, 0.0F, 0.8F + rand.nextFloat() * 0.2F, 1.0F);
        }
    }
}
