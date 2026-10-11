package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.awt.Color;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.ArcaneCraftingInput;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.common.blocks.world.ore.ShardType;
import thaumcraft.common.container.ContainerArcaneWorkbench;
import thaumcraft.common.items.casters.CasterManager;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.crafting.TileArcaneWorkbench;

/**
 * Экран магического верстака (порт GuiArcaneWorkbench): вращающиеся круги у слотов нужных кристаллов, доступный Вис ауры,
 * стоимость рецепта со скидкой и, при нехватке Вис, изображение результата в пустом слоте.
 * Надписи «available», «vis», «% discount» в оригинале были в коде по-английски — вынесены в ключи перевода.
 */
public class GuiArcaneWorkbench extends AbstractContainerScreen<ContainerArcaneWorkbench> {
    private final TileArcaneWorkbench tileEntity;
    private final Inventory ip;
    static final ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/arcaneworkbench.png");

    public GuiArcaneWorkbench(ContainerArcaneWorkbench menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.tileEntity = menu.getTile();
        this.ip = inv;
        this.imageHeight = 234;
        this.imageWidth = 190;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }

    /** В оригинале подписей окна и инвентаря нет. */
    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics g, float par1, int par2, int par3) {
        float pt = this.minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        int var5 = (this.width - this.imageWidth) / 2;
        int var6 = (this.height - this.imageHeight) / 2;
        g.blit(tex, var5, var6, 0, 0, this.imageWidth, this.imageHeight);
        int cost = 0;
        int discount = 0;
        ArcaneCraftingInput input = this.tileEntity.getCraftingInput();
        IArcaneRecipe result = ThaumcraftCraftingManager.findMatchingArcaneRecipe(input, this.ip.player);
        AspectList crystals = null;
        float df = CasterManager.getTotalVisDiscount(this.ip.player);
        if (result != null) {
            cost = result.getVis();
            cost = (int) (cost * (1.0F - df));
            discount = (int) (df * 100.0F);
            crystals = result.getCrystals();
        }
        if (crystals != null) {
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            for (Aspect a : crystals.getAspects()) {
                int id = ShardType.getMetaByAspect(a);
                if (id < 0 || id >= ContainerArcaneWorkbench.xx.length) {
                    continue;
                }
                Color col = new Color(a.getColor());
                RenderSystem.setShaderColor(col.getRed() / 255.0F, col.getGreen() / 255.0F, col.getBlue() / 255.0F, 0.33F);
                g.pose().pushPose();
                g.pose().translate(var5 + ContainerArcaneWorkbench.xx[id] + 7.5F, var6 + ContainerArcaneWorkbench.yy[id] + 8.0F, 0.0F);
                g.pose().mulPose(Axis.ZP.rotationDegrees(id * 60 + this.minecraft.player.tickCount % 360 + pt));
                g.pose().scale(0.5F, 0.5F, 1.0F);
                g.blit(tex, -32, -32, 192, 0, 64, 64);
                g.pose().popPose();
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableBlend();
        g.pose().pushPose();
        g.pose().translate(var5 + 168, var6 + 46, 0.0F);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        String text = I18n.get("tc.workbench.available", this.tileEntity.auraVisClient);
        int ll = this.font.width(text) / 2;
        g.drawString(this.font, text, -ll, 0, this.tileEntity.auraVisClient < cost ? 15625838 : 7237358, false);
        g.pose().popPose();
        if (cost > 0) {
            if (this.tileEntity.auraVisClient < cost) {
                // в 1.12 предмет рисовался поверх фона в пустом слоте результата
                ItemStack out = result.assemble(input, this.minecraft.level.registryAccess());
                g.renderItem(out, var5 + 160, var6 + 64);
                g.renderItemDecorations(this.font, out, var5 + 160, var6 + 64, "");
            }
            g.pose().pushPose();
            g.pose().translate(var5 + 168, var6 + 38, 0.0F);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            text = I18n.get("tc.workbench.vis", cost);
            if (discount > 0) {
                text = text + " " + I18n.get("tc.workbench.discount", discount);
            }
            ll = this.font.width(text) / 2;
            g.drawString(this.font, text, -ll, 0, 12648447, false);
            g.pose().popPose();
        }
    }
}
