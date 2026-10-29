package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerSpa;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.tiles.devices.TileSpa;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран купели (порт GuiSpa): переключатель смешивания (8×8 у 89,35), шкала бака (столбик текстуры жидкости 8×48 с её цветом,
 * закрываемый фоном сверху по заполнению) и подсказки — жидкость с количеством, режим.
 */
public class GuiSpa extends AbstractContainerScreen<ContainerSpa> {
    static final ResourceLocation tex = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_spa.png");

    public GuiSpa(ContainerSpa menu, Inventory inv, Component title) {
        super(menu, inv, title);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTicks) {
        super.render(g, mx, my, partialTicks);
        TileSpa spa = this.menu.getSpa();
        int mposx = mx - (this.leftPos + 104);
        int mposy = my - (this.topPos + 10);
        if (spa != null && mposx >= 0 && mposy >= 0 && mposx < 10 && mposy < 55) {
            FluidStack fluid = spa.tank.getFluid();
            if (!fluid.isEmpty()) {
                List<Component> list = new ArrayList<>();
                list.add(fluid.getHoverName());
                list.add(Component.literal(fluid.getAmount() + " mb"));
                g.renderComponentTooltip(this.font, list, mx, my);
            }
        }
        mposx = mx - (this.leftPos + 88);
        mposy = my - (this.topPos + 34);
        if (spa != null && mposx >= 0 && mposy >= 0 && mposx < 10 && mposy < 10) {
            g.renderTooltip(this.font, Component.translatable(spa.getMix() ? "text.spa.mix.true" : "text.spa.mix.false"), mx, my);
        }
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        TileSpa spa = this.menu.getSpa();
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(tex, k, l, 0, 0, this.imageWidth, this.imageHeight);
        if (spa == null) {
            return;
        }
        g.blit(tex, k + 89, l + 35, 208, spa.getMix() ? 16 : 32, 8, 8);
        FluidStack fluid = spa.tank.getFluid();
        if (!fluid.isEmpty()) {
            IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
            TextureAtlasSprite icon = this.minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
            float bar = (float) spa.tank.getFluidAmount() / spa.tank.getCapacity();
            int c = ext.getTintColor(fluid);
            RenderSystem.setShaderColor((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, 1.0F);
            // 6 плиток 16×16 в масштабе 0.5 = столбик 8×48
            for (int a = 0; a < 6; a++) {
                g.blit(k + 107, l + 15 + a * 8, 0, 8, 8, icon);
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.blit(tex, k + 107, l + 15, 107, 15, 10, (int) (48.0F - 48.0F * bar));
        }
        g.blit(tex, k + 106, l + 11, 232, 0, 10, 55);
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int var7 = (int) mx - (this.leftPos + 89);
        int var8 = (int) my - (this.topPos + 35);
        if (var7 >= 0 && var8 >= 0 && var7 < 8 && var8 < 8) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
            this.minecraft.player.playSound(SoundsTC.clack.get(), 0.4F, 1.0F);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }
}
