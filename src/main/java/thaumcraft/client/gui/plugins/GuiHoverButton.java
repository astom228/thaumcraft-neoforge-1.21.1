package thaumcraft.client.gui.plugins;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.UtilsFX;

/**
 * Значок с подсказкой (порт GuiHoverButton): x, y — центр; рисует аспект, текстуру 16×16 или предмет; не нажимается.
 * Подсказку рисует экран (drawButtonForegroundLayer) поверх всего.
 */
public class GuiHoverButton extends AbstractButton {
    final Component description;
    final int color;
    final Object tex;

    public GuiHoverButton(int x, int y, int width, int height, Component text, Component description, Object tex) {
        this(x, y, width, height, text, description, tex, 16777215);
    }

    public GuiHoverButton(int x, int y, int width, int height, Component text, Component description, Object tex, int color) {
        super(x, y, width, height, text);
        this.description = description;
        this.tex = tex;
        this.color = color;
    }

    @Override
    public boolean isMouseOver(double xx, double yy) {
        return this.visible && xx >= this.getX() - this.width / 2 && yy >= this.getY() - this.height / 2
                && xx < this.getX() - this.width / 2 + this.width && yy < this.getY() - this.height / 2 + this.height;
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return false;
    }

    @Override
    public void onPress() {
    }

    @Override
    protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
        boolean hovered = this.isMouseOver(xx, yy);
        Color c = new Color(this.color);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (hovered) {
            RenderSystem.setShaderColor(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
        } else {
            RenderSystem.setShaderColor(0.9F * (c.getRed() / 255.0F), 0.9F * (c.getGreen() / 255.0F), 0.9F * (c.getBlue() / 255.0F), 0.9F);
        }
        int x0 = this.getX() - this.width / 2;
        int y0 = this.getY() - this.height / 2;
        if (this.tex instanceof Aspect a) {
            Color c2 = new Color(a.getColor());
            if (!hovered) {
                RenderSystem.setShaderColor(c2.getRed() / 290.0F, c2.getGreen() / 290.0F, c2.getBlue() / 290.0F, 0.9F);
            } else {
                RenderSystem.setShaderColor(c2.getRed() / 255.0F, c2.getGreen() / 255.0F, c2.getBlue() / 255.0F, 1.0F);
            }
            g.blit(a.getImage(), x0, y0, 0.0F, 0.0F, 16, 16, 16, 16);
        } else if (this.tex instanceof ResourceLocation rl) {
            g.blit(rl, x0, y0, 0.0F, 0.0F, 16, 16, 16, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.tex instanceof ItemStack stack) {
            UtilsFX.renderItemStackShaded(g, stack, x0, y0 - (hovered ? 1 : 0), 1.0F);
        }
    }

    /** Подсказка: для предмета — его описание, иначе — название; описание кнопки — курсивом синим. */
    public void drawButtonForegroundLayer(Screen screen, GuiGraphics g, int xx, int yy) {
        Minecraft mc = Minecraft.getInstance();
        List<String> text = new ArrayList<>();
        if (this.tex instanceof ItemStack stack) {
            for (Component line : Screen.getTooltipFromItem(mc, stack)) {
                text.add(line.getString());
            }
        } else {
            text.add(this.getMessage().getString());
        }
        int m = 8;
        if (this.description != null) {
            m = 0;
            text.add("§o§9" + this.description.getString());
        }
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        UtilsFX.drawCustomTooltip(g, mc.font, text, xx + 4, yy + m, -99);
        g.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
