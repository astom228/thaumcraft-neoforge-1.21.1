package thaumcraft.common.items.tools;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Общее для пустотного металла (в оригинале — одинаковый код в каждом из ItemVoid*): самопочинка 1 ед. в секунду и
 * «Малое иссушение» — слабость при ударе (игрокам — только при включённом PvP).
 */
public final class VoidMetalHelper {
    private VoidMetalHelper() {
    }

    /** onUpdate оригинала: раз в 20 тиков владельца −1 к повреждению (на сервере; клиент получает стек синхронизацией). */
    public static void repairTick(ItemStack stack, Level level, Entity entity) {
        if (!level.isClientSide && stack.isDamaged() && entity instanceof LivingEntity && entity.tickCount % 20 == 0) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    public static void weaken(Entity target, int duration) {
        if (target.level().isClientSide || !(target instanceof LivingEntity living)) {
            return;
        }
        if (target instanceof Player) {
            MinecraftServer server = target.level().getServer();
            if (server == null || !server.isPvpAllowed()) {
                return;
            }
        }
        living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration));
    }
}
