package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.casters.FocusEffect;
import thaumcraft.api.casters.FocusEngine;
import thaumcraft.api.casters.FocusMedium;
import thaumcraft.api.casters.FocusMediumRoot;
import thaumcraft.api.casters.FocusModSplit;
import thaumcraft.api.casters.FocusNode;
import thaumcraft.api.casters.IFocusElement;
import thaumcraft.api.casters.NodeSetting;
import thaumcraft.client.gui.plugins.GuiImageButton;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.container.ContainerFocalManipulator;
import thaumcraft.common.items.casters.ItemFocus;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.playerdata.PacketFocusNameToServer;
import thaumcraft.common.lib.network.playerdata.PacketFocusNodesToServer;
import thaumcraft.common.lib.utils.InventoryUtils;
import thaumcraft.common.tiles.crafting.FocusElementNode;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

/**
 * Экран фокусного манипулятора (порт GuiFocalManipulator): дерево узлов заклинания (корень сверху, ветки вниз),
 * слева — подходящие элементы для выбранного пустого места, справа — настройки выбранного узла, сложность, опыт, Вис,
 * нужные кристаллы; поле имени фокуса. Каждое изменение дерева уходит на сервер (он проверяет). Правый щелчок по узлу
 * — заменить его на родительский элемент (как в оригинале).
 */
public class GuiFocalManipulator extends AbstractContainerScreen<ContainerFocalManipulator> {
    private static final ResourceLocation TEX = tc("textures/gui/gui_wandtable.png");
    private static final ResourceLocation TEX2 = tc("textures/gui/gui_wandtable2.png");
    private static final ResourceLocation TEX3 = tc("textures/gui/gui_wandtable3.png");
    private static final ResourceLocation TEXBASE = tc("textures/gui/gui_base.png");
    static final ResourceLocation I_MEDIUM = tc("textures/foci/_medium.png");
    static final ResourceLocation I_EFFECT = tc("textures/foci/_effect.png");
    private final TileFocalManipulator table;
    private GuiImageButton buttonConfirm;
    private EditBox nameField;
    private final List<HoverIcon> hoverIcons = new ArrayList<>();
    private final List<Spinner> spinners = new ArrayList<>();
    Slider scrollbarParts;
    Slider scrollbarMainSide;
    Slider scrollbarMainBottom;
    int totalComplexity = 0;
    int maxComplexity = 0;
    int lastNodeHover = -1;
    final DecimalFormat myFormatter = new DecimalFormat("#######.##");
    final ArrayList<String> shownParts = new ArrayList<>();
    int partsStart = 0;
    ItemStack[] components = null;
    boolean valid = false;
    private int nodeID = 0;
    int sMinX = 0;
    int sMinY = 0;
    int sMaxX = 0;
    int sMaxY = 0;
    int selectedNode = -1;
    float costCast = 0.0F;
    int costXp = 0;
    int costVis = 0;
    int scrollX = 0;
    int scrollY = 0;
    /** Перетаскивание поля узлов левой кнопкой (в оригинале — опрос Mouse.isButtonDown). */
    private boolean draggingField = false;

    public GuiFocalManipulator(ContainerFocalManipulator menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.table = menu.getTile();
        this.imageWidth = 231;
        this.imageHeight = 231;
    }

    private static ResourceLocation tc(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }

    @Override
    protected void init() {
        super.init();
        this.buttonConfirm = new GuiImageButton(this.leftPos + 242, this.topPos + 18, 24, 16, null, "wandtable.text3", TEXBASE, 232, 240, 24, 16, 0xFFFFFF,
                () -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0));
        this.addRenderableWidget(this.buttonConfirm);
        this.nameField = new EditBox(this.font, this.leftPos + 30, this.topPos + 11, 170, 12, Component.empty());
        this.nameField.setTextColor(-1);
        this.nameField.setTextColorUneditable(-1);
        this.nameField.setBordered(false);
        this.nameField.setMaxLength(50);
        if (this.table.focusName.isEmpty() && !this.table.getStackInSlot(0).isEmpty()) {
            this.table.focusName = this.table.getStackInSlot(0).getHoverName().getString();
        }
        this.nameField.setValue(this.table.focusName);
        this.nameField.setResponder(s -> {
            if (!s.equals(this.table.focusName)) {
                this.table.focusName = s;
                PacketDistributor.sendToServer(new PacketFocusNameToServer(this.table.getBlockPos(), s));
            }
        });
        this.gatherInfo(false);
    }

    // --- отрисовка -----------------------------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int gx = this.leftPos;
        int gy = this.topPos;
        int count = 0;
        int index = 0;
        for (String sk : this.shownParts) {
            if (++count - 1 >= this.partsStart) {
                FocusNode node = (FocusNode) FocusEngine.getElement(sk);
                g.pose().pushPose();
                g.pose().translate(0.0, 0.0, 5.0);
                drawPart(g, node, gx + 38, 43 + gy + 25 * index, node.getType() == IFocusElement.EnumUnitType.MOD ? 24.0F : 32.0F,
                        this.isPointInRegion(gx + 38 - 10, 32 + gy + 24 * index, 20, 20, mx + this.leftPos, my + this.topPos));
                g.pose().popPose();
                if (++index > 5) {
                    break;
                }
            }
        }
        count = 0;
        index = 0;
        for (String sk : this.shownParts) {
            if (++count - 1 >= this.partsStart) {
                FocusNode node = (FocusNode) FocusEngine.getElement(sk);
                if (this.isPointInRegion(gx + 38 - 10, 33 + gy + 25 * index, 20, 20, mx + this.leftPos, my + this.topPos)) {
                    this.drawHoveringTextFixed(g, this.genPartText(node, -1), gx + 44, 46 + gy + 24 * index, this.width - (this.leftPos + this.imageWidth - 16));
                }
                if (++index > 5) {
                    break;
                }
            }
        }
        if (this.lastNodeHover >= 0) {
            FocusElementNode fn = this.table.data.get(this.lastNodeHover);
            if (fn != null && fn.node != null) {
                int xx = this.leftPos + 140 - this.scrollX + fn.x * 24;
                int yy = this.topPos + 50 - this.scrollY + fn.y * 32;
                this.drawHoveringTextFixed(g, this.genPartText(fn.node, this.lastNodeHover), xx, yy, this.width - (this.leftPos + this.imageWidth - 16));
            }
        }
        this.buttonConfirm.active = this.table.vis <= 0.0F && this.valid;
        if (this.table.data != null && !this.table.data.isEmpty()) {
            // The frame in renderBg is at z=1; draw editable text above it.
            g.pose().pushPose();
            g.pose().translate(0.0, 0.0, 2.0);
            this.nameField.render(g, mx, my, pt);
            g.pose().popPose();
        }
        // подсказки кнопок-значков (drawButtonForegroundLayer)
        for (HoverIcon h : this.hoverIcons) {
            if (h.isMouseOver(mx, my)) {
                h.renderTooltip(g, mx, my);
                break;
            }
        }
        this.renderTooltip(g, mx, my);
    }

    private List<Component> genPartText(FocusNode node, int idx) {
        List<Component> list = new ArrayList<>();
        if (node == null) {
            return list;
        }
        FocusElementNode placed = idx >= 0 ? this.table.data.get(idx) : null;
        list.add(Component.translatable(node.getUnlocalizedName()));
        list.add(Component.translatable(node.getUnlocalizedText()).withStyle(ChatFormatting.DARK_PURPLE));
        int c = node.getComplexity();
        if (placed != null) {
            c = (int) (node.getComplexity() * placed.complexityMultiplier);
        }
        list.add(Component.translatable("focuspart.com").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" " + c).withStyle(placed != null && placed.complexityMultiplier > 1.0F ? ChatFormatting.RED : ChatFormatting.GOLD)));
        float p = placed != null ? placed.getPower(this.table.data) : node.getPowerMultiplier();
        if (p != 1.0F) {
            list.add(Component.translatable("focuspart.eff").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(" x" + this.myFormatter.format(p)).withStyle(p < 1.0F ? ChatFormatting.RED : ChatFormatting.GREEN)));
        }
        if (node instanceof FocusEffect fe) {
            float d = fe.getDamageForDisplay(placed == null ? 1.0F : placed.getPower(this.table.data));
            if (d > 0.0F) {
                list.add(Component.translatable("attribute.modifier.equals.0", this.myFormatter.format(d),
                        Component.translatable("attribute.name.generic.attack_damage")).withStyle(ChatFormatting.DARK_RED));
            } else if (d < 0.0F) {
                list.add(Component.translatable("attribute.modifier.equals.0", this.myFormatter.format(-d), Component.translatable("focus.heal.power"))
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
        }
        return list;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // в оригинале подписей нет; значения ползунков переносятся в прокрутку
        if (this.scrollbarMainSide != null) {
            this.scrollY = Math.round(this.scrollbarMainSide.getSliderValue());
        }
        if (this.scrollbarMainBottom != null) {
            this.scrollX = Math.round(this.scrollbarMainBottom.getSliderValue());
        }
        if (this.scrollbarParts != null) {
            this.partsStart = Math.round(this.scrollbarParts.getSliderValue());
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int k = this.leftPos;
        int l = this.topPos;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEX2, k, l, 0, 0, this.imageWidth, this.imageHeight);
        g.blit(TEX3, k - 71, l - 3, 0, 0, 71, 239);
        if (this.table.getStackInSlot(0).isEmpty() || this.table.doGuiReset) {
            if (this.table.doGuiReset) {
                this.resetNodes();
            } else {
                this.table.data.clear();
                this.gatherInfo(false);
            }
            this.table.focusName = "";
            if (!this.table.getStackInSlot(0).isEmpty()) {
                this.table.focusName = this.table.getStackInSlot(0).getHoverName().getString();
                this.nameField.setValue(this.table.focusName);
            }
            this.table.doGuiReset = false;
        }
        if (this.table.doGather) {
            this.gatherInfo(false);
            this.table.doGather = false;
        }
        this.drawNodes(g, this.leftPos + 132 - this.scrollX, this.topPos + 48 - this.scrollY, mx - this.leftPos, my - this.topPos);
        g.pose().pushPose();
        g.pose().translate(0.0, 0.0, 1.0);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(TEX, k, l, 0, 0, this.imageWidth, this.imageHeight);
        if (this.maxComplexity > 0) {
            g.drawString(this.font, this.totalComplexity + "/" + this.maxComplexity, k + 242, l + 36,
                    this.totalComplexity > this.maxComplexity ? 16151160 : 16761407);
        }
        g.drawString(this.font, "" + this.costXp, k + 242, l + 50, this.costXp > this.minecraft.player.experienceLevel ? 16151160 : 10092429);
        int v = this.costVis;
        if (this.table.vis > 0.0F) {
            v = (int) this.table.vis;
        }
        g.drawString(this.font, ChatFormatting.AQUA + "" + v, k + 242, l + 64, 10092429);
        if (this.costCast > 0.0F) {
            g.drawString(this.font, Component.translatable("item.Focus.cost1").withStyle(ChatFormatting.AQUA)
                    .append(": " + this.myFormatter.format(this.costCast)), k + 230, l + 80, 10092429);
        }
        if (this.components != null && this.components.length > 0) {
            g.drawString(this.font, Component.translatable("wandtable.text4").withStyle(ChatFormatting.GOLD), k + 230, l + 92, 10092429);
        }
        if (this.table.focusName != null && !this.table.data.isEmpty()) {
            g.blit(TEXBASE, this.leftPos + 24, this.topPos + 8, 192, 224, 8, 14);
            int a;
            for (a = 1; a < 22; a++) {
                g.blit(TEXBASE, this.leftPos + 24 + a * 8, this.topPos + 8, 200, 224, 8, 14);
            }
            g.blit(TEXBASE, this.leftPos + 24 + a * 8, this.topPos + 8, 208, 224, 8, 14);
        }
        for (HoverIcon h : this.hoverIcons) {
            h.render(g, mx, my);
        }
        for (Spinner s : this.spinners) {
            s.render(g, mx, my);
        }
        for (Slider s : new Slider[] {this.scrollbarParts, this.scrollbarMainSide, this.scrollbarMainBottom}) {
            if (s != null) {
                s.render(g, mx, my);
            }
        }
        g.pose().popPose();
    }

    private void drawClippedRect(GuiGraphics g, int x, int y, int sx, int sy, int w, int h) {
        if (this.isPointInRegion(48, 26, 166, 174, x + w / 2, y + h / 2)) {
            g.blit(TEX, x, y, sx, sy, w, h);
        }
    }

    /** mx, my — относительно окна (как в drawGuiContainerBackgroundLayer оригинала). */
    private void drawNodes(GuiGraphics g, int x, int y, int mx, int my) {
        if (this.table.data == null || this.table.data.isEmpty()) {
            return;
        }
        int hover = -1;
        for (FocusElementNode fn : this.table.data.values()) {
            int xx = x + fn.x * 24;
            int yy = y + fn.y * 32;
            boolean mouseover = this.isPointInRegion(63, 31, 136, 160, mx + this.leftPos, my + this.topPos)
                    && this.isPointInRegion(xx - this.leftPos - 10, yy - this.topPos - 10, 20, 20, mx + this.leftPos, my + this.topPos);
            if (mouseover && fn.parent >= 0) {
                hover = fn.id;
            }
            if (fn.node != null) {
                if (this.isPointInRegion(48, 16, 154, 192, xx - 8, yy - 8)) {
                    drawPart(g, fn.node, xx, yy, 32.0F, mouseover);
                }
            } else {
                this.drawClippedRect(g, xx - 12, yy - 12, 120, 232, 24, 24);
            }
            if (this.selectedNode == fn.id || mouseover && fn.parent >= 0) {
                this.drawClippedRect(g, xx - 12, yy - 12, 96, 232, 24, 24);
            }
            FocusElementNode parent = this.table.data.get(fn.parent);
            if (parent != null) {
                this.drawClippedRect(g, xx - 6, yy - 22, 54, 232, 12, 12);
                if (parent.node instanceof FocusModSplit) {
                    int q = Math.abs(fn.x - parent.x);
                    for (int a = 0; a < q; a++) {
                        if (fn.x < parent.x) {
                            if (a == 0) {
                                this.drawClippedRect(g, xx - 4, yy - 36, 8, 240, 16, 16);
                            } else {
                                this.drawClippedRect(g, xx - 12 + a * 24, yy - 36, 72, 240, 24, 16);
                            }
                        } else if (a == 0) {
                            this.drawClippedRect(g, xx - 12, yy - 36, 24, 240, 16, 16);
                        } else {
                            this.drawClippedRect(g, xx - 12 - a * 24, yy - 36, 72, 240, 24, 16);
                        }
                    }
                }
                if (fn.node == null) {
                    int s = parent.target && parent.trajectory ? 4 : 0;
                    if (this.isPointInRegion(48, 16, 168, 192, xx - 4, yy - 4)) {
                        if (parent.target) {
                            g.pose().pushPose();
                            g.pose().translate(xx - s, yy, 0.0);
                            g.pose().scale(0.5F, 0.5F, 0.5F);
                            g.blit(TEX, -8, -8, 152, 240, 16, 16);
                            g.pose().popPose();
                        }
                        if (parent.trajectory) {
                            g.pose().pushPose();
                            g.pose().translate(xx + s, yy, 0.0);
                            g.pose().scale(0.5F, 0.5F, 0.5F);
                            g.blit(TEX, -8, -8, 168, 240, 16, 16);
                            g.pose().popPose();
                        }
                    }
                }
            }
        }
        if (hover >= 0 && this.lastNodeHover != hover) {
            this.playRollover();
            this.lastNodeHover = hover;
        }
        if (hover < 0) {
            this.lastNodeHover = -1;
        }
        if (this.selectedNode >= 0) {
            this.drawNodeSettings(g, this.selectedNode);
        }
    }

    private void drawNodeSettings(GuiGraphics g, int selectedNode2) {
        FocusElementNode fn = this.table.data.get(selectedNode2);
        if (fn == null || fn.node == null || fn.node.getSettingList().isEmpty()) {
            return;
        }
        int a = 0;
        for (String sk : fn.node.getSettingList()) {
            NodeSetting setting = fn.node.getSetting(sk);
            if (setting.getResearch() == null || ThaumcraftCapabilities.knowsResearchStrict(this.minecraft.player, setting.getResearch())) {
                int x = this.leftPos + this.imageWidth;
                int y = this.topPos + this.imageHeight - 10 - fn.node.getSettingList().size() * 26 + a * 26;
                g.drawString(this.font, ChatFormatting.GOLD + setting.getLocalizedName(), x, y, 0xFFFFFF);
                a++;
            }
        }
    }

    /** Значок элемента: подложка цвета элемента (эффект / носитель) и картинка; корень и модификаторы — вдвое крупнее, без подложки. */
    public static void drawPart(GuiGraphics g, FocusNode node, int x, int y, float scale, boolean mouseover) {
        drawPart(g, node, x, y, scale, mouseover, 1.0F);
    }

    /** shade < 1 — затемнённый значок (неизученное исследование в книге; в 1.12 — яркость 50 вместо 220). */
    public static void drawPart(GuiGraphics g, FocusNode node, int x, int y, float scale, boolean mouseover, float shade) {
        boolean root = node.getType() == IFocusElement.EnumUnitType.MOD || node.getKey().equals("ROOT");
        if (root) {
            scale *= 2.0F;
        }
        Color color = new Color(FocusEngine.getElementColor(node.getKey()));
        float r = color.getRed() / 255.0F * shade;
        float gr = color.getGreen() / 255.0F * shade;
        float b = color.getBlue() / 255.0F * shade;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (node.getType() == IFocusElement.EnumUnitType.EFFECT) {
            quad(g, I_EFFECT, x, y, (float) (scale * 0.9 + (mouseover ? 2 : 0)), r, gr, b);
        } else if (node.getType() == IFocusElement.EnumUnitType.MEDIUM && !root) {
            quad(g, I_MEDIUM, x, y, (float) (scale * 0.9 + (mouseover ? 2 : 0)), r, gr, b);
        }
        g.pose().pushPose();
        g.pose().translate(0.0, 0.0, 1.0);
        quad(g, FocusEngine.getElementIcon(node.getKey()), x, y, scale / 2.0F + (mouseover ? 2 : 0), shade, shade, shade);
        g.pose().popPose();
    }

    /** UtilsFX.renderQuadCentered в интерфейсе: вся текстура квадратом size с центром (x, y), оттенок (r, g, b). */
    private static void quad(GuiGraphics g, ResourceLocation tex, int x, int y, float size, float r, float gr, float b) {
        if (tex == null) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x - size / 2.0F, y - size / 2.0F, 0.0F);
        g.pose().scale(size / 16.0F, size / 16.0F, 1.0F);
        RenderSystem.setShaderColor(r, gr, b, 1.0F);
        g.blit(tex, 0, 0, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();
    }

    /** drawHoveringTextFixed: подсказка с переносом по ширине textWidth (как у ванильной — со сдвигом +12, −12). */
    private void drawHoveringTextFixed(GuiGraphics g, List<Component> lines, int x, int y, int textWidth) {
        if (lines.isEmpty()) {
            return;
        }
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component c : lines) {
            out.addAll(this.font.split(c, Math.max(20, textWidth)));
        }
        g.pose().pushPose();
        g.pose().translate(0.0, 0.0, 300.0);
        g.renderTooltip(this.font, out, x, y);
        g.pose().popPose();
    }

    /** isPointInRegion GuiContainer 1.12: точка (px, py) минус угол окна — в прямоугольнике с полем 1. */
    private boolean isPointInRegion(int left, int top, int w, int h, int px, int py) {
        px -= this.leftPos;
        py -= this.topPos;
        return px >= left - 1 && px < left + w + 1 && py >= top - 1 && py < top + h + 1;
    }

    // --- ввод -----------------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double dmx, double dmy, int button) {
        int mx = (int) dmx;
        int my = (int) dmy;
        // Input may arrive before render has updated lastNodeHover for the new cursor position.
        this.lastNodeHover = -1;
        if (this.isPointInRegion(63, 31, 136, 160, mx, my)) {
            for (FocusElementNode node : this.table.data.values()) {
                int x = 132 - this.scrollX + node.x * 24;
                int y = 48 - this.scrollY + node.y * 32;
                if (node.parent >= 0 && this.isPointInRegion(x - 10, y - 10, 20, 20, mx, my)) {
                    this.lastNodeHover = node.id;
                    break;
                }
            }
        }
        for (Slider s : new Slider[] {this.scrollbarParts, this.scrollbarMainSide, this.scrollbarMainBottom}) {
            if (s != null && s.mousePressed(mx, my)) {
                return true;
            }
        }
        for (Spinner s : this.spinners) {
            if (s.mousePressed(mx, my)) {
                this.playButtonClick();
                this.gatherInfo(true);
                return true;
            }
        }
        if (button == 0 && this.isPointInRegion(this.leftPos + 63, this.topPos + 31, 136, 160, mx + this.leftPos, my + this.topPos)) {
            this.draggingField = true;
        }
        boolean handled = super.mouseClicked(dmx, dmy, button);
        int gx = this.leftPos;
        int gy = this.topPos;
        if (this.table.vis <= 0.0F && this.table.data != null && !this.table.data.isEmpty()) {
            if (this.table.data != null && !this.table.data.isEmpty()) {
                this.nameField.mouseClicked(dmx, dmy, button);
                this.nameField.setFocused(this.nameField.isMouseOver(dmx, dmy));
            }
            if (this.lastNodeHover >= 0) {
                this.selectedNode = this.lastNodeHover;
                if (button == 1 && this.table.data.get(this.selectedNode).node != null) {
                    FocusElementNode fn = this.table.data.get(this.selectedNode);
                    FocusElementNode par = this.table.data.get(fn.parent);
                    if (par != null && par.node != null) {
                        this.addNodeAt(par.node.getKey(), fn.parent, true);
                    }
                }
                this.gatherInfo(false);
                this.playButtonClick();
            }
            int count = 0;
            int index = 0;
            if (this.selectedNode >= 0) {
                for (String sk : this.shownParts) {
                    if (++count - 1 >= this.partsStart) {
                        if (this.isPointInRegion(gx + 28, gy + 32 + 24 * index, 20, 20, mx + this.leftPos, my + this.topPos)) {
                            this.addNodeAt(sk, this.selectedNode, true);
                            return true;
                        }
                        if (++index > 5) {
                            break;
                        }
                    }
                }
            }
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        for (Slider s : new Slider[] {this.scrollbarParts, this.scrollbarMainSide, this.scrollbarMainBottom}) {
            if (s != null && s.isMouseDown) {
                s.drag((int) mx, (int) my);
                return true;
            }
        }
        if (this.draggingField && button == 0) {
            if (this.scrollbarMainBottom != null) {
                this.scrollX = (int) (this.scrollX - dx);
                this.scrollX = (int) Math.max(this.scrollbarMainBottom.getMin(), Math.min(this.scrollbarMainBottom.getMax(), this.scrollX));
                this.scrollbarMainBottom.setSliderValue(this.scrollX);
            }
            if (this.scrollbarMainSide != null) {
                this.scrollY = (int) (this.scrollY - dy);
                this.scrollY = (int) Math.max(this.scrollbarMainSide.getMin(), Math.min(this.scrollbarMainSide.getMax(), this.scrollY));
                this.scrollbarMainSide.setSliderValue(this.scrollY);
            }
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.draggingField = false;
        for (Slider s : new Slider[] {this.scrollbarParts, this.scrollbarMainSide, this.scrollbarMainBottom}) {
            if (s != null) {
                s.isMouseDown = false;
            }
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (this.scrollbarParts != null && this.shownParts.size() > 6 && this.isPointInRegion(24, 24, 32, 157, (int) mx, (int) my)) {
            if (sy > 0) {
                if (this.partsStart > 0) {
                    this.partsStart--;
                    this.scrollbarParts.setSliderValue(this.partsStart);
                }
            } else if (sy < 0 && this.partsStart < this.shownParts.size() - 6) {
                this.partsStart++;
                this.scrollbarParts.setSliderValue(this.partsStart);
            }
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.nameField.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return this.nameField.keyPressed(keyCode, scanCode, modifiers) || this.nameField.canConsumeInput() || super.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (this.nameField.isFocused() && this.nameField.charTyped(c, modifiers)) {
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    private void playButtonClick() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundsTC.clack.get(), 1.0F, 0.4F));
    }

    private void playRollover() {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundsTC.clack.get(), 2.0F, 0.4F));
    }

    // --- дерево ---------------------------------------------------------------------------------------------------

    private int getNextId() {
        while (this.table.data.containsKey(this.nodeID)) {
            this.nodeID++;
        }
        return this.nodeID;
    }

    private void cullChildren(int idx) {
        if (this.table.data.containsKey(idx)) {
            for (int i : this.table.data.get(idx).children) {
                this.cullChildren(i);
                this.table.data.remove(i);
            }
        }
    }

    /** addNodeAt оригинала (по ключу элемента вместо класса): поставить элемент на место idx, дописать пустые места-потомки. */
    private ArrayList<Integer> addNodeAt(String key, int idx, boolean gather) {
        ArrayList<Integer> ret = new ArrayList<>();
        boolean same = false;
        FocusElementNode previous = null;
        if (this.table.data.containsKey(idx)) {
            this.cullChildren(idx);
            FocusNode cur = this.table.data.get(idx).node;
            if (cur != null && cur.getKey().equals(key)) {
                same = true;
            } else {
                previous = this.table.data.remove(idx);
            }
        }
        FocusElementNode fn;
        FocusNode node;
        if (!same) {
            Supplier<? extends IFocusElement> factory = FocusEngine.elements.get(key);
            if (factory == null || !(factory.get() instanceof FocusNode created)) {
                return ret;
            }
            fn = new FocusElementNode();
            node = created;
            fn.node = node;
            if (previous != null) {
                fn.y = previous.y;
                fn.x = previous.x;
            }
            fn.id = this.getNextId();
            ret.add(fn.id);
            this.selectedNode = fn.id;
            if (previous != null && this.table.data.containsKey(previous.parent)) {
                fn.parent = previous.parent;
                int[] c = this.table.data.get(previous.parent).children;
                for (int a = 0; a < c.length; a++) {
                    if (c[a] == previous.id) {
                        c[a] = fn.id;
                        break;
                    }
                }
            }
            fn.target = node.canSupply(FocusNode.EnumSupplyType.TARGET);
            fn.trajectory = node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);
            this.table.data.put(this.nodeID, fn);
        } else {
            fn = this.table.data.get(idx);
            node = fn.node;
        }
        if (fn.target || fn.trajectory) {
            if (node instanceof FocusModSplit) {
                FocusElementNode blank1 = new FocusElementNode();
                blank1.parent = fn.id;
                blank1.id = this.getNextId();
                ret.add(this.nodeID);
                blank1.x = fn.x - 1;
                blank1.y = fn.y + 1;
                this.table.data.put(this.nodeID, blank1);
                this.selectedNode = this.nodeID;
                FocusElementNode blank2 = new FocusElementNode();
                blank2.parent = fn.id;
                blank2.x = fn.x + 1;
                blank2.y = fn.y + 1;
                blank2.id = this.getNextId();
                ret.add(this.nodeID);
                this.table.data.put(this.nodeID, blank2);
                fn.children = new int[] {blank1.id, blank2.id};
            } else {
                FocusElementNode blank = new FocusElementNode();
                blank.parent = fn.id;
                blank.x = fn.x;
                blank.y = fn.y + 1;
                blank.id = this.getNextId();
                ret.add(this.nodeID);
                this.table.data.put(this.nodeID, blank);
                fn.children = new int[] {blank.id};
                this.selectedNode = this.nodeID;
            }
        }
        if (gather) {
            this.calcNodeTreeLayout();
            this.gatherInfo(true);
        }
        return ret;
    }

    private void processLeftNodes(FocusElementNode start, int[] bounds) {
        if (start.children.length > 0) {
            this.processLeftNodes(this.table.data.get(start.children[0]), bounds);
        }
        int ox = 0;
        if (start.children.length == 1) {
            ox = bounds[0] - 1;
            bounds[0] = this.table.data.get(start.children[0]).x;
        }
        start.x = bounds[0];
        if (start.children.length == 1) {
            bounds[0] = ox;
        }
        bounds[0]++;
        if (start.children.length > 1) {
            this.processLeftNodes(this.table.data.get(start.children[1]), bounds);
        }
    }

    private void moveNodes(FocusElementNode start, int amt) {
        for (int ci : start.children) {
            this.moveNodes(this.table.data.get(ci), amt);
        }
        start.x -= amt;
    }

    private void calcNodeTreeLayout() {
        int fsi = -1;
        for (FocusElementNode node : this.table.data.values()) {
            if (fsi < 0 && node.node instanceof FocusModSplit) {
                fsi = node.id;
            }
        }
        if (fsi >= 0) {
            int[] bounds = {0};
            this.processLeftNodes(this.table.data.get(fsi), bounds);
            this.moveNodes(this.table.data.get(fsi), bounds[0] / 2);
        }
        for (FocusElementNode node : this.table.data.values()) {
            if (node.node instanceof FocusModSplit) {
                FocusElementNode p = this.table.data.get(node.parent);
                if (p != null && p.node != null && !(p.node instanceof FocusModSplit)) {
                    node.x = p.x;
                } else {
                    int xx = 0;
                    for (int a : node.children) {
                        xx += this.table.data.get(a).x;
                    }
                    xx /= node.children.length;
                    node.x = xx;
                }
            }
        }
        if (this.selectedNode >= 0 && !this.table.data.containsKey(this.selectedNode)) {
            this.selectedNode = -1;
        }
    }

    private void resetNodes() {
        this.nodeID = 0;
        this.table.data.clear();
        ArrayList<Integer> r1 = this.addNodeAt("ROOT", 0, false);
        this.selectedNode = r1.size() > 1 ? r1.get(1) : -1;
        this.calcNodeTreeLayout();
        this.gatherInfo(true);
    }

    private void calcScrollBounds() {
        this.sMinX = 0;
        this.sMinY = 0;
        this.sMaxX = 0;
        this.sMaxY = 0;
        for (FocusElementNode fn : this.table.data.values()) {
            this.sMinX = Math.min(this.sMinX, fn.x);
            this.sMinY = Math.min(this.sMinY, fn.y);
            this.sMaxX = Math.max(this.sMaxX, fn.x);
            this.sMaxY = Math.max(this.sMaxY, fn.y);
        }
    }

    private void gatherPartsList() {
        this.shownParts.clear();
        if (this.minecraft == null || this.selectedNode < 0 || !this.table.data.containsKey(this.selectedNode)) {
            return;
        }
        this.partsStart = 0;
        ArrayList<String> pMed = new ArrayList<>();
        ArrayList<String> pEff = new ArrayList<>();
        ArrayList<String> pMod = new ArrayList<>();
        boolean hasExlusive = false;
        boolean hasMedium = false;
        for (FocusElementNode fn : this.table.data.values()) {
            if (fn.node instanceof FocusMedium m) {
                hasMedium = !(fn.node instanceof FocusMediumRoot);
                if (m.isExclusive()) {
                    hasExlusive = true;
                    break;
                }
            }
        }
        FocusElementNode node = this.table.data.get(this.selectedNode);
        FocusElementNode parent = this.table.data.get(node.parent);
        if (parent != null && parent.node != null) {
            for (String key : FocusEngine.elements.keySet()) {
                IFocusElement element = FocusEngine.getElement(key);
                if (element == null || !ThaumcraftCapabilities.knowsResearchStrict(this.minecraft.player, element.getResearch()) || element.getKey().equals("ROOT")
                        || !(element instanceof FocusNode fn) || fn.mustBeSupplied() == null) {
                    continue;
                }
                for (FocusNode.EnumSupplyType type : fn.mustBeSupplied()) {
                    if (parent.node.canSupply(type)) {
                        switch (element.getType()) {
                            case EFFECT -> pEff.add(key);
                            case MEDIUM -> {
                                if (!hasExlusive && (!((FocusMedium) element).isExclusive() || !hasMedium)) {
                                    pMed.add(key);
                                }
                            }
                            case MOD -> pMod.add(key);
                            default -> {
                            }
                        }
                        break;
                    }
                }
            }
        }
        Collections.sort(pMed);
        Collections.sort(pEff);
        Collections.sort(pMod);
        this.shownParts.addAll(pMed);
        this.shownParts.addAll(pEff);
        this.shownParts.addAll(pMod);
    }

    private void gatherInfo(boolean sync) {
        this.hoverIcons.clear();
        this.spinners.clear();
        this.buttonConfirm.setX(this.leftPos + 242);
        this.buttonConfirm.setY(this.topPos + 18);
        this.hoverIcons.add(new HoverIcon(this.leftPos + 241, this.topPos + 39, 32, 16, Component.translatable("wandtable.text5"), tc("textures/gui/complex.png")));
        this.hoverIcons.add(new HoverIcon(this.leftPos + 241, this.topPos + 53, 32, 16, Component.translatable("wandtable.text2"), tc("textures/gui/costxp.png")));
        this.hoverIcons.add(new HoverIcon(this.leftPos + 241, this.topPos + 67, 32, 16, Component.translatable("wandtable.text1"), tc("textures/gui/costvis.png")));
        FocusElementNode fn = this.table.data.get(this.selectedNode);
        if (fn != null && fn.node != null && !fn.node.getSettingList().isEmpty()) {
            int a = 0;
            for (String sk : fn.node.getSettingList()) {
                NodeSetting setting = fn.node.getSetting(sk);
                if (setting.getResearch() == null || ThaumcraftCapabilities.knowsResearchStrict(this.minecraft.player, setting.getResearch())) {
                    int x = this.leftPos + this.imageWidth;
                    int y = this.topPos + this.imageHeight + 3 - fn.node.getSettingList().size() * 26 + a * 26;
                    int w = setting.getType() instanceof NodeSetting.NodeSettingIntList ? 72 : 32;
                    this.spinners.add(new Spinner(x, y, w, setting));
                    a++;
                }
            }
        }
        this.shownParts.clear();
        this.components = null;
        ItemStack is = this.menu.getSlot(0).getItem();
        if (is.isEmpty()) {
            return;
        }
        HashMap<String, Integer> compCount = new HashMap<>();
        this.totalComplexity = 0;
        this.maxComplexity = is.getItem() instanceof ItemFocus f ? f.getMaxComplexity() : 0;
        boolean emptyNodes = false;
        AspectList crystals = new AspectList();
        if (this.table.data != null) {
            for (FocusElementNode node : this.table.data.values()) {
                if (node.node != null) {
                    int a = compCount.getOrDefault(node.node.getKey(), 0);
                    node.complexityMultiplier = 0.5F * (++a + 1);
                    compCount.put(node.node.getKey(), a);
                    this.totalComplexity = (int) (this.totalComplexity + node.node.getComplexity() * node.complexityMultiplier);
                    if (node.node.getAspect() != null) {
                        crystals.add(node.node.getAspect(), 1);
                    }
                } else {
                    emptyNodes = true;
                }
            }
        }
        this.costCast = this.totalComplexity / 5.0F;
        this.costVis = this.totalComplexity * 10 + this.maxComplexity / 5;
        this.costXp = (int) Math.min(1L, Math.round(Math.sqrt(this.totalComplexity)));
        boolean validCrystals = false;
        if (crystals.getAspects().length > 0) {
            validCrystals = true;
            this.components = new ItemStack[crystals.getAspects().length];
            int r = 0;
            for (Aspect as : crystals.getAspects()) {
                this.components[r++] = ThaumcraftApiHelper.makeCrystal(as, crystals.getAmount(as));
            }
            for (ItemStack c : this.components) {
                if (!InventoryUtils.isPlayerCarryingAmount(this.minecraft.player, c, false)) {
                    validCrystals = false;
                }
            }
            int i = 0;
            int q = 0;
            for (ItemStack stack : this.components) {
                this.hoverIcons.add(new HoverIcon(this.leftPos + 234 + i * 16, this.topPos + 110 + 16 * q, 16, 16, stack.getHoverName(), stack));
                if (++i > 4) {
                    i = 0;
                    q++;
                }
            }
        }
        this.gatherPartsList();
        this.scrollbarParts = null;
        if (this.shownParts.size() > 6) {
            this.scrollbarParts = new Slider(this.leftPos + 51, this.topPos + 30, 8, 149, 0.0F, this.shownParts.size() - 6, 0.0F, true);
        }
        this.valid = this.totalComplexity <= this.maxComplexity && !emptyNodes && validCrystals && this.table.xpCost <= this.minecraft.player.experienceLevel;
        this.calcScrollBounds();
        if (this.scrollY > (this.sMaxY - 3) * 32) {
            this.scrollY = (this.sMaxY - 3) * 32;
        }
        if (this.scrollX > this.sMaxX * 24) {
            this.scrollX = this.sMaxX * 24;
        }
        if (this.scrollX < this.sMinX * 24) {
            this.scrollX = this.sMinX * 24;
        }
        this.scrollbarMainSide = null;
        this.scrollbarMainBottom = null;
        if (this.sMaxY * 32 > 130) {
            this.scrollbarMainSide = new Slider(this.leftPos + 203, this.topPos + 32, 8, 156, 0.0F, (this.sMaxY - 3) * 32, this.scrollY, true);
        } else {
            this.scrollY = 0;
        }
        if (this.sMinX * 24 >= -70 && this.sMaxX * 24 <= 70) {
            this.scrollX = 0;
        } else {
            this.scrollbarMainBottom = new Slider(this.leftPos + 64, this.topPos + 195, 132, 8, this.sMinX * 24, this.sMaxX * 24, this.scrollX, false);
        }
        if (sync) {
            PacketDistributor.sendToServer(new PacketFocusNodesToServer(this.table.getBlockPos(), new HashMap<>(this.table.data), this.table.focusName));
        }
    }

    // --- элементы управления (порт GuiHoverButton, GuiSliderTC, GuiFocusSettingSpinnerButton) ---------------------------

    /** Значок с подсказкой (GuiHoverButton): x, y — центр; не нажимается. */
    private class HoverIcon {
        final int x;
        final int y;
        final int w;
        final int h;
        final Component text;
        final Object tex;

        HoverIcon(int x, int y, int w, int h, Component text, Object tex) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.text = text;
            this.tex = tex;
        }

        boolean isMouseOver(int mx, int my) {
            return mx >= this.x - this.w / 2 && my >= this.y - this.h / 2 && mx < this.x - this.w / 2 + this.w && my < this.y - this.h / 2 + this.h;
        }

        void render(GuiGraphics g, int mx, int my) {
            boolean hov = this.isMouseOver(mx, my);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (this.tex instanceof ResourceLocation rl) {
                float c = hov ? 1.0F : 0.9F;
                RenderSystem.setShaderColor(c, c, c, hov ? 1.0F : 0.9F);
                g.blit(rl, this.x - this.w / 2, this.y - this.h / 2, 0, 0, 16, 16, 16, 16);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else if (this.tex instanceof ItemStack stack) {
                UtilsFX.renderItemStackShaded(g, stack, this.x - this.w / 2, this.y - this.h / 2 - (hov ? 1 : 0), 1.0F);
            }
        }

        void renderTooltip(GuiGraphics g, int mx, int my) {
            List<String> lines = new ArrayList<>();
            if (this.tex instanceof ItemStack stack) {
                for (Component c : stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(GuiFocalManipulator.this.minecraft.level),
                        GuiFocalManipulator.this.minecraft.player, TooltipFlag.Default.NORMAL)) {
                    lines.add(c.getString());
                }
            } else {
                lines.add(this.text.getString());
            }
            UtilsFX.drawCustomTooltip(g, GuiFocalManipulator.this.font, lines, mx + 4, my + 8, -99);
        }
    }

    /** Ползунок (GuiSliderTC): дорожка и бегунок из gui_base. */
    private static class Slider {
        float sliderPosition;
        boolean isMouseDown;
        final float min;
        final float max;
        final boolean vertical;
        final int x;
        final int y;
        final int w;
        final int h;

        Slider(int x, int y, int w, int h, float min, float max, float def, boolean vertical) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.min = min;
            this.max = max;
            this.sliderPosition = max == min ? 0.0F : (def - min) / (max - min);
            this.vertical = vertical;
        }

        float getMax() {
            return this.max;
        }

        float getMin() {
            return this.min;
        }

        float getSliderValue() {
            return this.min + (this.max - this.min) * this.sliderPosition;
        }

        void setSliderValue(float value) {
            this.sliderPosition = this.max == this.min ? 0.0F : (value - this.min) / (this.max - this.min);
        }

        boolean mousePressed(int mx, int my) {
            if (mx >= this.x && my >= this.y && mx < this.x + this.w && my < this.y + this.h) {
                this.drag(mx, my);
                this.isMouseDown = true;
                return true;
            }
            return false;
        }

        void drag(int mx, int my) {
            this.sliderPosition = this.vertical ? (float) (my - (this.y + 4)) / (this.h - 8) : (float) (mx - (this.x + 4)) / (this.w - 8);
            this.sliderPosition = Math.max(0.0F, Math.min(1.0F, this.sliderPosition));
        }

        void render(GuiGraphics g, int mx, int my) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.pose().pushPose();
            if (this.vertical) {
                g.pose().translate(this.x + 2, this.y, 0.0F);
                g.pose().scale(1.0F, this.h / 32.0F, 1.0F);
                g.blit(TEXBASE, 0, 0, 240, 176, 4, 32);
            } else {
                g.pose().translate(this.x, this.y + 2, 0.0F);
                g.pose().scale(this.w / 32.0F, 1.0F, 1.0F);
                g.blit(TEXBASE, 0, 0, 208, 176, 32, 4);
            }
            g.pose().popPose();
            if (this.vertical) {
                g.blit(TEXBASE, this.x, this.y + (int) (this.sliderPosition * (this.h - 8)), 20, 20, 8, 8);
            } else {
                g.blit(TEXBASE, this.x + (int) (this.sliderPosition * (this.w - 8)), this.y, 20, 20, 8, 8);
            }
        }
    }

    /** Переключатель настройки узла (GuiFocusSettingSpinnerButton): стрелки по краям, значение посередине. */
    private class Spinner {
        final int x;
        final int y;
        final int w;
        final NodeSetting setting;

        Spinner(int x, int y, int w, NodeSetting setting) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.setting = setting;
        }

        void render(GuiGraphics g, int mx, int my) {
            boolean hov = mx >= this.x && my >= this.y && mx < this.x + this.w + 10 && my < this.y + 10;
            float c = hov ? 1.0F : 0.9F;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(c, c, c, c);
            g.blit(TEXBASE, this.x, this.y, 20, 0, 10, 10);
            g.blit(TEXBASE, this.x + this.w, this.y, 30, 0, 10, 10);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            String s = this.setting.getValueText();
            g.drawString(GuiFocalManipulator.this.font, s, this.x + (this.w + 10) / 2 - GuiFocalManipulator.this.font.width(s) / 2, this.y + 1, 0xFFFFFF);
        }

        boolean mousePressed(int mx, int my) {
            if (mx >= this.x && my >= this.y && mx < this.x + 10 && my < this.y + 10) {
                this.setting.decrement();
                return true;
            }
            if (mx >= this.x + this.w && my >= this.y && mx < this.x + this.w + 10 && my < this.y + 10) {
                this.setting.increment();
                return true;
            }
            return false;
        }
    }
}
