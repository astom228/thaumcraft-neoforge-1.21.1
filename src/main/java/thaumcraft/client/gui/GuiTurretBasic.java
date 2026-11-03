package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerTurretBasic;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

/** Окно арбалета-турели (порт GuiTurretBasic): ячейка стрел и полоска здоровья. */
public class GuiTurretBasic extends AbstractContainerScreen<ContainerTurretBasic> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_turret_basic.png");

    public GuiTurretBasic(ContainerTurretBasic menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 175;
        this.imageHeight = 232;
    }

    protected ResourceLocation texture() {
        return TEX;
    }

    protected int healthX() {
        return 68;
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
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        g.blit(this.texture(), k, l, 0, 0, this.imageWidth, this.imageHeight);
        EntityTurretCrossbow t = this.menu.getTurret();
        if (t != null) {
            int h = (int) (39.0F * (t.getHealth() / t.getMaxHealth()));
            g.blit(this.texture(), k + this.healthX(), l + 59, 192, 48, h, 6);
        }
    }
}
