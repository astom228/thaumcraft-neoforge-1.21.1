package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thaumcraft.Thaumcraft;
import thaumcraft.common.container.ContainerTurretAdvanced;
import thaumcraft.common.entities.construct.EntityTurretCrossbowAdvanced;

/** Окно улучшенного арбалета (порт GuiTurretAdvanced): ячейка стрел, здоровье, четыре флажка целей (GuiToggleButton). */
public class GuiTurretAdvanced extends AbstractContainerScreen<ContainerTurretAdvanced> {
    public static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_turret_advanced.png");

    public GuiTurretAdvanced(ContainerTurretAdvanced menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 175;
        this.imageHeight = 232;
    }

    @Override
    protected void init() {
        super.init();
        EntityTurretCrossbowAdvanced t = this.menu.getAdvanced();
        if (t == null) {
            return;
        }
        this.addRenderableWidget(new Toggle(this.leftPos + 90, this.topPos + 13, "button.turretfocus.1", t::getTargetAnimal, () -> this.send(1)));
        this.addRenderableWidget(new Toggle(this.leftPos + 90, this.topPos + 27, "button.turretfocus.2", t::getTargetMob, () -> this.send(2)));
        this.addRenderableWidget(new Toggle(this.leftPos + 90, this.topPos + 41, "button.turretfocus.3", t::getTargetPlayer, () -> this.send(3)));
        this.addRenderableWidget(new Toggle(this.leftPos + 90, this.topPos + 55, "button.turretfocus.4", t::getTargetFriendly, () -> this.send(4)));
    }

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
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
        EntityTurretCrossbowAdvanced t = this.menu.getAdvanced();
        if (t != null) {
            int h = (int) (39.0F * (t.getHealth() / t.getMaxHealth()));
            g.blit(TEX, k + 30, l + 59, 192, 48, h, 6);
        }
    }

    /** Флажок (порт GuiToggleButton): клетка 8×8, отмечена — если флаг включён; подпись справа. */
    static class Toggle extends AbstractButton {
        final BooleanSupplier value;
        final Runnable onPress;

        Toggle(int x, int y, String key, BooleanSupplier value, Runnable onPress) {
            super(x, y, 8, 8, Component.translatable(key));
            this.value = value;
            this.onPress = onPress;
        }

        @Override
        public void onPress() {
            this.onPress.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.blit(TEX, this.getX(), this.getY(), 192, 16, 8, 8);
            if (this.value.getAsBoolean()) {
                g.blit(TEX, this.getX(), this.getY(), 192, 24, 8, 8);
            }
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, this.getMessage(), this.getX() + 12, this.getY(), 16777215);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
