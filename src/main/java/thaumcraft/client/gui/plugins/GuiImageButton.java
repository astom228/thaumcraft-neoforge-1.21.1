package thaumcraft.client.gui.plugins;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Кнопка-картинка (порт GuiImageButton): x, y — центр; рисуется кусок текстуры (lx, ly, ww, hh) с оттенком color,
 * подпись — ключ перевода, мелким шрифтом по центру. active = false — затемнена и не нажимается.
 */
public class GuiImageButton extends AbstractButton {
    final ResourceLocation loc;
    final int lx;
    final int ly;
    final int ww;
    final int hh;
    public String description;
    public int color;
    final String displayKey;
    final Runnable onPress;

    public GuiImageButton(int x, int y, int width, int height, String buttonText, String description, ResourceLocation loc, int lx, int ly, int ww,
                          int hh, int color, Runnable onPress) {
        super(x, y, width, height, Component.translatable(buttonText == null ? "" : buttonText));
        this.displayKey = buttonText;
        this.description = description;
        this.color = color;
        this.loc = loc;
        this.lx = lx;
        this.ly = ly;
        this.ww = ww;
        this.hh = hh;
        this.onPress = onPress;
    }

    /** Область нажатия — прямоугольник с центром в (x, y). */
    @Override
    public boolean isMouseOver(double xx, double yy) {
        return this.visible && xx >= this.getX() - this.width / 2 && yy >= this.getY() - this.height / 2
                && xx < this.getX() - this.width / 2 + this.width && yy < this.getY() - this.height / 2 + this.height;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
        boolean hovered = this.isMouseOver(xx, yy);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Color c = new Color(this.color);
        float cc = 0.9F;
        float ac = 1.0F;
        if (hovered) {
            cc = 1.0F;
        }
        if (!this.active) {
            cc = 0.5F;
            ac = 0.9F;
        }
        RenderSystem.setShaderColor(cc * (c.getRed() / 255.0F), cc * (c.getGreen() / 255.0F), cc * (c.getBlue() / 255.0F), ac);
        g.blit(this.loc, this.getX() - this.ww / 2, this.getY() - this.hh / 2, this.lx, this.ly, this.ww, this.hh);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.displayKey != null) {
            int j = 16777215;
            if (!this.active) {
                j = 10526880;
            } else if (hovered) {
                j = 16777120;
            }
            Font font = Minecraft.getInstance().font;
            g.pose().pushPose();
            g.pose().translate(this.getX(), this.getY(), 0.0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            g.drawCenteredString(font, this.getMessage(), 0, -4, j);
            g.pose().popPose();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
