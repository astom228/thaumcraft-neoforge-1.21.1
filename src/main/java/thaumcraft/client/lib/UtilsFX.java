package thaumcraft.client.lib;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.text.DecimalFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.config.ModConfig;

/** Порт части UtilsFX (отрисовка в интерфейсе; методы добавляются по этапам). */
public final class UtilsFX {
    static final DecimalFormat myFormatter = new DecimalFormat("#######.##");
    /** EnumFacing.HORIZONTALS 1.12 (S, W, N, E): смещения обводки текста по X и Z. */
    private static final int[][] HORIZONTALS = {{0, 1}, {-1, 0}, {0, -1}, {1, 0}};

    /**
     * Значок аспекта 16×16 с количеством (drawTag оригинала). blend — второй аргумент glBlendFunc: 771 — обычное смешивание,
     * 1 — аддитивное (так рисуются почти все аспекты). bw — чёрно-белый (неизвестный) значок.
     * TODO этап 6: bonus > 0 — анимированный пузырёк из текстуры частиц.
     */
    public static void drawTag(GuiGraphics g, int x, int y, Aspect aspect, float amount, int bonus, int blend, float alpha, boolean bw) {
        if (aspect == null) {
            return;
        }
        int c = aspect.getColor();
        float r = (c >> 16 & 255) / 255.0F;
        float gr = (c >> 8 & 255) / 255.0F;
        float b = (c & 255) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                blend == 1 ? GlStateManager.DestFactor.ONE : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        if (!bw) {
            RenderSystem.setShaderColor(r, gr, b, alpha);
        } else {
            RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, alpha * 0.8F);
        }
        g.blit(aspect.getImage(), x, y, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        if (amount > 0.0F) {
            Font font = Minecraft.getInstance().font;
            g.pose().pushPose();
            float q = 0.5F;
            if (!ModConfig.CONFIG_GRAPHICS.largeTagText.get()) {
                g.pose().scale(0.5F, 0.5F, 0.5F);
                q = 1.0F;
            }
            String am = myFormatter.format(amount);
            int sw = font.width(am);
            int tx = (int) ((32 - sw + x * 2) * q);
            int ty = (int) ((32 - font.lineHeight + y * 2) * q);
            for (int[] e : HORIZONTALS) {
                g.drawString(font, am, tx + e[0], ty + e[1], 0, false);
            }
            g.drawString(font, am, tx, ty, 0xFFFFFF, false);
            g.pose().popPose();
        }
    }

    public static void drawTag(GuiGraphics g, int x, int y, Aspect aspect, float amount, int bonus) {
        drawTag(g, x, y, aspect, amount, bonus, 771, 1.0F, false);
    }

    private UtilsFX() {
    }
}
