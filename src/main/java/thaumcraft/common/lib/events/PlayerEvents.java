package thaumcraft.common.lib.events;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/**
 * События игрока (порт PlayerEvents оригинала; остальные обработчики добавляются на своих этапах).
 * attachCapabilitiesPlayer и cloneCapabilitiesEvent заменены Data Attachments с copyOnDeath (ThaumcraftAttachments).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class PlayerEvents {
    /** playerJoin: при появлении игрока в мире (вход, возрождение, смена измерения) — синхронизация знаний и Искажения. */
    @SubscribeEvent
    public static void playerJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ServerPlayer player) {
            ThaumcraftCapabilities.getKnowledge(player).sync(player);
            ThaumcraftCapabilities.getWarp(player).sync(player);
        }
    }
}
