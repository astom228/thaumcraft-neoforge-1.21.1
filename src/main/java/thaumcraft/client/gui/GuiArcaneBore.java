package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerArcaneBore;
import thaumcraft.common.entities.construct.EntityArcaneBore;

/**
 * Окно магического бура (порт GuiArcaneBore): кирка (сломанная — с отметкой), здоровье, ширина/глубина/скорость и
 * особые свойства (очистка, удача, шёлковое касание). Подписи в оригинале были английским текстом — здесь ключи перевода.
 */
public class GuiArcaneBore extends AbstractContainerScreen<ContainerArcaneBore> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_arcanebore.png");

    public GuiArcaneBore(ContainerArcaneBore menu, Inventory inv, Component title) {
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
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        g.blit(TEX, k, l, 0, 0, this.imageWidth, this.imageHeight);
        EntityArcaneBore bore = this.menu.getBore();
        if (bore == null) {
            return;
        }
        int h = (int) (39.0F * (bore.getHealth() / bore.getMaxHealth()));
        g.blit(TEX, k + 68, l + 59, 192, 48, h, 6);
        ItemStack pick = bore.getMainHandItem();
        if (!pick.isEmpty() && pick.isDamageableItem() && pick.getDamageValue() + 1 >= pick.getMaxDamage()) {
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);
            g.blit(TEX, k + 80, l + 29, 240, 0, 16, 16);
            g.pose().popPose();
        }
        g.pose().pushPose();
        g.pose().translate(k + 124, l + 18, 505.0F);
        g.pose().scale(0.5F, 0.5F, 1.0F);
        g.drawString(this.font, Component.translatable("tc.bore.width", 1 + bore.getDigRadius() * 2), 0, 0, 16777215, true);
        g.drawString(this.font, Component.translatable("tc.bore.depth", bore.getDigDepth()), 64, 0, 16777215, true);
        g.drawString(this.font, Component.translatable("tc.bore.speed", bore.getDigSpeed(Blocks.STONE.defaultBlockState())), 0, 10, 16777215, true);
        int base = 0;
        int refining = bore.getRefining();
        int fortune = bore.getFortune();
        if (bore.hasSilkTouch() || refining > 0 || fortune > 0) {
            g.drawString(this.font, Component.translatable("tc.bore.other"), 0, 24, 16777215, true);
        }
        if (refining > 0) {
            g.drawString(this.font, Component.translatable("tc.bore.refining", refining), 4, 34 + base, 12632256, true);
            base += 9;
        }
        if (fortune > 0) {
            g.drawString(this.font, Component.translatable("tc.bore.fortune", fortune), 4, 34 + base, 15648330, true);
            base += 9;
        }
        if (bore.hasSilkTouch()) {
            g.drawString(this.font, Component.translatable("tc.bore.silk"), 4, 34 + base, 8421631, true);
        }
        g.pose().popPose();
    }
}
