package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.container.ContainerThaumatorium;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

/**
 * Экран тауматориума (порт GuiThaumatorium): выбор рецепта стрелками, щелчок по результату — выбрать/снять (кнопка меню),
 * аспекты рецепта с заполненностью, число выбранных рецептов из доступных.
 */
public class GuiThaumatorium extends AbstractContainerScreen<ContainerThaumatorium> {
    static final ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_thaumatorium.png");
    private final TileThaumatorium inventory;
    private int index = 0;
    private int lastSize;
    int startAspect = 0;

    public GuiThaumatorium(ContainerThaumatorium menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.inventory = menu.getTile();
        this.menu.updateRecipes();
        this.lastSize = this.menu.recipes.size();
        this.refreshIndex();
    }

    /** На клиенте список рецептов пересчитывается каждый тик (в оригинале — через eventHandler при изменении слота). */
    @Override
    protected void containerTick() {
        super.containerTick();
        this.menu.updateRecipes();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    void refreshIndex() {
        if (this.inventory.recipeHash != null && !this.menu.recipes.isEmpty()) {
            for (int a = 0; a < this.menu.recipes.size(); a++) {
                if (this.inventory.recipeHash.contains(this.menu.recipes.get(a).hash)) {
                    this.index = a;
                    break;
                }
            }
        }
        this.startAspect = 0;
    }

    @Override
    protected void renderBg(GuiGraphics g, float par1, int mx, int my) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        RenderSystem.enableBlend();
        g.blit(tex, k, l, 0, 0, this.imageWidth, this.imageHeight);
        if (this.index >= this.menu.recipes.size()) {
            this.index = this.menu.recipes.size() - 1;
        }
        if (this.menu.recipes.isEmpty()) {
            return;
        }
        if (this.lastSize != this.menu.recipes.size()) {
            this.lastSize = this.menu.recipes.size();
            this.refreshIndex();
        }
        if (this.index < 0) {
            this.index = 0;
        }
        CrucibleRecipe rec = this.menu.recipes.get(this.index);
        if (this.menu.recipes.size() > 1) {
            g.blit(tex, k + 128, l + 16, this.index > 0 ? 192 : 176, 16, 16, 8);
            g.blit(tex, k + 128, l + 24, this.index < this.menu.recipes.size() - 1 ? 192 : 176, 24, 16, 8);
        }
        if (rec.getAspects().size() > 6) {
            g.blit(tex, k + 32, l + 40, this.startAspect > 0 ? 192 : 176, 32, 8, 16);
            g.blit(tex, k + 136, l + 40, this.startAspect < rec.getAspects().size() - 1 ? 200 : 184, 32, 8, 16);
        } else {
            this.startAspect = 0;
        }
        if (this.inventory.recipeHash != null && !this.inventory.recipeHash.isEmpty()) {
            int x = mx - (k + 112);
            int y = my - (l + 16);
            if (x >= 0 && y >= 0 && x < 16 && y < 16 || this.inventory.recipeHash.contains(rec.hash)) {
                g.blit(tex, k + 104, l + 8, 176, 96, 48, 48);
            }
            float alpha = 0.6F + Mth.sin(this.minecraft.player.tickCount / 5.0F) * 0.4F + 0.4F;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            g.blit(tex, k + 88, l + 16, 176, 56, 24, 24);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        this.drawAspects(g, k, l, rec);
        this.drawOutput(g, k, l, mx, my, rec);
        if (this.inventory.maxRecipes > 1) {
            g.pose().pushPose();
            g.pose().translate(k + 136, l + 33, 0.0F);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            String text = this.inventory.recipeHash.size() + "/" + this.inventory.maxRecipes;
            int ll = this.font.width(text) / 2;
            g.drawString(this.font, text, -ll, 0, 16777215, false);
            g.pose().popPose();
        }
    }

    private void drawAspects(GuiGraphics g, int k, int l, CrucibleRecipe rec) {
        int count = 0;
        int pos = 0;
        if (this.inventory.recipeHash.contains(rec.hash)) {
            for (Aspect aspect : rec.getAspects().getAspectsSortedByName()) {
                if (count >= this.startAspect) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    g.blit(tex, k + 41 + 16 * pos, l + 57, 176, 8, 14, 6);
                    int i1 = (int) ((float) this.inventory.essentia.getAmount(aspect) / rec.getAspects().getAmount(aspect) * 12.0F);
                    Color c = new Color(aspect.getColor());
                    RenderSystem.setShaderColor(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, 1.0F);
                    g.blit(tex, k + 42 + 16 * pos, l + 58, 176, 0, i1, 4);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    pos++;
                }
                if (++count >= 6 + this.startAspect) {
                    break;
                }
            }
        }
        count = 0;
        pos = 0;
        for (Aspect aspect : rec.getAspects().getAspectsSortedByName()) {
            if (count >= this.startAspect) {
                UtilsFX.drawTag(g, k + 40 + 16 * pos, l + 40, aspect, rec.getAspects().getAmount(aspect), 0);
                pos++;
            }
            if (++count >= 6 + this.startAspect) {
                break;
            }
        }
    }

    private void drawOutput(GuiGraphics g, int x, int y, int mx, int my, CrucibleRecipe rec) {
        if (this.inventory.recipeHash.size() < this.inventory.maxRecipes || this.inventory.recipeHash.contains(rec.hash)) {
            float alpha = 0.3F + Mth.sin(this.minecraft.player.tickCount / 4.0F) * 0.3F + 0.3F;
            RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, alpha);
        }
        g.renderItem(rec.getRecipeOutput(), x + 112, y + 16);
        g.renderItemDecorations(this.font, rec.getRecipeOutput(), x + 112, y + 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int xx = mx - (x + 112);
        int yy = my - (y + 16);
        if (xx >= 0 && yy >= 0 && xx < 16 && yy < 16) {
            g.renderTooltip(this.font, rec.getRecipeOutput(), mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double dmx, double dmy, int button) {
        boolean r = super.mouseClicked(dmx, dmy, button);
        int mx = (int) dmx;
        int my = (int) dmy;
        int gx = (this.width - this.imageWidth) / 2;
        int gy = (this.height - this.imageHeight) / 2;
        int x = mx - (gx + 112);
        int y = my - (gy + 16);
        if (!this.menu.recipes.isEmpty() && this.index >= 0 && this.index < this.menu.recipes.size()) {
            CrucibleRecipe rec = this.menu.recipes.get(this.index);
            if (x >= 0 && y >= 0 && x < 16 && y < 16
                    && (this.inventory.recipeHash.size() < this.inventory.maxRecipes || this.inventory.recipeHash.contains(rec.hash))) {
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, this.index);
                this.minecraft.player.playSound(SoundsTC.hhon.get(), 0.3F, 1.0F);
            }
            if (this.menu.recipes.size() > 1) {
                if (this.index > 0) {
                    x = mx - (gx + 128);
                    y = my - (gy + 16);
                    if (x >= 0 && y >= 0 && x < 16 && y < 8) {
                        this.index--;
                        this.minecraft.player.playSound(SoundsTC.clack.get(), 0.4F, 1.0F);
                    }
                }
                if (this.index < this.menu.recipes.size() - 1) {
                    x = mx - (gx + 128);
                    y = my - (gy + 24);
                    if (x >= 0 && y >= 0 && x < 16 && y < 8) {
                        this.index++;
                        this.minecraft.player.playSound(SoundsTC.clack.get(), 0.4F, 1.0F);
                    }
                }
            }
            rec = this.menu.recipes.get(this.index);
            if (rec.getAspects().size() > 6) {
                if (this.startAspect > 0) {
                    x = mx - (gx + 32);
                    y = my - (gy + 40);
                    if (x >= 0 && y >= 0 && x < 8 && y < 16) {
                        this.startAspect--;
                        this.minecraft.player.playSound(SoundsTC.clack.get(), 0.4F, 1.0F);
                    }
                }
                if (this.startAspect < rec.getAspects().size() - 1) {
                    x = mx - (gx + 136);
                    y = my - (gy + 40);
                    if (x >= 0 && y >= 0 && x < 8 && y < 16) {
                        this.startAspect++;
                        this.minecraft.player.playSound(SoundsTC.clack.get(), 0.4F, 1.0F);
                    }
                }
            }
        }
        return r;
    }
}
