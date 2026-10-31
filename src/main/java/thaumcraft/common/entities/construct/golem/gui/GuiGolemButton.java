package thaumcraft.common.entities.construct.golem.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/**
 * Общая основа кнопок окна печати (GuiGolemCategoryButton, GuiGolemRedstoneButton, GuiGolemLockButton,
 * GuiGolemBWListButton, GuiGolemPropButton, GuiPlusMinusButton оригинала — все рисуют кусок gui_base.png). Только клиент.
 */
public abstract class GuiGolemButton extends AbstractButton {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    final Runnable onPress;

    GuiGolemButton(int x, int y, int w, int h, Component text, Runnable onPress) {
        super(x, y, w, h, text);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    void color(float c) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(c, c, c, c);
    }

    void reset() {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Подпись поверх остального окна. */
    void text(GuiGraphics g, String s, int x, int y, boolean centered) {
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 300.0F);
        var font = net.minecraft.client.Minecraft.getInstance().font;
        if (centered) {
            g.drawCenteredString(font, s, x, y, 16777215);
        } else {
            g.drawString(font, s, x, y, 16777215);
        }
        g.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    /** Значок раздела (x, y — центр); при наведении — название слева. */
    public static class Category extends GuiGolemButton {
        final int icon;
        final boolean selected;

        public Category(int x, int y, String key, int icon, boolean selected, Runnable onPress) {
            super(x - 8, y - 8, 16, 16, Component.translatable(key), onPress);
            this.icon = icon;
            this.selected = selected;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
            boolean hov = this.isHovered();
            this.color(this.selected || hov ? 1.0F : 0.7F);
            g.blit(TEX, this.getX(), this.getY(), this.icon * 16, 120, 16, 16);
            this.reset();
            if (hov) {
                String s = this.getMessage().getString();
                this.text(g, s, this.getX() + 8 - 10 - net.minecraft.client.Minecraft.getInstance().font.width(s), this.getY() + 4, false);
            }
        }
    }

    /** Реагирует ли печать на красный камень. */
    public static class Redstone extends GuiGolemButton {
        final java.util.function.BooleanSupplier on;

        public Redstone(int x, int y, java.util.function.BooleanSupplier on, Runnable onPress) {
            super(x, y, 16, 16, Component.empty(), onPress);
            this.on = on;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
            this.color(this.isHovered() ? 1.0F : 0.9F);
            g.blit(TEX, this.getX(), this.getY(), this.on.getAsBoolean() ? 64 : 80, 136, 16, 16);
            this.reset();
            if (this.isHovered()) {
                String s = Component.translatable(this.on.getAsBoolean() ? "golem.prop.redon" : "golem.prop.redoff").getString();
                this.text(g, s, this.getX() - 2 - net.minecraft.client.Minecraft.getInstance().font.width(s), this.getY() + 4, false);
            }
        }
    }

    /** Замок печати (только големы владельца). */
    public static class Lock extends GuiGolemButton {
        final java.util.function.BooleanSupplier locked;

        public Lock(int x, int y, java.util.function.BooleanSupplier locked, Runnable onPress) {
            super(x, y, 16, 16, Component.empty(), onPress);
            this.locked = locked;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
            this.color(this.isHovered() ? 1.0F : 0.9F);
            g.blit(TEX, this.getX(), this.getY(), this.locked.getAsBoolean() ? 32 : 48, 136, 16, 16);
            this.reset();
            if (this.isHovered()) {
                this.text(g, Component.translatable(this.locked.getAsBoolean() ? "golem.prop.lock" : "golem.prop.unlock").getString(), this.getX() + 8,
                        this.getY() + 17, true);
            }
        }
    }

    /** Чёрный/белый список фильтра. */
    public static class BWList extends GuiGolemButton {
        final java.util.function.BooleanSupplier black;

        public BWList(int x, int y, java.util.function.BooleanSupplier black, Runnable onPress) {
            super(x, y, 16, 16, Component.empty(), onPress);
            this.black = black;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
            this.color(this.isHovered() ? 1.0F : 0.9F);
            g.blit(TEX, this.getX(), this.getY(), this.black.getAsBoolean() ? 0 : 16, 136, 16, 16);
            this.reset();
            if (this.isHovered()) {
                this.text(g, Component.translatable(this.black.getAsBoolean() ? "button.bl" : "button.wl").getString(), this.getX() + 8, this.getY() + 17, true);
            }
        }
    }

    /** Переключатель настройки (галочка и название). */
    public static class Prop extends GuiGolemButton {
        final java.util.function.BooleanSupplier value;

        public Prop(int x, int y, String key, java.util.function.BooleanSupplier value, Runnable onPress) {
            super(x, y, 8, 8, Component.translatable(key), onPress);
            this.value = value;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
            this.color(1.0F);
            g.blit(TEX, this.getX() - 2, this.getY() - 2, 2, 18, 12, 12);
            if (this.value.getAsBoolean()) {
                g.blit(TEX, this.getX() - 2, this.getY() - 2, 18, 18, 12, 12);
            }
            this.reset();
            g.drawString(net.minecraft.client.Minecraft.getInstance().font, this.getMessage(), this.getX() + 12, this.getY(), 16777215);
        }
    }

    /** «−»/«+» (GuiPlusMinusButton). */
    public static class PlusMinus extends GuiGolemButton {
        final boolean minus;

        public PlusMinus(int x, int y, boolean minus, Runnable onPress) {
            super(x, y, 10, 10, Component.empty(), onPress);
            this.minus = minus;
        }

        @Override
        protected void renderWidget(GuiGraphics g, int xx, int yy, float pt) {
            this.color(this.isHovered() ? 1.0F : 0.9F);
            g.blit(TEX, this.getX(), this.getY(), this.minus ? 0 : 10, 0, 10, 10);
            this.reset();
        }
    }
}
