package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerHandMirror;

/** Экран ручного зеркала (порт GuiHandMirror): слот отправки и рамка вокруг ячейки с самим зеркалом. Подписей нет. */
public class GuiHandMirror extends AbstractContainerScreen<ContainerHandMirror> {
    static final ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_handmirror.png");
    private final int ci;

    public GuiHandMirror(ContainerHandMirror menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.ci = inv.selected;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics g, float par1, int par2, int par3) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int var5 = (this.width - this.imageWidth) / 2;
        int var6 = (this.height - this.imageHeight) / 2;
        g.blit(tex, var5, var6, 0, 0, this.imageWidth, this.imageHeight);
        RenderSystem.enableBlend();
        g.blit(tex, var5 + 8 + this.ci * 18, var6 + 142, 240, 0, 16, 16);
        RenderSystem.disableBlend();
    }
}
