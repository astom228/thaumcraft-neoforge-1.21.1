package thaumcraft.client.lib.events;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.misc.PacketMiscEvent;

/**
 * Клиентская часть PacketMiscEvent и «тумана» Искажения (RenderEventHandler.fogFiddled оригинала): плотность тумана плавно
 * растёт до 0.1 и спадает за последние 100 тиков. В 1.12 туман был экспоненциальным (GL_EXP, плотность fogTarget); в 1.21
 * туман только линейный — дальность видимости берётся 3 / плотность (там экспоненциальный туман гасит ~95% цвета).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class WarpFog {
    private static boolean fogFiddled = false;
    private static float fogTarget = 0.0F;
    private static int fogDuration = 0;

    private WarpFog() {
    }

    public static void handle(PacketMiscEvent msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        switch (msg.kind()) {
            case PacketMiscEvent.WARP_EVENT -> {
                if (!ModConfig.CONFIG_GRAPHICS.nostress.get()) {
                    mc.player.level().playLocalSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundsTC.heartbeat.get(), SoundSource.AMBIENT,
                            1.0F, 1.0F, false);
                }
            }
            case PacketMiscEvent.MIST_EVENT -> {
                fogFiddled = true;
                fogDuration = 2400;
            }
            case PacketMiscEvent.MIST_EVENT_SHORT -> {
                fogFiddled = true;
                if (fogDuration < 200) {
                    fogDuration = 200;
                }
            }
            default -> {
            }
        }
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused() || !fogFiddled) {
            return;
        }
        if (fogDuration < 100) {
            fogTarget = 0.1F * (fogDuration / 100.0F);
        } else if (fogTarget < 0.1F) {
            fogTarget += 0.001F;
        }
        fogDuration--;
        if (fogDuration < 0) {
            fogFiddled = false;
        }
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog event) {
        if (fogFiddled && fogTarget > 0.0F) {
            float far = Math.min(event.getFarPlaneDistance(), 3.0F / fogTarget);
            event.setNearPlaneDistance(0.0F);
            event.setFarPlaneDistance(far);
            event.setCanceled(true);
        }
    }
}
