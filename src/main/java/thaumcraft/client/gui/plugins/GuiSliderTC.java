package thaumcraft.client.gui.plugins;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Ползунок (порт GuiSliderTC): бегунок 8×8 из gui_base.png; значение min..max; setMax сбрасывает положение. */
public class GuiSliderTC extends AbstractWidget {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    private float sliderPosition;
    public boolean isMouseDown;
    private final float min;
    private float max;
    private final boolean vertical;

    public GuiSliderTC(int x, int y, int w, int h, float min, float max, float def, boolean vertical) {
        super(x, y, w, h, Component.empty());
        this.min = min;
        this.max = max;
        this.sliderPosition = max == min ? 0.0F : (def - min) / (max - min);
        this.vertical = vertical;
    }

    public float getMax() {
        return this.max;
    }

    public void setMax(float max) {
        this.max = max;
        this.sliderPosition = 0.0F;
    }

    public float getSliderValue() {
        return this.min + (this.max - this.min) * this.sliderPosition;
    }

    public void setSliderValue(float value) {
        this.sliderPosition = this.max == this.min ? 0.0F : (value - this.min) / (this.max - this.min);
    }

    private void drag(double mx, double my) {
        this.sliderPosition = this.vertical ? (float) (my - (this.getY() + 4)) / (this.height - 8) : (float) (mx - (this.getX() + 4)) / (this.width - 8);
        this.sliderPosition = Math.max(0.0F, Math.min(1.0F, this.sliderPosition));
    }

    @Override
    public void onClick(double mx, double my) {
        this.drag(mx, my);
        this.isMouseDown = true;
    }

    @Override
    protected void onDrag(double mx, double my, double dx, double dy) {
        this.drag(mx, my);
    }

    @Override
    public void onRelease(double mx, double my) {
        this.isMouseDown = false;
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.vertical) {
            g.blit(TEX, this.getX(), this.getY() + (int) (this.sliderPosition * (this.height - 8)), 20, 20, 8, 8);
        } else {
            g.blit(TEX, this.getX() + (int) (this.sliderPosition * (this.width - 8)), this.getY(), 20, 20, 8, 8);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
