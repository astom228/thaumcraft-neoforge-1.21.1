package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.client.gui.plugins.GuiImageButton;
import thaumcraft.client.gui.plugins.GuiScrollButton;
import thaumcraft.client.gui.plugins.GuiSliderTC;
import thaumcraft.common.container.ContainerLogistics;
import thaumcraft.common.entities.construct.golem.gui.GuiGolemButton;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.misc.PacketLogisticsRequestToServer;
import thaumcraft.common.lib.network.misc.PacketMiscStringToServer;

/**
 * Окно логистики (порт GuiLogistics): витрина предметов печатей снабжения с прокруткой и поиском; выбранный предмет —
 * количество ползунком или «−/+», кнопка «Заказать». Список раз в секунду обновляется с сервера.
 */
public class GuiLogistics extends AbstractContainerScreen<ContainerLogistics> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_logistics.png");
    static final ResourceLocation TEXBASE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_base.png");
    int selectedSlot = -1;
    final ContainerLogistics con;
    long lu = 0L;
    int lastStackSize = 1;
    int stackSize = 1;
    boolean stacksizeUpdated = false;
    ItemStack selectedStack = ItemStack.EMPTY;
    int lastScrollPos = 0;
    GuiSliderTC scrollbar;
    GuiSliderTC countbar;
    GuiGolemButton.PlusMinus countbutton1;
    GuiGolemButton.PlusMinus countbutton2;
    GuiImageButton requestbutton;
    EditBox searchField;

    public GuiLogistics(ContainerLogistics menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.con = menu;
        this.imageWidth = 215;
        this.imageHeight = 215;
    }

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    @Override
    protected void init() {
        super.init();
        int gl = this.leftPos;
        int gt = this.topPos;
        this.addRenderableWidget(new GuiScrollButton(gl + 195, gt + 16, 10, 10, true, true, () -> this.send(1)));
        this.addRenderableWidget(new GuiScrollButton(gl + 195, gt + 180, 10, 10, false, true, () -> this.send(0)));
        this.countbutton1 = this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + 13, gt + 195, true, () -> this.changeCount(-1)));
        this.countbutton2 = this.addRenderableWidget(new GuiGolemButton.PlusMinus(gl + 57, gt + 195, false, () -> this.changeCount(1)));
        this.scrollbar = this.addRenderableWidget(new GuiSliderTC(gl + 196, gt + 28, 8, 149, 0.0F, this.con.end, this.con.start, true));
        this.countbar = this.addRenderableWidget(new GuiSliderTC(gl + 24, gt + 196, 32, 8, 1.0F, 64.0F, 1.0F, false));
        this.requestbutton = this.addRenderableWidget(new GuiImageButton(gl + 116, gt + 200, 40, 13, "tc.logistics.request", null, TEXBASE,
                37, 82, 40, 13, 16777215, this::request));
        this.searchField = new EditBox(this.font, gl + 143, gt + 196, 55, this.font.lineHeight, Component.empty());
        this.searchField.setMaxLength(10);
        this.searchField.setBordered(true);
        this.searchField.setTextColor(16777215);
        this.searchField.setCanLoseFocus(true);
        this.searchField.setResponder(s -> PacketDistributor.sendToServer(new PacketMiscStringToServer(0, s)));
        this.addRenderableWidget(this.searchField);
    }

    private ItemStack selectedSlotStack() {
        if (this.selectedSlot >= 0 && this.selectedSlot < ContainerLogistics.SIZE) {
            return this.menu.getSlot(this.selectedSlot).getItem();
        }
        return ItemStack.EMPTY;
    }

    private void changeCount(int d) {
        ItemStack stack = this.selectedSlotStack();
        if (stack.isEmpty()) {
            return;
        }
        this.stackSize = Math.max(1, Math.min(stack.getCount(), this.stackSize + d));
        this.stacksizeUpdated = true;
    }

    private void request() {
        ItemStack stack = this.selectedSlotStack();
        if (!stack.isEmpty()) {
            ItemStack s2 = stack.copy();
            s2.setCount(1);
            PacketDistributor.sendToServer(new PacketLogisticsRequestToServer(s2, this.stackSize));
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        long ct = System.currentTimeMillis();
        if (ct > this.lu) {
            this.lu = ct + 1000L;
            this.send(22);
        }
        if (this.scrollbar.getMax() != this.con.end) {
            this.scrollbar.setMax(this.con.end);
        }
        int sv = Math.round(this.scrollbar.getSliderValue());
        if (sv != this.lastScrollPos) {
            this.lastScrollPos = sv;
            this.send(100 + this.lastScrollPos);
        } else if (this.con.updated && sv != this.con.start) {
            this.scrollbar.setSliderValue(this.con.start);
            this.con.updated = false;
        }
        if (this.selectedSlot >= 0 && !this.selectedStack.isEmpty() && !ItemStack.isSameItemSameComponents(this.selectedStack, this.selectedSlotStack())) {
            this.selectedSlot = -1;
            for (Slot slot : this.menu.slots) {
                if (ItemStack.isSameItemSameComponents(this.selectedStack, slot.getItem())) {
                    this.selectedSlot = slot.index;
                    break;
                }
            }
        }
        if (this.selectedSlot >= 0 && this.selectedSlotStack().isEmpty()) {
            this.selectedSlot = -1;
        }
        boolean vis = this.selectedSlot >= 0;
        this.countbar.visible = vis;
        this.countbutton1.visible = vis;
        this.countbutton2.visible = vis;
        this.requestbutton.visible = vis;
        ItemStack stack = this.selectedSlotStack();
        if (!stack.isEmpty()) {
            if (this.countbar.getMax() != stack.getCount()) {
                this.countbar.setMax(stack.getCount());
            }
            int cv = Math.round(this.countbar.getSliderValue());
            if (cv != this.lastStackSize) {
                this.lastStackSize = cv;
                this.stackSize = cv;
            } else if (this.stacksizeUpdated && cv != this.stackSize) {
                this.countbar.setSliderValue(this.stackSize);
                this.stacksizeUpdated = false;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (sy < 0) {
            this.send(0);
        } else if (sy > 0) {
            this.send(1);
        }
        return true;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        super.slotClicked(slot, slotId, button, type);
        if (slot != null && slotId >= 0 && slotId < ContainerLogistics.SIZE && slot.hasItem() && this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundsTC.clack.get(), 0.66F, 1.0F);
            this.selectedSlot = slotId;
            this.selectedStack = slot.getItem().copy();
        }
    }

    @Override
    protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.searchField.isFocused() && key != 256) {
            this.searchField.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        super.render(g, mouseX, mouseY, partialTicks);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEX, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (this.selectedSlot >= 0) {
            g.blit(TEX, this.leftPos + 17 + this.selectedSlot % 9 * 19, this.topPos + 17 + this.selectedSlot / 9 * 19, 222, 46, 20, 20);
        }
        if (!this.searchField.isFocused() && this.searchField.getValue().isEmpty()) {
            g.drawString(this.font, Component.translatable("tc.logistics.search"), this.leftPos + 146, this.topPos + 197, 2236962, false);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        if (this.selectedSlot >= 0 && !this.selectedSlotStack().isEmpty()) {
            String s = "" + this.stackSize;
            g.drawString(this.font, s, 83 - this.font.width(s) / 2, 196, 3355443, false);
        }
    }
}
