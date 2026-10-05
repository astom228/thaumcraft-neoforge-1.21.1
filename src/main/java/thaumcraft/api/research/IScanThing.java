package thaumcraft.api.research;

import net.minecraft.world.entity.player.Player;

/**
 * Что можно отсканировать таумометром (порт IScanThing). object — сущность, BlockPos, ItemStack или null (взгляд в небо).
 * getResearchKey: ключ, который получит игрок; null или "" — без ключа (onSuccess вызывается всегда, сообщение не выводится).
 */
public interface IScanThing {
    boolean checkThing(Player player, Object object);

    String getResearchKey(Player player, Object object);

    default void onSuccess(Player player, Object object) {
    }
}
