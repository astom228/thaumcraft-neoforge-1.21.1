package thaumcraft.client.lib.events;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.UtilsFX;

/**
 * Порт RenderEventHandler (части переносятся по этапам): тик эффектов HUD и отрисовка поверх кадра
 * (RenderTickEvent END оригинала → RenderFrameEvent.Post): HUD и частицы интерфейса.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class RenderEventHandler {
    @SubscribeEvent
    public static void clientWorldTick(ClientTickEvent.Post event) {
        HudHandler.tickKnowledgeGains();
    }

    @SubscribeEvent
    public static void renderTick(RenderFrameEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        float partialTicks = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        Window win = mc.getWindow();
        UtilsFX.beginFrameOverlay();
        if (mc.getCameraEntity() instanceof Player player) {
            HudHandler.renderHuds(mc, partialTicks, player, win.getGuiScaledWidth(), win.getGuiScaledHeight());
        }
        ParticleEngine.renderGuiParticles(partialTicks);
        UtilsFX.endFrameOverlay();
    }
}
