package thaumcraft.client.lib.events;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.common.config.ConfigPotions;

/**
 * Тёмная виньетка Искажения (ShaderHandler.warpVignette + RenderEventHandler.renderVignette оригинала): пока у игрока
 * «Смертоносный взгляд», края экрана плавно темнеют (с лёгкой пульсацией), потом так же светлеют. Пост-шейдеры эффектов
 * (desaturatetc, blurtc, hunger, sunscorned) в оригинале не загружались — файлов не было, — поэтому не переносятся.
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public final class WarpVignette {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/vignette.png");
    private static int warpVignette = 0;
    private static float targetBrightness = 1.0F;
    private static float prevVignetteBrightness = 0.0F;

    private WarpVignette() {
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.isPaused()) {
            return;
        }
        if (mc.player.hasEffect(ConfigPotions.DEATH_GAZE)) {
            warpVignette = 10;
        }
        if (warpVignette > 0) {
            warpVignette--;
            targetBrightness = 0.0F;
        } else {
            targetBrightness = 1.0F;
        }
    }

    /** Слой интерфейса: затемнение текстурой vignette (смешивание ZERO, ONE_MINUS_SRC_COLOR), сглаживание 1% за кадр. */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float brightness = 1.0F - targetBrightness;
        prevVignetteBrightness = prevVignetteBrightness + (brightness - prevVignetteBrightness) * 0.01F;
        if (prevVignetteBrightness > 0.0F) {
            float b = prevVignetteBrightness * (1.0F + Mth.sin(mc.player.tickCount / 2.0F) * 0.1F);
            int w = g.guiWidth();
            int h = g.guiHeight();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            g.setColor(b, b, b, 1.0F);
            g.blit(TEX, 0, 0, -90, 0.0F, 0.0F, w, h, w, h);
            g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }
}
