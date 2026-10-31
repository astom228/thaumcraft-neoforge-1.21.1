package thaumcraft.common.entities.construct.golem.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Кнопка «собрать» окна пресса (порт GuiGolemCraftButton): 24×16 из gui_golembuilder.png; неактивная — с затемнением. Только клиент. */
public class GuiGolemCraftButton extends AbstractButton {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_golembuilder.png");
    final Runnable onPress;

    public GuiGolemCraftButton(int x, int y, Runnable onPress) {
        super(x, y, 24, 16, Component.empty());
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        float c = this.active && this.isHovered() ? 1.0F : 0.9F;
        RenderSystem.setShaderColor(c, c, c, c);
        g.blit(TEX, this.getX(), this.getY(), 216, 64, 24, 16);
        if (!this.active) {
            g.blit(TEX, this.getX(), this.getY(), 216, 40, 24, 16);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
