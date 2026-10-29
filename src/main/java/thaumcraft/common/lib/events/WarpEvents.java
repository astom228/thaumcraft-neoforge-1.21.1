package thaumcraft.common.lib.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import thaumcraft.common.config.ConfigPotions;
import thaumcraft.common.lib.utils.EntityUtils;

/** События Искажения (порт WarpEvents). Сервер. */
public class WarpEvents {
    /**
     * Смертоносный взгляд: всех живых, на кого игрок смотрит (конус 0.75, дальность 8 + 3·уровень, не больше 24) и кого видит,
     * иссушает на 4 с и делает враждебными к игроку. Игроков — только при включённом PvP; уже иссушённых — пропускает.
     */
    public static void checkDeathGaze(ServerPlayer player) {
        MobEffectInstance pe = player.getEffect(ConfigPotions.DEATH_GAZE);
        if (pe == null) {
            return;
        }
        int range = Math.min(8 + pe.getAmplifier() * 3, 24);
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), e -> e != player)) {
            if (entity.isPickable() && entity.isAlive() && EntityUtils.isVisibleTo(0.75F, player, entity, range) && player.hasLineOfSight(entity)
                    && (!(entity instanceof Player) || player.server.isPvpAllowed()) && !entity.hasEffect(MobEffects.WITHER)) {
                entity.setLastHurtByMob(player);
                player.setLastHurtMob(entity);
                if (entity instanceof Mob mob) {
                    mob.setTarget(player);
                }
                entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 80));
            }
        }
    }
}
