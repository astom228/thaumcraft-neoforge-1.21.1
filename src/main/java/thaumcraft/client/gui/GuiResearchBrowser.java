package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.client.lib.UtilsFX;

/**
 * Карта исследований Таумономикона (порт GuiResearchBrowser). Сам экран — подшаг 7.4; пока здесь общий для тостов
 * и книги метод отрисовки значка исследования.
 */
public class GuiResearchBrowser {
    /** Размеры текстур значков (в оригинале запрашивались у OpenGL на каждом кадре). */
    private static final Map<ResourceLocation, int[]> ICON_SIZES = new ConcurrentHashMap<>();

    /**
     * Значок исследования 16×16 (drawResearchIcon): значки сменяются раз в секунду; картинка с кадрами по высоте или ширине
     * анимируется (кадр 150 мс); bw — затемнённый значок. Значки фокусов — этап 12.
     */
    public static void drawResearchIcon(GuiGraphics g, ResearchEntry iconResearch, int iconX, int iconY, float zLevel, boolean bw) {
        if (iconResearch.getIcons() == null || iconResearch.getIcons().length == 0) {
            return;
        }
        int idx = (int) (System.currentTimeMillis() / 1000L % iconResearch.getIcons().length);
        Object icon = iconResearch.getIcons()[idx];
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (icon instanceof ResourceLocation rl) {
            RenderSystem.setShaderTexture(0, rl);
            if (bw) {
                RenderSystem.setShaderColor(0.2F, 0.2F, 0.2F, 1.0F);
            }
            int[] size = textureSize(rl);
            int w = size[0];
            int h = size[1];
            if (h > w && h % w == 0) {
                int m = h / w;
                float q = 16.0F / m;
                float idx1 = (float) (System.currentTimeMillis() / 150L % m) * q;
                UtilsFX.drawTexturedQuadF(g.pose(), iconX, iconY, 0.0F, idx1, 16.0F, q, zLevel);
            } else if (w > h && w % h == 0) {
                int m = w / h;
                float q = 16.0F / m;
                float idx1 = (float) (System.currentTimeMillis() / 150L % m) * q;
                UtilsFX.drawTexturedQuadF(g.pose(), iconX, iconY, idx1, 0.0F, q, 16.0F, zLevel);
            } else {
                UtilsFX.drawTexturedQuadFull(g.pose(), iconX, iconY, zLevel);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else if (icon instanceof ItemStack stack) {
            g.renderItem(stack, iconX, iconY);
        }
        // TODO этап 12: строка "focus:…" — GuiFocalManipulator.drawPart
        RenderSystem.disableBlend();
    }

    private static int[] textureSize(ResourceLocation rl) {
        return ICON_SIZES.computeIfAbsent(rl, k -> {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(k);
            GlStateManager._bindTexture(tex.getId());
            int w = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            return new int[] {Math.max(1, w), Math.max(1, h)};
        });
    }
}
