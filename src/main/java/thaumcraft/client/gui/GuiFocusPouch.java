package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerFocusPouch;

/** Экран сумки для фокусов (порт GuiFocusPouch): 18 ячеек; ячейка хотбара с самой сумкой помечена и закрыта. */
public class GuiFocusPouch extends AbstractContainerScreen<ContainerFocusPouch> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_focuspouch.png");
    private final int blockSlot;

    public GuiFocusPouch(ContainerFocusPouch menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.blockSlot = menu.getPouchSlot();
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
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 200.0F);
        RenderSystem.enableBlend();
        g.blit(TEX, 8 + this.blockSlot * 18, 209, 240, 0, 16, 16);
        RenderSystem.disableBlend();
        g.pose().popPose();
    }

    /** checkHotbarKeys оригинала — false: цифровые клавиши не перекладывают предметы. */
    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        return false;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.enableBlend();
        g.blit(TEX, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        RenderSystem.disableBlend();
    }
}
