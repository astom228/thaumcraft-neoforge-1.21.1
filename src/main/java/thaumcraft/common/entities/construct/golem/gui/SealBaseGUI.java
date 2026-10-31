package thaumcraft.common.entities.construct.golem.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.seals.ISealConfigFilter;
import thaumcraft.api.golems.seals.ISealConfigToggles;
import thaumcraft.api.golems.seals.ISealEntity;
import thaumcraft.client.gui.plugins.GuiHoverButton;

/**
 * Экран настройки печати (порт SealBaseGUI): круг с разделами по дуге слева, внизу — инвентарь. Нажатия уходят на сервер
 * номерами кнопок (clickMenuButton); значения возвращаются числами окна. Только клиент.
 */
public class SealBaseGUI extends AbstractContainerScreen<SealBaseContainer> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    final ISealEntity seal;
    final int middleX;
    final int middleY;
    int category = -1;
    final int[] categories;

    public SealBaseGUI(SealBaseContainer menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.seal = menu.getSeal();
        this.imageWidth = 176;
        this.imageHeight = 232;
        this.middleX = this.imageWidth / 2;
        this.middleY = (this.imageHeight - 72) / 2 - 8;
        this.categories = menu.categories;
        this.category = menu.getCategory();
    }

    @Override
    protected void init() {
        super.init();
        this.setupCategories();
    }

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private boolean isOwner() {
        return this.minecraft != null && this.minecraft.player != null && this.minecraft.player.getUUID().toString().equals(this.seal.getOwner());
    }

    private void setupCategories() {
        this.clearWidgets();
        int gl = this.leftPos;
        int gt = this.topPos;
        int c = 0;
        float slice = 60.0F / this.categories.length;
        float start = -180.0F + (this.categories.length - 1) * slice / 2.0F;
        if (slice > 24.0F) {
            slice = 24.0F;
        }
        if (slice < 12.0F) {
            slice = 12.0F;
        }
        for (int cat : this.categories) {
            if (this.category < 0) {
                this.category = cat;
            }
            if (this.categories.length > 1) {
                int xx = (int) (Mth.cos((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
                int yy = (int) (Mth.sin((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
                final int index = c;
                this.addRenderableWidget(new GuiGolemButton.Category(gl + this.middleX + xx, gt + this.middleY + yy, "button.category." + cat, cat,
                        this.category == cat, () -> this.selectCategory(index)));
            }
            c++;
        }
        int xx = (int) (Mth.cos((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
        int yy = (int) (Mth.sin((start - c * slice) / 180.0F * (float) Math.PI) * 86.0F);
        this.addRenderableWidget(new GuiGolemButton.Redstone(gl + this.middleX + xx - 8, gt + this.middleY + yy - 8, this.seal::isRedstoneSensitive, () -> {
            if (this.isOwner()) {
                this.send(this.seal.isRedstoneSensitive() ? 28 : 27);
            }
        }));
        switch (this.category) {
            case 0 -> {
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX - 5 - 14, gt + this.middleY - 17, true, () -> this.send(80)));
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX - 5 + 14, gt + this.middleY - 17, false, () -> this.send(81)));
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX + 18 - 12, gt + this.middleY + 4, true, () -> this.send(82)));
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX + 18 + 11, gt + this.middleY + 4, false, () -> this.send(83)));
                this.addRenderableWidget(new GuiGolemButton.Lock(gl + this.middleX - 32, gt + this.middleY, this.seal::isLocked, () -> {
                    if (this.isOwner()) {
                        this.send(this.seal.isLocked() ? 26 : 25);
                    }
                }));
            }
            case 1 -> {
                if (this.seal.getSeal() instanceof ISealConfigFilter f) {
                    int s = f.getFilterSize();
                    int sy = 16 + (s - 1) / 3 * 12;
                    this.addRenderableWidget(new GuiGolemButton.BWList(gl + this.middleX - 8, gt + this.middleY + (s - 1) / 3 * 24 - sy + 27, f::isBlacklist,
                            () -> this.send(f.isBlacklist() ? 21 : 20)));
                }
            }
            case 2 -> {
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX - 5 - 14, gt + this.middleY - 25, true, () -> this.send(90)));
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX - 5 + 14, gt + this.middleY - 25, false, () -> this.send(91)));
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX - 5 - 14, gt + this.middleY, true, () -> this.send(92)));
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX - 5 + 14, gt + this.middleY, false, () -> this.send(93)));
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX - 5 - 14, gt + this.middleY + 25, true, () -> this.send(94)));
                this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + this.middleX - 5 + 14, gt + this.middleY + 25, false, () -> this.send(95)));
            }
            case 3 -> {
                if (this.seal.getSeal() instanceof ISealConfigToggles cp) {
                    int n = cp.getToggles().length;
                    int s = n < 4 ? 8 : (n < 6 ? 7 : (n < 9 ? 6 : 5));
                    int h = (n - 1) * s;
                    int w = 12;
                    for (ISealConfigToggles.SealToggle prop : cp.getToggles()) {
                        int ww = 12 + Math.min(100, this.font.width(Component.translatable(prop.getName())));
                        ww /= 2;
                        if (ww > w) {
                            w = ww;
                        }
                    }
                    int p = 0;
                    for (ISealConfigToggles.SealToggle prop : cp.getToggles()) {
                        final int index = p;
                        this.addRenderableWidget(new GuiGolemButton.Prop(gl + this.middleX - w, gt + this.middleY - 5 - h + p * s * 2, prop.getName(), prop::getValue,
                                () -> this.send(prop.getValue() ? 60 + index : 30 + index)));
                        p++;
                    }
                }
            }
            case 4 -> {
                EnumGolemTrait[] tags = this.seal.getSeal().getRequiredTags();
                if (tags != null) {
                    int p = 0;
                    for (EnumGolemTrait tag : tags) {
                        this.addRenderableWidget(new GuiHoverButton(gl + this.middleX + p * 18 - (tags.length - 1) * 9, gt + this.middleY - 8, 16, 16,
                                tag.getLocalizedName(), tag.getLocalizedDescription(), tag.icon));
                        p++;
                    }
                }
                tags = this.seal.getSeal().getForbiddenTags();
                if (tags != null) {
                    int p = 0;
                    for (EnumGolemTrait tag : tags) {
                        this.addRenderableWidget(new GuiHoverButton(gl + this.middleX + p * 18 - (tags.length - 1) * 9, gt + this.middleY + 24, 16, 16,
                                tag.getLocalizedName(), tag.getLocalizedDescription(), tag.icon));
                        p++;
                    }
                }
            }
            default -> {
            }
        }
    }

    private void selectCategory(int index) {
        if (this.categories[index] != this.category) {
            this.category = this.categories[index];
            this.menu.setCategoryClient(this.category);
            this.send(index);
            this.setupCategories();
        }
    }

    /** Цифры 1–9 не перекладывают предметы в слоты фильтра (checkHotbarKeys оригинала). */
    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        return false;
    }

    private static String colorKey(DyeColor c) {
        return "color." + (c == DyeColor.LIGHT_GRAY ? "silver" : c.getName());
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mouseX, int mouseY) {
        int gl = this.leftPos;
        int gt = this.topPos;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEX, gl + this.middleX - 80, gt + this.middleY - 80, 96, 0, 160, 160);
        g.blit(TEX, gl, gt + 143, 0, 167, 176, 89);
        g.drawCenteredString(this.font, Component.translatable("button.category." + this.category), gl + this.middleX, gt + this.middleY - 64, 16777215);
        switch (this.category) {
            case 0 -> {
                g.blit(TEX, gl + this.middleX + 17, gt + this.middleY + 3, 2, 18, 12, 12);
                if (this.seal.getColor() >= 1 && this.seal.getColor() <= 16) {
                    int c = DyeColor.byId(this.seal.getColor() - 1).getTextureDiffuseColor();
                    RenderSystem.setShaderColor((c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F, 1.0F);
                    g.blit(TEX, gl + this.middleX + 20, gt + this.middleY + 6, 74, 31, 6, 6);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
                int mx = mouseX - gl;
                int my = mouseY - gt;
                if (mx >= this.middleX + 5 && mx <= this.middleX + 41 && my >= this.middleY + 3 && my <= this.middleY + 15) {
                    Component text = this.seal.getColor() >= 1 && this.seal.getColor() <= 16
                            ? Component.translatable("golem.prop.color", Component.translatable(colorKey(DyeColor.byId(this.seal.getColor() - 1))))
                            : Component.translatable("golem.prop.colorall");
                    g.drawCenteredString(this.font, text, gl + this.middleX + 23, gt + this.middleY + 17, 16777215);
                }
                g.drawCenteredString(this.font, Component.translatable("golem.prop.priority"), gl + this.middleX, gt + this.middleY - 28, 12299007);
                g.drawCenteredString(this.font, "" + this.seal.getPriority(), gl + this.middleX, gt + this.middleY - 16, 16777215);
                if (this.isOwner()) {
                    g.drawCenteredString(this.font, Component.translatable("golem.prop.owner"), gl + this.middleX, gt + this.middleY + 32, 12299007);
                }
            }
            case 1 -> {
                if (this.seal.getSeal() instanceof ISealConfigFilter f) {
                    int s = f.getFilterSize();
                    int sx = 16 + (s - 1) % 3 * 12;
                    int sy = 16 + (s - 1) / 3 * 12;
                    for (int a = 0; a < s; a++) {
                        int x = a % 3;
                        int y = a / 3;
                        g.blit(TEX, gl + this.middleX + x * 24 - sx, gt + this.middleY + y * 24 - sy, 0, 56, 32, 32);
                    }
                }
            }
            case 2 -> {
                g.drawCenteredString(this.font, Component.translatable("button.caption.y"), gl + this.middleX, gt + this.middleY - 24 - 9, 14540253);
                g.drawCenteredString(this.font, Component.translatable("button.caption.x"), gl + this.middleX, gt + this.middleY - 9, 14540253);
                g.drawCenteredString(this.font, Component.translatable("button.caption.z"), gl + this.middleX, gt + this.middleY + 24 - 9, 14540253);
                g.drawCenteredString(this.font, "" + this.seal.getArea().getY(), gl + this.middleX, gt + this.middleY - 24, 16777215);
                g.drawCenteredString(this.font, "" + this.seal.getArea().getX(), gl + this.middleX, gt + this.middleY, 16777215);
                g.drawCenteredString(this.font, "" + this.seal.getArea().getZ(), gl + this.middleX, gt + this.middleY + 24, 16777215);
            }
            case 4 -> {
                g.drawCenteredString(this.font, Component.translatable("button.caption.required"), gl + this.middleX, gt + this.middleY - 26, 14540253);
                g.drawCenteredString(this.font, Component.translatable("button.caption.forbidden"), gl + this.middleX, gt + this.middleY + 6, 14540253);
            }
            default -> {
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        if (this.category == 1 && this.seal.getSeal() instanceof ISealConfigFilter cp && !cp.isBlacklist()) {
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 200.0F);
            for (int i1 = 0; i1 < this.menu.filterSlots; i1++) {
                Slot slot = this.menu.slots.get(i1);
                if (slot.isActive() && !slot.getItem().isEmpty()) {
                    int sz = cp.getFilterSlotSize(i1);
                    String s = sz == 0 ? ChatFormatting.GOLD + "*" : String.valueOf(sz);
                    g.drawString(this.font, s, this.leftPos + slot.x + 19 - 2 - this.font.width(s), this.topPos + slot.y + 6 + 3, 16777215, true);
                }
            }
            g.pose().popPose();
        }
        for (GuiEventListener l : this.children()) {
            if (l instanceof GuiHoverButton b && b.isMouseOver(mouseX, mouseY)) {
                b.drawButtonForegroundLayer(this, g, mouseX, mouseY);
                break;
            }
        }
        this.renderTooltip(g, mouseX, mouseY);
    }
}
