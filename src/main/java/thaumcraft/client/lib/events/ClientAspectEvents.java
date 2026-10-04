package thaumcraft.client.lib.events;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.lib.events.AspectRegistryEvents;

/** Клиент: таблица аспектов строится после получения рецептов от сервера (теги к этому моменту уже получены). */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ClientAspectEvents {
    @SubscribeEvent
    public static void onRecipesUpdated(RecipesUpdatedEvent event) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            AspectRegistryEvents.build(LogicalSide.CLIENT, event.getRecipeManager(), connection.registryAccess());
        }
    }
}
