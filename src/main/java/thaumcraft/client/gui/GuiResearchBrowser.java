package thaumcraft.client.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.config.ConfigResearch;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.network.playerdata.PacketSyncResearchFlagsToServer;
import thaumcraft.common.lib.research.ResearchManager;

/**
 * Карта исследований Таумономикона (порт GuiResearchBrowser): вкладки категорий (свои — слева, других модов — справа,
 * с прокруткой), перетаскивание карты, масштаб колесом (1–2), поиск по исследованиям, категориям и рецептам.
 * Состояние вкладки, позиции и поиска — статическое, как в оригинале (только клиент).
 */
public class GuiResearchBrowser extends Screen {
    private static int guiBoundsLeft;
    private static int guiBoundsTop;
    private static int guiBoundsRight;
    private static int guiBoundsBottom;
    /** Крайние колонки/ряды видимых исследований раздела и центр карты (в координатах карты при текущем зуме). */
    private static int mapMinCol;
    private static int mapMaxCol;
    private static int mapMinRow;
    private static int mapMaxRow;
    private static int guiCenterX;
    private static int guiCenterY;
    /** Раздел целиком помещается в поле по ширине / высоте (при текущем зуме). */
    private static boolean mapFitsX;
    private static boolean mapFitsY;
    protected int mouseX = 0;
    protected int mouseY = 0;
    protected float screenZoom = 1.0F;
    protected double curMouseX;
    protected double curMouseY;
    protected double guiMapX;
    protected double guiMapY;
    protected double tempMapX;
    protected double tempMapY;
    private int isMouseButtonDown = 0;
    public static double lastX = -9999.0;
    public static double lastY = -9999.0;
    private int screenX;
    private int screenY;
    private final int startX = 16;
    private final int startY = 16;
    long t = 0L;
    private final LinkedList<ResearchEntry> research = new LinkedList<>();
    static String selectedCategory = null;
    private ResearchEntry currentHighlight = null;
    private final Player player;
    long popuptime = 0L;
    String popupmessage = "";
    private EditBox searchField;
    private static boolean searching = false;
    private final ArrayList<String> categoriesTC = new ArrayList<>();
    private final ArrayList<String> categoriesOther = new ArrayList<>();
    static int catScrollPos = 0;
    static int catScrollMax = 0;
    public int addonShift = 0;
    private final ArrayList<String> invisible = new ArrayList<>();
    final ArrayList<SearchEntry> searchResults = new ArrayList<>();
    static final ResourceLocation tx1 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/gui/gui_research_browser.png");
    static final ResourceLocation nodeTexture = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/auranodes.png");
    /** Размеры текстур значков (в оригинале запрашивались у OpenGL на каждом кадре). */
    private static final Map<ResourceLocation, int[]> ICON_SIZES = new ConcurrentHashMap<>();

    public GuiResearchBrowser() {
        this(lastX, lastY);
    }

    public GuiResearchBrowser(double x, double y) {
        super(Component.empty());
        this.curMouseX = this.guiMapX = this.tempMapX = x;
        this.curMouseY = this.guiMapY = this.tempMapY = y;
        this.player = Minecraft.getInstance().player;
    }

    /**
     * initGui + setWorldAndResolution оригинала (вызывается и при смене размера окна / масштаба интерфейса):
     * перестроить вкладки и поставить карту на место.
     */
    @Override
    protected void init() {
        this.updateResearch();
        this.placeMap(lastX == -9999.0, lastY == -9999.0);
    }

    public void updateResearch() {
        this.clearWidgets();
        this.addRenderableWidget(new GuiSearchButton(1, this.height - 17, 16, 16, Component.translatable("tc.search")));
        this.searchField = new EditBox(this.font, 20, 20, 89, this.font.lineHeight, Component.empty());
        this.searchField.setMaxLength(15);
        this.searchField.setBordered(true);
        this.searchField.setVisible(false);
        this.searchField.setTextColor(16777215);
        this.searchField.setResponder(s -> this.updateSearch());
        if (searching) {
            this.searchField.setVisible(true);
            this.searchField.setCanLoseFocus(false);
            this.searchField.setFocused(true);
            this.setFocused(this.searchField);
            this.searchField.setValue("");
            this.updateSearch();
        }
        this.addWidget(this.searchField);
        this.screenX = this.width - 32;
        this.screenY = this.height - 32;
        this.research.clear();
        if (selectedCategory == null && !ResearchCategories.researchCategories.isEmpty()) {
            selectedCategory = ResearchCategories.researchCategories.keySet().iterator().next();
        }
        int limit = (int) Math.floor((this.screenY - 28) / 24.0F);
        this.addonShift = 0;
        int count = 0;
        this.categoriesTC.clear();
        this.categoriesOther.clear();
        for (String rcl : ResearchCategories.researchCategories.keySet()) {
            int rt = 0;
            int rco = 0;
            for (ResearchEntry res : ResearchCategories.getResearchCategory(rcl).research.values()) {
                if (!res.hasMeta(ResearchEntry.EnumResearchMeta.AUTOUNLOCK)) {
                    rt++;
                    if (ThaumcraftCapabilities.knowsResearch(this.player, res.getKey())) {
                        rco++;
                    }
                }
            }
            int v = (int) ((float) rco / rt * 100.0F);
            ResearchCategory rc = ResearchCategories.getResearchCategory(rcl);
            if (rc.researchKey == null || ThaumcraftCapabilities.knowsResearchStrict(this.player, rc.researchKey)) {
                if (Arrays.asList(ConfigResearch.TCCategories).contains(rcl)) {
                    this.categoriesTC.add(rcl);
                    this.addRenderableWidget(new GuiCategoryButton(rc, rcl, false, 1, 10 + this.categoriesTC.size() * 24, 16, 16,
                            I18n.get("tc.research_category." + rcl), v));
                    continue;
                }
                count++;
                if (count <= limit + catScrollPos && count - 1 >= catScrollPos) {
                    this.categoriesOther.add(rcl);
                    this.addRenderableWidget(new GuiCategoryButton(rc, rcl, true, this.width - 17, 10 + this.categoriesOther.size() * 24, 16, 16,
                            I18n.get("tc.research_category." + rcl), v));
                }
            }
        }
        if (count > limit || count < catScrollPos) {
            this.addonShift = (this.screenY - 28) % 24 / 2;
            this.addRenderableWidget(new GuiScrollButton(false, this.width - 14, 20, 10, 11, () -> {
                if (catScrollPos > 0) {
                    catScrollPos--;
                    this.updateResearch();
                }
            }));
            this.addRenderableWidget(new GuiScrollButton(true, this.width - 14, this.screenY + 1, 10, 11, () -> {
                if (catScrollPos < catScrollMax) {
                    catScrollPos++;
                    this.updateResearch();
                }
            }));
        }
        catScrollMax = count - limit;
        if (selectedCategory != null && !selectedCategory.isEmpty() && ResearchCategories.getResearchCategory(selectedCategory) != null) {
            this.research.addAll(ResearchCategories.getResearchCategory(selectedCategory).research.values());
            mapMinCol = Integer.MAX_VALUE;
            mapMaxCol = Integer.MIN_VALUE;
            mapMinRow = Integer.MAX_VALUE;
            mapMaxRow = Integer.MIN_VALUE;
            for (ResearchEntry res : this.research) {
                if (res != null && this.isVisible(res)) {
                    mapMinCol = Math.min(mapMinCol, res.getDisplayColumn());
                    mapMaxCol = Math.max(mapMaxCol, res.getDisplayColumn());
                    mapMinRow = Math.min(mapMinRow, res.getDisplayRow());
                    mapMaxRow = Math.max(mapMaxRow, res.getDisplayRow());
                }
            }
            if (mapMinCol > mapMaxCol) {
                mapMinCol = mapMaxCol = mapMinRow = mapMaxRow = 0;
            }
            this.recalcBounds();
        }
    }

    /**
     * Границы прокрутки карты (крайнее исследование не уходит дальше края поля) и центр — в координатах карты с учётом зума.
     * Карта рисуется со смещением startX и масштабом 1/zoom, поле в этих координатах — от startX·zoom шириной screenX·zoom;
     * значок исследования занимает 32 точки от (колонка·24 − 8). В оригинале зум в границах не учитывался (DEVIATIONS.md).
     */
    private void recalcBounds() {
        double z = this.screenZoom;
        double fw = this.screenX * z;
        double fh = this.screenY * z;
        double ox = this.startX * (z - 1.0);
        double oy = this.startY * (z - 1.0);
        guiBoundsLeft = Mth.floor(mapMinCol * 24 - fw + 48 - ox);
        guiBoundsRight = Mth.floor(mapMaxCol * 24 - 24 - ox);
        guiBoundsTop = Mth.floor(mapMinRow * 24 - fh + 48 - oy);
        guiBoundsBottom = Mth.floor(mapMaxRow * 24 - 24 - oy);
        int cx = Mth.floor((mapMinCol + mapMaxCol) * 12 + 8 - fw / 2.0 - ox);
        int cy = Mth.floor((mapMinRow + mapMaxRow) * 12 + 8 - fh / 2.0 - oy);
        guiBoundsRight = Math.max(guiBoundsRight, guiBoundsLeft + 1);
        guiBoundsBottom = Math.max(guiBoundsBottom, guiBoundsTop + 1);
        guiCenterX = Mth.clamp(cx, guiBoundsLeft, guiBoundsRight - 1);
        guiCenterY = Mth.clamp(cy, guiBoundsTop, guiBoundsBottom - 1);
        mapFitsX = (mapMaxCol - mapMinCol) * 24 + 32 <= fw;
        mapFitsY = (mapMaxRow - mapMinRow) * 24 + 32 <= fh;
    }

    /**
     * Положение карты при открытии, смене размера окна/масштаба и зума. В оригинале восстанавливалась сохранённая позиция,
     * если она попадала в границы, — а у небольшого раздела границы охватывают всё поле, и раздел оказывался где угодно
     * (DEVIATIONS.md). Теперь по каждой оси: раздел помещается в поле — по центру; нет — сохранённая позиция в границах.
     */
    private void placeMap(boolean centerX, boolean centerY) {
        this.guiMapX = this.tempMapX = this.curMouseX = centerX || mapFitsX ? guiCenterX
                : Mth.clamp(this.guiMapX, guiBoundsLeft, guiBoundsRight - 1);
        this.guiMapY = this.tempMapY = this.curMouseY = centerY || mapFitsY ? guiCenterY
                : Mth.clamp(this.guiMapY, guiBoundsTop, guiBoundsBottom - 1);
    }

    private boolean isVisible(ResearchEntry res) {
        if (ThaumcraftCapabilities.knowsResearch(this.player, res.getKey())) {
            return true;
        }
        if (!this.invisible.contains(res.getKey()) && (!res.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN) || this.canUnlockResearch(res))) {
            if (res.getParents() == null && res.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN)) {
                return false;
            }
            if (res.getParents() != null) {
                for (String r : res.getParents()) {
                    ResearchEntry ri = ResearchCategories.getResearch(r);
                    if (ri != null && !this.isVisible(ri)) {
                        this.invisible.add(r);
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean canUnlockResearch(ResearchEntry res) {
        return ResearchManager.doesPlayerHaveRequisites(this.player, res.getKey());
    }

    @Override
    public void removed() {
        lastX = this.guiMapX;
        lastY = this.guiMapY;
        super.removed();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searching && this.searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (!searching && this.minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searching && this.searchField.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void updateSearch() {
        this.searchResults.clear();
        this.invisible.clear();
        String s1 = this.searchField.getValue().toLowerCase(Locale.ROOT);
        for (String cat : this.categoriesTC) {
            if (cat.toLowerCase(Locale.ROOT).contains(s1)) {
                this.searchResults.add(new SearchEntry(I18n.get("tc.research_category." + cat), new SearchResult(cat, null, true)));
            }
        }
        for (String cat : this.categoriesOther) {
            if (cat.toLowerCase(Locale.ROOT).contains(s1)) {
                this.searchResults.add(new SearchEntry(I18n.get("tc.research_category." + cat), new SearchResult(cat, null, true)));
            }
        }
        ArrayList<ResourceLocation> dupCheck = new ArrayList<>();
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(this.player);
        for (String pre : knowledge.getResearchList()) {
            ResearchEntry ri = ResearchCategories.getResearch(pre);
            if (ri == null) {
                continue;
            }
            String name = I18n.get(ri.getName());
            if (name.toLowerCase(Locale.ROOT).contains(s1)) {
                this.searchResults.add(new SearchEntry(name, new SearchResult(pre, null)));
            }
            int stage = knowledge.getResearchStage(pre);
            if (ri.getStages() != null) {
                int s = Math.min(ri.getStages().length - 1, stage + 1);
                ResearchStage page = ri.getStages()[s];
                if (page != null && page.getRecipes() != null) {
                    for (ResourceLocation rec : page.getRecipes()) {
                        if (!dupCheck.contains(rec)) {
                            dupCheck.add(rec);
                            ItemStack ro = ResearchRecipes.getRecipeOutput(rec);
                            if (ro != null && !ro.isEmpty() && ro.getHoverName().getString().toLowerCase(Locale.ROOT).contains(s1)) {
                                this.searchResults.add(new SearchEntry(ro.getHoverName().getString(), new SearchResult(pre, rec)));
                            }
                        }
                    }
                }
            }
        }
        this.searchResults.sort(Comparator.comparing((SearchEntry e) -> e.name).thenComparing(e -> e.result));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!searching) {
            if (scrollY < 0) {
                this.screenZoom += 0.25F;
            } else if (scrollY > 0) {
                this.screenZoom -= 0.25F;
            }
            this.screenZoom = Mth.clamp(this.screenZoom, 1.0F, 2.0F);
            this.recalcBounds();
            this.placeMap(false, false);
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float par3) {
        if (!searching) {
            if (!this.minecraft.mouseHandler.isLeftPressed()) {
                this.isMouseButtonDown = 0;
            } else {
                if ((this.isMouseButtonDown == 0 || this.isMouseButtonDown == 1) && mx >= this.startX && mx < this.startX + this.screenX
                        && my >= this.startY && my < this.startY + this.screenY) {
                    if (this.isMouseButtonDown == 0) {
                        this.isMouseButtonDown = 1;
                    } else {
                        this.guiMapX = this.guiMapX - (double) (mx - this.mouseX) * this.screenZoom;
                        this.guiMapY = this.guiMapY - (double) (my - this.mouseY) * this.screenZoom;
                        this.tempMapX = this.curMouseX = this.guiMapX;
                        this.tempMapY = this.curMouseY = this.guiMapY;
                    }
                    this.mouseX = mx;
                    this.mouseY = my;
                }
                if (this.tempMapX < (double) guiBoundsLeft) {
                    this.tempMapX = (double) guiBoundsLeft;
                }
                if (this.tempMapY < (double) guiBoundsTop) {
                    this.tempMapY = (double) guiBoundsTop;
                }
                if (this.tempMapX >= (double) guiBoundsRight) {
                    this.tempMapX = guiBoundsRight - 1.0;
                }
                if (this.tempMapY >= (double) guiBoundsBottom) {
                    this.tempMapY = guiBoundsBottom - 1.0;
                }
            }
        }
        this.renderTransparentBackground(g);
        this.t = System.nanoTime() / 50000000L;
        // доля тика, как partialTicks в 1.12 (экраны 1.21 получают в render прошедшие тики за кадр)
        float pt = this.minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        int locX = Mth.floor(this.curMouseX + (this.guiMapX - this.curMouseX) * pt);
        int locY = Mth.floor(this.curMouseY + (this.guiMapY - this.curMouseY) * pt);
        if (locX < guiBoundsLeft) {
            locX = guiBoundsLeft;
        }
        if (locY < guiBoundsTop) {
            locY = guiBoundsTop;
        }
        if (locX >= guiBoundsRight) {
            locX = guiBoundsRight - 1;
        }
        if (locY >= guiBoundsBottom) {
            locY = guiBoundsBottom - 1;
        }
        if (!searching) {
            // карта рисуется только внутри поля под рамкой; значки-предметы выводятся из буфера до рамки и вкладок
            g.enableScissor(this.startX - 2, this.startY - 2, this.startX + this.screenX + 2, this.startY + this.screenY + 2);
            g.pose().pushPose();
            g.pose().scale(1.0F / this.screenZoom, 1.0F / this.screenZoom, 1.0F);
            this.genResearchBackgroundZoomable(g, mx, my, locX, locY);
            g.pose().popPose();
            g.flush();
            g.disableScissor();
        } else {
            this.searchField.render(g, mx, my, par3);
            int q = 0;
            for (SearchEntry p : this.searchResults) {
                SearchResult sr = p.result;
                int color = sr.cat ? 14527146 : (sr.recipe == null ? 14540253 : 11184861);
                if (sr.recipe != null) {
                    RenderSystem.enableBlend();
                    g.pose().pushPose();
                    g.pose().scale(0.5F, 0.5F, 0.5F);
                    g.blit(tx1, 44, (32 + q * 10) * 2, 224, 48, 16, 16);
                    g.pose().popPose();
                }
                if (mx > 22 && mx < 18 + this.screenX && my >= 32 + q * 10 && my < 40 + q * 10) {
                    color = sr.recipe == null ? 16777215 : (sr.cat ? 16764108 : 13421823);
                }
                g.drawString(this.font, p.name, 32, 32 + q * 10, color, false);
                if (32 + (++q + 1) * 10 > this.screenY) {
                    g.drawString(this.font, I18n.get("tc.search.more"), 22, 34 + q * 10, 11184810, false);
                    break;
                }
            }
        }
        this.genResearchBackgroundFixedPost(g, mx, my, par3);
        if (this.popuptime > System.currentTimeMillis()) {
            List<String> text = new ArrayList<>();
            text.add(this.popupmessage);
            UtilsFX.drawCustomTooltip(g, this.font, text, 10, 34, -99);
        }
    }

    @Override
    public void tick() {
        this.curMouseX = this.guiMapX;
        this.curMouseY = this.guiMapY;
        double var1 = this.tempMapX - this.guiMapX;
        double var3 = this.tempMapY - this.guiMapY;
        if (var1 * var1 + var3 * var3 < 4.0) {
            this.guiMapX += var1;
            this.guiMapY += var3;
        } else {
            this.guiMapX += var1 * 0.85;
            this.guiMapY += var3 * 0.85;
        }
    }

    protected void genResearchBackgroundZoomable(GuiGraphics g, int mx, int my, int locX, int locY) {
        ResearchCategory cat = ResearchCategories.getResearchCategory(selectedCategory);
        if (cat == null) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, cat.background);
        this.drawTexturedModalRectWithDoubles(g, (this.startX - 2) * this.screenZoom, (this.startY - 2) * this.screenZoom, locX / 2.0, locY / 2.0,
                (this.screenX + 4) * this.screenZoom, (this.screenY + 4) * this.screenZoom);
        if (cat.background2 != null) {
            RenderSystem.setShaderTexture(0, cat.background2);
            this.drawTexturedModalRectWithDoubles(g, (this.startX - 2) * this.screenZoom, (this.startY - 2) * this.screenZoom, locX / 1.5,
                    locY / 1.5, (this.screenX + 4) * this.screenZoom, (this.screenY + 4) * this.screenZoom);
        }
        RenderSystem.enableDepthTest();
        IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(this.player);
        for (ResearchEntry source : this.research) {
            if (source.getParents() != null && source.getParents().length > 0) {
                for (int a = 0; a < source.getParents().length; a++) {
                    ResearchEntry parent = source.getParents()[a] != null ? ResearchCategories.getResearch(source.getParentsClean()[a]) : null;
                    if (parent != null && parent.getCategory().equals(selectedCategory)
                            && (parent.getSiblings() == null || !Arrays.asList(parent.getSiblings()).contains(source.getKey()))) {
                        boolean knowsParent = ThaumcraftCapabilities.knowsResearchStrict(this.player, source.getParents()[a]);
                        boolean b = this.isVisible(source) && !source.getParents()[a].startsWith("~");
                        if (b) {
                            if (knowsParent) {
                                this.drawLine(g, source.getDisplayColumn(), source.getDisplayRow(), parent.getDisplayColumn(), parent.getDisplayRow(),
                                        0.6F, 0.6F, 0.6F, locX, locY, 3.0F, true, source.hasMeta(ResearchEntry.EnumResearchMeta.REVERSE));
                            } else if (this.isVisible(parent)) {
                                this.drawLine(g, source.getDisplayColumn(), source.getDisplayRow(), parent.getDisplayColumn(), parent.getDisplayRow(),
                                        0.2F, 0.2F, 0.2F, locX, locY, 2.0F, true, source.hasMeta(ResearchEntry.EnumResearchMeta.REVERSE));
                            }
                        }
                    }
                }
            }
            if (source.getSiblings() != null && source.getSiblings().length > 0) {
                for (int a = 0; a < source.getSiblings().length; a++) {
                    ResearchEntry sibling = source.getSiblings()[a] != null ? ResearchCategories.getResearch(source.getSiblings()[a]) : null;
                    if (sibling != null && sibling.getCategory().equals(selectedCategory)) {
                        boolean knowsSibling = ThaumcraftCapabilities.knowsResearchStrict(this.player, sibling.getKey());
                        if (this.isVisible(source) && !source.getSiblings()[a].startsWith("~")) {
                            if (knowsSibling) {
                                this.drawLine(g, sibling.getDisplayColumn(), sibling.getDisplayRow(), source.getDisplayColumn(), source.getDisplayRow(),
                                        0.3F, 0.3F, 0.4F, locX, locY, 1.0F, false, source.hasMeta(ResearchEntry.EnumResearchMeta.REVERSE));
                            } else if (this.isVisible(sibling)) {
                                this.drawLine(g, sibling.getDisplayColumn(), sibling.getDisplayRow(), source.getDisplayColumn(), source.getDisplayRow(),
                                        0.1875F, 0.1875F, 0.25F, locX, locY, 0.0F, false, source.hasMeta(ResearchEntry.EnumResearchMeta.REVERSE));
                            }
                        }
                    }
                }
            }
        }
        this.currentHighlight = null;
        for (ResearchEntry iconResearch : this.research) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            boolean hasWarp = false;
            if (iconResearch.getStages() != null) {
                for (ResearchStage stage : iconResearch.getStages()) {
                    if (stage.getWarp() > 0) {
                        hasWarp = true;
                        break;
                    }
                }
            }
            int var26 = iconResearch.getDisplayColumn() * 24 - locX;
            int var27 = iconResearch.getDisplayRow() * 24 - locY;
            if (var26 < -24 || var27 < -24 || var26 > this.screenX * this.screenZoom || var27 > this.screenY * this.screenZoom) {
                continue;
            }
            int iconX = this.startX + var26;
            int iconY = this.startY + var27;
            if (!this.isVisible(iconResearch)) {
                continue;
            }
            if (hasWarp) {
                drawForbidden(g, iconX + 8, iconY + 8);
            }
            float c;
            if (knowledge.isResearchComplete(iconResearch.getKey())) {
                c = 1.0F;
            } else if (this.canUnlockResearch(iconResearch)) {
                c = (float) Math.sin(System.currentTimeMillis() % 600L / 600.0 * Math.PI * 2.0) * 0.25F + 0.75F;
            } else {
                c = 0.3F;
            }
            RenderSystem.setShaderColor(c, c, c, 1.0F);
            RenderSystem.enableBlend();
            if (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.ROUND)) {
                g.blit(tx1, iconX - 8, iconY - 8, 144, 48 + (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN) ? 32 : 0), 32, 32);
            } else {
                int ix = 80;
                int iy = 48;
                if (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN)) {
                    iy += 32;
                }
                if (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.HEX)) {
                    ix += 32;
                }
                g.blit(tx1, iconX - 8, iconY - 8, ix, iy, 32, 32);
            }
            if (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.SPIKY)) {
                g.blit(tx1, iconX - 8, iconY - 8, 176, 48 + (iconResearch.hasMeta(ResearchEntry.EnumResearchMeta.HIDDEN) ? 32 : 0), 32, 32);
            }
            boolean bw = false;
            if (!this.canUnlockResearch(iconResearch)) {
                RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 1.0F);
                bw = true;
            }
            if (knowledge.hasResearchFlag(iconResearch.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH)) {
                g.pose().pushPose();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                g.pose().translate(iconX - 9, iconY - 9, 0.0F);
                g.pose().scale(0.5F, 0.5F, 1.0F);
                g.blit(tx1, 0, 0, 176, 16, 32, 32);
                g.pose().popPose();
            }
            if (knowledge.hasResearchFlag(iconResearch.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE)) {
                g.pose().pushPose();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                g.pose().translate(iconX - 9, iconY + 9, 0.0F);
                g.pose().scale(0.5F, 0.5F, 1.0F);
                g.blit(tx1, 0, 0, 208, 16, 32, 32);
                g.pose().popPose();
            }
            drawResearchIcon(g, iconResearch, iconX, iconY, 0.0F, bw);
            if (mx >= this.startX && my >= this.startY && mx < this.startX + this.screenX && my < this.startY + this.screenY
                    && mx >= (iconX - 2) / this.screenZoom && mx <= (iconX + 18) / this.screenZoom
                    && my >= (iconY - 2) / this.screenZoom && my <= (iconY + 18) / this.screenZoom) {
                this.currentHighlight = iconResearch;
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
        RenderSystem.disableDepthTest();
    }

    /**
     * Значок исследования 16×16 (drawResearchIcon): значки сменяются раз в секунду; картинка с кадрами по высоте или ширине
     * анимируется (кадр 150 мс); bw — затемнённый значок; «focus:КЛЮЧ» — значок элемента фокуса.
     */
    public static void drawResearchIcon(GuiGraphics g, ResearchEntry iconResearch, int iconX, int iconY, float zLevel, boolean bw) {
        if (iconResearch.getIcons() == null || iconResearch.getIcons().length == 0) {
            return;
        }
        int idx = (int) (System.currentTimeMillis() / 1000L % iconResearch.getIcons().length);
        Object icon = iconResearch.getIcons()[idx];
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (icon instanceof ResourceLocation rl) {
            RenderSystem.setShaderTexture(0, rl);
            if (bw) {
                RenderSystem.setShaderColor(0.2F, 0.2F, 0.2F, 1.0F);
            }
            int[] size = textureSize(rl);
            int w = size[0];
            int h = size[1];
            if (h > w && h % w == 0) {
                int m = h / w;
                float q = 16.0F / m;
                float idx1 = (float) (System.currentTimeMillis() / 150L % m) * q;
                UtilsFX.drawTexturedQuadF(g.pose(), iconX, iconY, 0.0F, idx1, 16.0F, q, zLevel);
            } else if (w > h && w % h == 0) {
                int m = w / h;
                float q = 16.0F / m;
                float idx1 = (float) (System.currentTimeMillis() / 150L % m) * q;
                UtilsFX.drawTexturedQuadF(g.pose(), iconX, iconY, idx1, 0.0F, q, 16.0F, zLevel);
            } else {
                UtilsFX.drawTexturedQuadFull(g.pose(), iconX, iconY, zLevel);
            }
        } else if (icon instanceof ItemStack stack) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.renderItem(stack, iconX, iconY);
        } else if (icon instanceof String str && str.startsWith("focus")) {
            String k = str.replaceAll("focus:", "");
            if (thaumcraft.api.casters.FocusEngine.getElement(k.trim()) instanceof thaumcraft.api.casters.FocusNode fp) {
                GuiFocalManipulator.drawPart(g, fp, iconX + 8, iconY + 8, 24.0F, false, bw ? 0.2F : 1.0F);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int[] textureSize(ResourceLocation rl) {
        return ICON_SIZES.computeIfAbsent(rl, k -> {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(k);
            GlStateManager._bindTexture(tex.getId());
            int w = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            return new int[] {Math.max(1, w), Math.max(1, h)};
        });
    }

    /** Рамка карты, кнопки, подсказка исследования под курсором (genResearchBackgroundFixedPost). */
    private void genResearchBackgroundFixedPost(GuiGraphics g, int mx, int my, float par3) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int c = 16; c < this.width - 16; c += 64) {
            int p = 64;
            if (c + p > this.width - 16) {
                p = this.width - 16 - c;
            }
            if (p > 0) {
                g.blit(tx1, c, -2, 48, 13, p, 22);
                g.blit(tx1, c, this.height - 20, 48, 13, p, 22);
            }
        }
        for (int var16 = 16; var16 < this.height - 16; var16 += 64) {
            int p = 64;
            if (var16 + p > this.height - 16) {
                p = this.height - 16 - var16;
            }
            if (p > 0) {
                g.blit(tx1, -2, var16, 13, 48, 22, p);
                g.blit(tx1, this.width - 20, var16, 13, 48, 22, p);
            }
        }
        g.blit(tx1, -2, -2, 13, 13, 22, 22);
        g.blit(tx1, -2, this.height - 20, 13, 13, 22, 22);
        g.blit(tx1, this.width - 20, -2, 13, 13, 22, 22);
        g.blit(tx1, this.width - 20, this.height - 20, 13, 13, 22, 22);
        for (var renderable : this.renderables) {
            renderable.render(g, mx, my, par3);
        }
        if (this.currentHighlight != null) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(this.player);
            List<String> text = new ArrayList<>();
            text.add("§6" + I18n.get(this.currentHighlight.getName()));
            if (this.canUnlockResearch(this.currentHighlight)) {
                if (!knowledge.isResearchComplete(this.currentHighlight.getKey()) && this.currentHighlight.getStages() != null) {
                    int stage = knowledge.getResearchStage(this.currentHighlight.getKey());
                    if (stage > 0) {
                        text.add("@@" + ChatFormatting.AQUA + I18n.get("tc.research.stage") + " " + stage + "/"
                                + this.currentHighlight.getStages().length + ChatFormatting.RESET);
                    } else {
                        text.add("@@" + ChatFormatting.GREEN + I18n.get("tc.research.begin") + ChatFormatting.RESET);
                    }
                }
            } else {
                text.add("@@§c" + I18n.get("tc.researchmissing"));
                int a = 0;
                for (String p : this.currentHighlight.getParents()) {
                    if (!ThaumcraftCapabilities.knowsResearchStrict(this.player, p)) {
                        String s = "?";
                        try {
                            s = I18n.get(ResearchCategories.getResearch(this.currentHighlight.getParentsClean()[a]).getName());
                        } catch (Exception ignored) {
                        }
                        text.add("@@§e - " + s);
                    }
                    a++;
                }
            }
            if (knowledge.hasResearchFlag(this.currentHighlight.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH)) {
                text.add("@@" + I18n.get("tc.research.newresearch"));
            }
            if (knowledge.hasResearchFlag(this.currentHighlight.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE)) {
                text.add("@@" + I18n.get("tc.research.newpage"));
            }
            UtilsFX.drawCustomTooltip(g, this.font, text, mx + 3, my - 3, -99);
        }
        RenderSystem.enableDepthTest();
    }

    @Override
    public boolean mouseClicked(double mxd, double myd, int button) {
        int mx = (int) mxd;
        int my = (int) myd;
        this.popuptime = System.currentTimeMillis() - 1L;
        if (!searching && this.currentHighlight != null && !ThaumcraftCapabilities.knowsResearch(this.player, this.currentHighlight.getKey())
                && this.canUnlockResearch(this.currentHighlight)) {
            this.updateResearch();
            PacketDistributor.sendToServer(new PacketSyncProgressToServer(this.currentHighlight.getKey(), true));
            this.openPage(this.currentHighlight, null);
            this.popuptime = System.currentTimeMillis() + 3000L;
            this.popupmessage = I18n.get("tc.research.popup", I18n.get(this.currentHighlight.getName()));
        } else if (this.currentHighlight != null && ThaumcraftCapabilities.knowsResearch(this.player, this.currentHighlight.getKey())) {
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(this.player);
            knowledge.clearResearchFlag(this.currentHighlight.getKey(), IPlayerKnowledge.EnumResearchFlag.RESEARCH);
            knowledge.clearResearchFlag(this.currentHighlight.getKey(), IPlayerKnowledge.EnumResearchFlag.PAGE);
            PacketDistributor.sendToServer(PacketSyncResearchFlagsToServer.of(this.player, this.currentHighlight.getKey()));
            this.openPage(this.currentHighlight, null);
        } else if (searching) {
            int q = 0;
            for (SearchEntry p : this.searchResults) {
                SearchResult sr = p.result;
                if (mx > 22 && mx < 18 + this.screenX && my >= 32 + q * 10 && my < 40 + q * 10) {
                    if (ThaumcraftCapabilities.knowsResearch(this.player, sr.key) && !sr.cat) {
                        this.openPage(ResearchCategories.getResearch(sr.key), sr.recipe);
                        break;
                    }
                    if (this.categoriesTC.contains(sr.key) || this.categoriesOther.contains(sr.key)) {
                        this.selectCategory(sr.key);
                        break;
                    }
                }
                if (32 + (++q + 1) * 10 > this.screenY) {
                    break;
                }
            }
        }
        return super.mouseClicked(mxd, myd, button);
    }

    private void openPage(ResearchEntry entry, ResourceLocation recipe) {
        this.minecraft.setScreen(new GuiResearchPage(entry, recipe, this.guiMapX, this.guiMapY));
    }

    private void selectCategory(String key) {
        searching = false;
        this.searchField.setVisible(false);
        this.searchField.setCanLoseFocus(true);
        this.searchField.setFocused(false);
        selectedCategory = key;
        this.updateResearch();
        this.guiMapX = this.tempMapX = this.curMouseX = guiCenterX;
        this.guiMapY = this.tempMapY = this.curMouseY = guiCenterY;
    }

    private void startSearch() {
        selectedCategory = "";
        searching = true;
        this.searchField.setVisible(true);
        this.searchField.setCanLoseFocus(false);
        this.searchField.setFocused(true);
        this.setFocused(this.searchField);
        this.searchField.setValue("");
        this.updateSearch();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void drawLine(GuiGraphics g, int x, int y, int x2, int y2, float r, float gr, float b, int locX, int locY, float zMod, boolean arrow,
                          boolean flipped) {
        boolean bigCorner = false;
        int xd;
        int yd;
        int xm;
        int ym;
        int xx;
        int yy;
        if (flipped) {
            xd = Math.abs(x2 - x);
            yd = Math.abs(y2 - y);
            xm = xd == 0 ? 0 : (x2 - x > 0 ? -1 : 1);
            ym = yd == 0 ? 0 : (y2 - y > 0 ? -1 : 1);
            if (xd > 1 && yd > 1) {
                bigCorner = true;
            }
            xx = x2 * 24 - 4 - locX + this.startX;
            yy = y2 * 24 - 4 - locY + this.startY;
        } else {
            xd = Math.abs(x - x2);
            yd = Math.abs(y - y2);
            xm = xd == 0 ? 0 : (x - x2 > 0 ? -1 : 1);
            ym = yd == 0 ? 0 : (y - y2 > 0 ? -1 : 1);
            if (xd > 1 && yd > 1) {
                bigCorner = true;
            }
            xx = x * 24 - 4 - locX + this.startX;
            yy = y * 24 - 4 - locY + this.startY;
        }
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, zMod);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(r, gr, b, 1.0F);
        if (arrow) {
            if (flipped) {
                int xx3 = x * 24 - 8 - locX + this.startX;
                int yy3 = y * 24 - 8 - locY + this.startY;
                if (xm < 0) {
                    g.blit(tx1, xx3, yy3, 160, 112, 32, 32);
                } else if (xm > 0) {
                    g.blit(tx1, xx3, yy3, 128, 112, 32, 32);
                } else if (ym > 0) {
                    g.blit(tx1, xx3, yy3, 64, 112, 32, 32);
                } else if (ym < 0) {
                    g.blit(tx1, xx3, yy3, 96, 112, 32, 32);
                }
            } else if (ym < 0) {
                g.blit(tx1, xx - 4, yy - 4, 64, 112, 32, 32);
            } else if (ym > 0) {
                g.blit(tx1, xx - 4, yy - 4, 96, 112, 32, 32);
            } else if (xm > 0) {
                g.blit(tx1, xx - 4, yy - 4, 160, 112, 32, 32);
            } else if (xm < 0) {
                g.blit(tx1, xx - 4, yy - 4, 128, 112, 32, 32);
            }
        }
        int v = 1;
        int h = 0;
        while (v < yd - (bigCorner ? 1 : 0)) {
            g.blit(tx1, xx + xm * 24 * h, yy + ym * 24 * v, 0, 228, 24, 24);
            v++;
        }
        if (bigCorner) {
            if (xm < 0 && ym > 0) {
                g.blit(tx1, xx + xm * 24 * h - 24, yy + ym * 24 * v, 0, 180, 48, 48);
            }
            if (xm > 0 && ym > 0) {
                g.blit(tx1, xx + xm * 24 * h, yy + ym * 24 * v, 48, 180, 48, 48);
            }
            if (xm < 0 && ym < 0) {
                g.blit(tx1, xx + xm * 24 * h - 24, yy + ym * 24 * v - 24, 96, 180, 48, 48);
            }
            if (xm > 0 && ym < 0) {
                g.blit(tx1, xx + xm * 24 * h, yy + ym * 24 * v - 24, 144, 180, 48, 48);
            }
        } else {
            if (xm < 0 && ym > 0) {
                g.blit(tx1, xx + xm * 24 * h, yy + ym * 24 * v, 48, 228, 24, 24);
            }
            if (xm > 0 && ym > 0) {
                g.blit(tx1, xx + xm * 24 * h, yy + ym * 24 * v, 72, 228, 24, 24);
            }
            if (xm < 0 && ym < 0) {
                g.blit(tx1, xx + xm * 24 * h, yy + ym * 24 * v, 96, 228, 24, 24);
            }
            if (xm > 0 && ym < 0) {
                g.blit(tx1, xx + xm * 24 * h, yy + ym * 24 * v, 120, 228, 24, 24);
            }
        }
        v += bigCorner ? 1 : 0;
        for (int var26 = h + (bigCorner ? 2 : 1); var26 < xd; var26++) {
            g.blit(tx1, xx + xm * 24 * var26, yy + ym * 24 * v, 24, 228, 24, 24);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();
    }

    /** Фиолетовое свечение за исследованием, дающим Искажение. */
    public static void drawForbidden(GuiGraphics g, double x, double y) {
        int count = Minecraft.getInstance().player.tickCount;
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0);
        RenderSystem.setShaderTexture(0, nodeTexture);
        UtilsFX.renderQuadCentered(g.pose(), 32, 32, 160 + count % 32, 90.0F, 0.33F, 0.0F, 0.44F, 220, 1, 0.9F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();
    }

    /** Прямоугольник с UV в единицах 1/256 текстуры (за пределами текстуры — повтор). */
    public void drawTexturedModalRectWithDoubles(GuiGraphics g, float xCoord, float yCoord, double minU, double minV, double maxU, double maxV) {
        float f2 = 0.00390625F;
        float f3 = 0.00390625F;
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buf.addVertex(m, xCoord, (float) (yCoord + maxV), 0.0F).setUv((float) (minU * f2), (float) ((minV + maxV) * f3));
        buf.addVertex(m, (float) (xCoord + maxU), (float) (yCoord + maxV), 0.0F).setUv((float) ((minU + maxU) * f2), (float) ((minV + maxV) * f3));
        buf.addVertex(m, (float) (xCoord + maxU), yCoord, 0.0F).setUv((float) ((minU + maxU) * f2), (float) (minV * f3));
        buf.addVertex(m, xCoord, yCoord, 0.0F).setUv((float) (minU * f2), (float) (minV * f3));
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    // --- кнопки ---------------------------------------------------------------------------------------------------------

    private abstract static class TCButton extends AbstractButton {
        TCButton(int x, int y, int w, int h, Component message) {
            super(x, y, w, h, message);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    private class GuiCategoryButton extends TCButton {
        final ResearchCategory rc;
        final String key;
        final boolean flip;
        final int completion;

        GuiCategoryButton(ResearchCategory rc, String key, boolean flip, int x, int y, int w, int h, String name, int completion) {
            super(x, y, w, h, Component.literal(name));
            this.rc = rc;
            this.key = key;
            this.flip = flip;
            this.completion = completion;
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.active && this.visible && mouseX >= this.getX() && mouseY >= this.getY() + GuiResearchBrowser.this.addonShift
                    && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height + GuiResearchBrowser.this.addonShift;
        }

        @Override
        protected boolean clicked(double mouseX, double mouseY) {
            return this.isMouseOver(mouseX, mouseY);
        }

        @Override
        public void onPress() {
            if (!this.key.equals(selectedCategory)) {
                GuiResearchBrowser.this.selectCategory(this.key);
            }
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            int shift = GuiResearchBrowser.this.addonShift;
            boolean hovered = this.isMouseOver(mouseX, mouseY);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            boolean selected = this.key.equals(selectedCategory);
            if (!selected) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                RenderSystem.setShaderColor(0.6F, 1.0F, 1.0F, 1.0F);
            }
            g.blit(tx1, this.getX() - 3, this.getY() - 3 + shift, 13, 13, 22, 22);
            RenderSystem.setShaderTexture(0, this.rc.icon);
            if (!selected && !hovered) {
                RenderSystem.setShaderColor(0.66F, 0.66F, 0.66F, 0.8F);
            } else {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
            UtilsFX.drawTexturedQuadFull(g.pose(), this.getX(), this.getY() + shift, 0.0F);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            boolean nr = false;
            boolean np = false;
            IPlayerKnowledge knowledge = ThaumcraftCapabilities.getKnowledge(GuiResearchBrowser.this.player);
            for (String rk : this.rc.research.keySet()) {
                if (ThaumcraftCapabilities.knowsResearch(GuiResearchBrowser.this.player, rk)) {
                    if (!nr && knowledge.hasResearchFlag(rk, IPlayerKnowledge.EnumResearchFlag.RESEARCH)) {
                        nr = true;
                    }
                    if (!np && knowledge.hasResearchFlag(rk, IPlayerKnowledge.EnumResearchFlag.PAGE)) {
                        np = true;
                    }
                    if (nr && np) {
                        break;
                    }
                }
            }
            if (nr) {
                g.pose().pushPose();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.7F);
                g.pose().translate(this.getX() - 2, this.getY() + shift - 2, 0.0);
                g.pose().scale(0.25F, 0.25F, 1.0F);
                g.blit(tx1, 0, 0, 176, 16, 32, 32);
                g.pose().popPose();
            }
            if (np) {
                g.pose().pushPose();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.7F);
                g.pose().translate(this.getX() - 2, this.getY() + shift + 9, 0.0);
                g.pose().scale(0.25F, 0.25F, 1.0F);
                g.blit(tx1, 0, 0, 208, 16, 32, 32);
                g.pose().popPose();
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (hovered) {
                var font = Minecraft.getInstance().font;
                String dp = this.getMessage().getString() + " (" + this.completion + "%)";
                g.drawString(font, dp, !this.flip ? this.getX() + 22 : GuiResearchBrowser.this.screenX + 9 - font.width(dp), this.getY() + 4 + shift, 16777215);
                int t = 9;
                if (nr) {
                    String s = I18n.get("tc.research.newresearch");
                    g.drawString(font, s, !this.flip ? this.getX() + 22 : GuiResearchBrowser.this.screenX + 9 - font.width(s), this.getY() + 4 + t + shift, 16777215);
                    t += 9;
                }
                if (np) {
                    String s = I18n.get("tc.research.newpage");
                    g.drawString(font, s, !this.flip ? this.getX() + 22 : GuiResearchBrowser.this.screenX + 9 - font.width(s), this.getY() + 4 + t + shift, 16777215);
                }
            }
        }
    }

    private static class GuiScrollButton extends TCButton {
        final boolean flip;
        final Runnable action;

        GuiScrollButton(boolean flip, int x, int y, int w, int h, Runnable action) {
            super(x, y, w, h, Component.empty());
            this.flip = flip;
            this.action = action;
        }

        @Override
        public void onPress() {
            this.action.run();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (this.isHovered()) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 1.0F);
            }
            g.blit(tx1, this.getX(), this.getY(), 51, this.flip ? 71 : 55, 10, 11);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private class GuiSearchButton extends TCButton {
        GuiSearchButton(int x, int y, int w, int h, Component message) {
            super(x, y, w, h, message);
        }

        @Override
        public void onPress() {
            GuiResearchBrowser.this.startSearch();
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            if (this.isHovered()) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                RenderSystem.setShaderColor(0.8F, 0.8F, 0.8F, 1.0F);
            }
            g.blit(tx1, this.getX(), this.getY(), 160, 16, 16, 16);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.isHovered()) {
                g.drawString(Minecraft.getInstance().font, this.getMessage(), this.getX() + 19, this.getY() + 4, 16777215);
            }
        }
    }

    // --- поиск ----------------------------------------------------------------------------------------------------------

    record SearchEntry(String name, SearchResult result) {
    }

    static class SearchResult implements Comparable<SearchResult> {
        final String key;
        final ResourceLocation recipe;
        final boolean cat;

        SearchResult(String key, ResourceLocation rec) {
            this(key, rec, false);
        }

        SearchResult(String key, ResourceLocation recipe, boolean cat) {
            this.key = key;
            this.recipe = recipe;
            this.cat = cat;
        }

        @Override
        public int compareTo(SearchResult arg) {
            int k = this.key.compareTo(arg.key);
            return k == 0 && this.recipe != null && arg.recipe != null ? this.recipe.compareTo(arg.recipe) : k;
        }
    }

    static boolean isEscape(int keyCode) {
        return keyCode == GLFW.GLFW_KEY_ESCAPE;
    }
}
