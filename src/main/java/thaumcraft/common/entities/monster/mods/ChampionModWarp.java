package thaumcraft.common.entities.monster.mods;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;

/** Искажённый (порт ChampionModWarp): с шансом 33% даёт игроку 1–3 временного Искажения. */
public class ChampionModWarp implements IChampionModifierEffect {
    @Override
    public float performEffect(LivingEntity boss, LivingEntity target, DamageSource source, float amount) {
        if (boss.level().random.nextFloat() < 0.33F && target instanceof ServerPlayer p) {
            ThaumcraftApi.internalMethods.addWarpToPlayer(p, 1 + boss.level().random.nextInt(3), IPlayerWarp.EnumWarpType.TEMPORARY);
        }
        return amount;
    }

    @Override
    public void showFX(LivingEntity boss) {
        var rand = boss.level().random;
        if (!rand.nextBoolean()) {
            IChampionModifierEffect.particle(boss, 1.0F, 0.0, 0.0, 0.0, 0.8F + rand.nextFloat() * 0.2F, 0.0F, 0.9F + rand.nextFloat() * 0.1F, 0.7F, true, 264, 8, 1, 10 + rand.nextInt(4), 0.6F + rand.nextFloat() * 0.4F, 0.0F, 0);
        }
    }
}
