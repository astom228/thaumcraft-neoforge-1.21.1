package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerPech;
import thaumcraft.common.lib.SoundsTC;

/** Окно торговли с пехом (порт GuiPech): кнопка-кубики подсвечивается, когда сделка возможна. */
public class GuiPech extends AbstractContainerScreen<ContainerPech> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_pech.png");

    public GuiPech(ContainerPech menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 175;
        this.imageHeight = 232;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int x, int y) {
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.enableBlend();
        g.blit(TEX, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.menu.canTrade()) {
            g.blit(TEX, this.leftPos + 67, this.topPos + 24, 176, 0, 25, 25);
        }
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = (int) mx - (this.leftPos + 67);
        int y = (int) my - (this.topPos + 24);
        if (x >= 0 && y >= 0 && x < 25 && y < 25 && this.menu.canTrade() && this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            if (this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundsTC.pech_dice.get(), 0.5F, 0.95F + this.minecraft.player.getRandom().nextFloat() * 0.1F);
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
