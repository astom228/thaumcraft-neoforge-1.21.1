package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.container.ContainerPotionSprayer;
import thaumcraft.common.tiles.devices.TilePotionSprayer;

/**
 * Экран распылителя зелий (порт GuiPotionSprayer, 192×233): столбик зарядов цвета зелья с бегущей текстурой, аспекты рецепта
 * с полосками накопления (цвет аспекта поверх тёмной подложки), рамка столбика. Подписей нет.
 */
public class GuiPotionSprayer extends AbstractContainerScreen<ContainerPotionSprayer> {
    static final ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_potion_sprayer.png");

    public GuiPotionSprayer(ContainerPotionSprayer menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 192;
        this.imageHeight = 233;
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
        TilePotionSprayer inv = this.menu.getSprayer();
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(tex, k, l, 0, 0, this.imageWidth, this.imageHeight);
        if (inv == null) {
            return;
        }
        if (inv.charges > 0) {
            int c = inv.color;
            RenderSystem.setShaderColor((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, 1.0F);
            int scroll = this.minecraft.player.tickCount % 256;
            g.blit(tex, k + 128, l + 36 + (8 - inv.charges) * 9, 232, scroll, 8, inv.charges * 9);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        this.drawAspects(g, inv, k, l);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(tex, k + 125, l + 28, 205, 28, 14, 88);
        RenderSystem.disableBlend();
    }

    private void drawAspects(GuiGraphics g, TilePotionSprayer inv, int k, int l) {
        int pos = 0;
        for (Aspect aspect : inv.recipe.getAspectsSortedByName()) {
            RenderSystem.setShaderColor(0.2F, 0.2F, 0.2F, 1.0F);
            g.blit(tex, k + 96 + 22 * (pos % 2), l + 46 + 16 * (pos / 2) - 14, 192, 56, 2, 14);
            int i1 = (int) ((float) inv.recipeProgress.getAmount(aspect) / inv.recipe.getAmount(aspect) * 14.0F);
            int c = aspect.getColor();
            RenderSystem.setShaderColor((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, 1.0F);
            g.blit(tex, k + 96 + 22 * (pos % 2), l + 46 + 16 * (pos / 2) - i1, 192, 56, 2, i1);
            pos++;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        pos = 0;
        for (Aspect aspect : inv.recipe.getAspectsSortedByName()) {
            UtilsFX.drawTag(g, k + 79 + 22 * (pos % 2), l + 31 + 16 * (pos / 2), aspect, inv.recipe.getAmount(aspect), 0);
            pos++;
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
