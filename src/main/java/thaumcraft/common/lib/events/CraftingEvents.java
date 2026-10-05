package thaumcraft.common.lib.events;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.capabilities.IPlayerWarp;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * Порт CraftingEvents оригинала: Искажение за создание предмета и отметка «создан предмет» для требований исследований.
 * Топливо (getBurnTime) — в свойствах предметов; ярлык с фиалом — с ярлыками (этап 9).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class CraftingEvents {
    @SubscribeEvent
    public static void onCrafting(PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        int warp = ThaumcraftApi.getWarp(event.getCrafting());
        if (!ModConfig.CONFIG_MISC.wussMode.get() && warp > 0) {
            ThaumcraftApi.internalMethods.addWarpToPlayer(event.getEntity(), warp, IPlayerWarp.EnumWarpType.NORMAL);
        }
        int stackHash = ResearchManager.createItemStackHash(event.getCrafting().copy());
        if (ResearchManager.craftingReferences.contains(stackHash)) {
            ResearchManager.completeResearch(event.getEntity(), "[#]" + stackHash);
        }
    }
}
