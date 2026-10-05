package thaumcraft.client.lib;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import java.text.DecimalFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;
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

    // --- квадраты оригинала (текстура и шейдер задаются вызывающим) ------------------------------------------------------

    /** Квадрат 16×16 с UV в шестнадцатых долях текстуры (drawTexturedQuadF). Цвет — текущий цвет шейдера. */
    public static void drawTexturedQuadF(PoseStack pose, float x, float y, float u, float v, float w, float h, float z) {
        float d = 0.0625F;
        Matrix4f m = pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, x, y + 16.0F, z).setUv(u * d, (v + h) * d);
        b.addVertex(m, x + 16.0F, y + 16.0F, z).setUv((u + w) * d, (v + h) * d);
        b.addVertex(m, x + 16.0F, y, z).setUv((u + w) * d, v * d);
        b.addVertex(m, x, y, z).setUv(u * d, v * d);
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** Вся текстура в квадрате 16×16 (drawTexturedQuadFull). */
    public static void drawTexturedQuadFull(PoseStack pose, float x, float y, float z) {
        drawTexturedQuadF(pose, x, y, 0.0F, 0.0F, 16.0F, 16.0F, z);
    }

    /**
     * Квадрат scale×scale с центром в начале координат, кадр frame из сетки gridX×gridY (renderQuadCentered / TexturedQuadTC).
     * blend — второй аргумент glBlendFunc (1 — аддитивно, 771 — обычно). brightness в интерфейсе не влияет (нет карты освещения).
     * Как в оригинале, текстура отражена по горизонтали (x = -0.5 ↔ правый край кадра).
     */
    public static void renderQuadCentered(PoseStack pose, int gridX, int gridY, int frame, float scale, float red, float green, float blue,
                                          int brightness, int blend, float opacity) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                blend == 1 ? GlStateManager.DestFactor.ONE : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        int xm = frame % gridX;
        int ym = frame / gridY;
        float f1 = (float) xm / gridX;
        float f2 = f1 + 1.0F / gridX;
        float f3 = (float) ym / gridY;
        float f4 = f3 + 1.0F / gridY;
        Matrix4f m = pose.last().pose();
        float h = 0.5F * scale;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.addVertex(m, -h, h, 0.0F).setUv(f2, f4).setColor(red, green, blue, opacity);
        b.addVertex(m, h, h, 0.0F).setUv(f2, f3).setColor(red, green, blue, opacity);
        b.addVertex(m, h, -h, 0.0F).setUv(f1, f3).setColor(red, green, blue, opacity);
        b.addVertex(m, -h, -h, 0.0F).setUv(f1, f4).setColor(red, green, blue, opacity);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.defaultBlendFunc();
    }

    // --- отрисовка поверх кадра (RenderTickEvent оригинала) -------------------------------------------------------------

    /**
     * Ортопроекция оригинала для HUD и частиц интерфейса: glOrtho(0, w, h, 0, 1000, 3000), сдвиг -2000, очистка глубины.
     * Вызывается после отрисовки кадра (RenderFrameEvent.Post); парный вызов — endFrameOverlay.
     */
    public static void beginFrameOverlay() {
        Window win = Minecraft.getInstance().getWindow();
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, (float) (win.getWidth() / win.getGuiScale()),
                (float) (win.getHeight() / win.getGuiScale()), 0.0F, 1000.0F, 3000.0F), VertexSorting.ORTHOGRAPHIC_Z);
        Matrix4fStack mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        mv.translate(0.0F, 0.0F, -2000.0F);
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    public static void endFrameOverlay() {
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private UtilsFX() {
    }
}
