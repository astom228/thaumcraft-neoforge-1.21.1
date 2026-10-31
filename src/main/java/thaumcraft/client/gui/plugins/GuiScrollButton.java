package thaumcraft.client.gui.plugins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Стрелка прокрутки (порт GuiScrollButton): «−»/«+» (или вверх/вниз) из gui_base.png; x, y — левый верхний угол. */
public class GuiScrollButton extends AbstractButton {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    final boolean minus;
    final boolean vertical;
    final Runnable onPress;

    public GuiScrollButton(int x, int y, int width, int height, boolean minus, boolean vertical, Runnable onPress) {
        super(x, y, width, height, Component.empty());
        this.minus = minus;
        this.vertical = vertical;
        this.onPress = onPress;
    }

    public GuiScrollButton(int x, int y, int width, int height, boolean minus, Runnable onPress) {
        this(x, y, width, height, minus, false, onPress);
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
        float c = this.isHovered() && this.active ? 1.0F : 0.9F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(c, c, c, c);
        g.blit(TEX, this.getX(), this.getY(), this.vertical ? 67 : (this.minus ? 20 : 30), this.vertical ? (this.minus ? 0 : 10) : 0, 10, 10);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
