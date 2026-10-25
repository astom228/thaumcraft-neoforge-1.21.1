package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerSmelter;

/** Экран плавильни (порт GuiSmelter): пламя, ход переплавки, шкала эссенции (до 256) и рамка шкалы. Подписей нет. */
public class GuiSmelter extends AbstractContainerScreen<ContainerSmelter> {
    static final ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_smelter.png");

    public GuiSmelter(ContainerSmelter menu, Inventory inv, Component title) {
        super(menu, inv, title);
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
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        RenderSystem.enableBlend();
        g.blit(tex, k, l, 0, 0, this.imageWidth, this.imageHeight);
        int burn = this.getBurnTimeRemainingScaled(20);
        if (burn > 0) {
            g.blit(tex, k + 80, l + 26 + 20 - burn, 176, 20 - burn, 16, burn);
        }
        int i1 = this.getCookProgressScaled(46);
        g.blit(tex, k + 106, l + 13 + 46 - i1, 216, 46 - i1, 9, i1);
        i1 = this.getVisScaled(48);
        g.blit(tex, k + 61, l + 12 + 48 - i1, 200, 48 - i1, 8, i1);
        g.blit(tex, k + 60, l + 8, 232, 0, 10, 55);
        RenderSystem.disableBlend();
    }

    private int getCookProgressScaled(int par1) {
        int smeltTime = this.menu.getSmeltTime();
        if (smeltTime <= 0) {
            smeltTime = 1;
        }
        return this.menu.getCookTime() * par1 / smeltTime;
    }

    private int getVisScaled(int par1) {
        return this.menu.getVis() * par1 / 256;
    }

    private int getBurnTimeRemainingScaled(int par1) {
        int current = this.menu.getItemBurnTime();
        if (current == 0) {
            current = 200;
        }
        return this.menu.getBurnTime() * par1 / current;
    }
}
