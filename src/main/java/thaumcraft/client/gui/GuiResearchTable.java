package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.Thaumcraft;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.ITheorycraftAid;
import thaumcraft.api.research.theorycraft.ResearchTableData;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.gui.plugins.GuiImageButton;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.container.ContainerResearchTable;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.misc.PacketStartTheoryToServer;
import thaumcraft.common.tiles.crafting.TileResearchTable;

/**
 * Экран стола исследований (порт GuiResearchTable): выбор помощников и начало теории, колода бумаги (вытянуть карточки),
 * три карточки с анимацией выбора, стопка разыгранных карточек, очки категорий справа (со штрафом), вдохновение сверху.
 * В оригинале проверки «нет бумаги» сравнивали слот с null (пустой слот — не null), поэтому надпись о бумаге не выводилась,
 * а колода рисовалась без бумаги — исправлено (DEVIATIONS.md).
 */
public class GuiResearchTable extends AbstractContainerScreen<ContainerResearchTable> {
    private final TileResearchTable table;
    final Player player;
    static final ResourceLocation txBackground = rl("textures/gui/gui_research_table.png");
    static final ResourceLocation txBase = rl("textures/gui/gui_base.png");
    static final ResourceLocation txPaper = rl("textures/gui/paper.png");
    static final ResourceLocation txPaperGilded = rl("textures/gui/papergilded.png");
    static final ResourceLocation txQuestion = rl("textures/aspects/_unknown.png");
    ResearchTableData.CardChoice lastDraw;
    float[] cardHover = new float[] {0.0F, 0.0F, 0.0F};
    float[] cardZoomOut = new float[] {0.0F, 0.0F, 0.0F};
    float[] cardZoomIn = new float[] {0.0F, 0.0F, 0.0F};
    boolean[] cardActive = new boolean[] {true, true, true};
    boolean cardSelected = false;
    public HashMap<String, Integer> tempCatTotals = new HashMap<>();
    long nexCatCheck = 0L;
    long nextCheck = 0L;
    int dummyInspirationStart = 0;
    Set<String> currentAids = new HashSet<>();
    Set<String> selectedAids = new HashSet<>();
    GuiImageButton buttonCreate;
    GuiImageButton buttonComplete;
    GuiImageButton buttonScrap;
    public ArrayList<ResearchTableData.CardChoice> cardChoices = new ArrayList<>();
    private final Map<String, CardBodyLayout> cardBodyLayouts = new HashMap<>();

    private record CardBodyLayout(float scale, int width, List<FormattedCharSequence> lines) {}

    /** Длинные переводы помещаются над предметами; перенос не пересчитывается на каждом кадре. */
    private CardBodyLayout cardBodyLayout(ResearchTableData.CardChoice choice) {
        String text = choice.card.getLocalizedText();
        ItemStack[] items = choice.card.getRequiredItems();
        int height = items != null && items.length > 0 ? 110 : 160;
        String key = height + ":" + text;
        CardBodyLayout cached = this.cardBodyLayouts.get(key);
        if (cached != null) return cached;
        float scale = 1.0F;
        int width = 140;
        List<FormattedCharSequence> lines = this.font.split(FormattedText.of(text), width);
        for (int attempt = 0; attempt < 24 && lines.size() * this.font.lineHeight * scale > height; attempt++) {
            scale *= 0.95F;
            width = Mth.floor(140.0F / scale);
            lines = this.font.split(FormattedText.of(text), width);
        }
        CardBodyLayout layout = new CardBodyLayout(scale, width, lines);
        if (this.cardBodyLayouts.size() >= 64) this.cardBodyLayouts.clear();
        this.cardBodyLayouts.put(key, layout);
        return layout;
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }

    public GuiResearchTable(ContainerResearchTable menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.table = menu.tileEntity;
        this.imageWidth = 255;
        this.imageHeight = 255;
        this.player = inv.player;
        if (this.table.data != null) {
            for (String cat : this.table.data.categoryTotals.keySet()) {
                this.tempCatTotals.put(cat, this.table.data.categoryTotals.get(cat));
            }
            this.syncFromTableChoices();
            this.lastDraw = this.table.data.lastDraw;
        }
    }

    private void syncFromTableChoices() {
        this.cardChoices.clear();
        this.cardChoices.addAll(this.table.data.cardChoices);
    }

    @Override
    protected void init() {
        super.init();
        this.buttonCreate = this.addRenderableWidget(new GuiImageButton(this.leftPos + 128, this.topPos + 22, 49, 11, "button.create.theory", null,
                txBase, 37, 66, 51, 13, 8978346, () -> {
                    this.playButtonClick();
                    PacketDistributor.sendToServer(new PacketStartTheoryToServer(this.table.getBlockPos(), new HashSet<>(this.selectedAids)));
                }));
        this.buttonComplete = this.addRenderableWidget(new GuiImageButton(this.leftPos + 191, this.topPos + 96, 49, 11, "button.complete.theory", null,
                txBase, 37, 66, 51, 13, 8978346, () -> {
                    this.playButtonClick();
                    this.sendButton(7);
                    this.tempCatTotals.clear();
                    this.lastDraw = null;
                }));
        this.buttonScrap = this.addRenderableWidget(new GuiImageButton(this.leftPos + 128, this.topPos + 168, 49, 11, "button.scrap.theory", null,
                txBase, 37, 66, 51, 13, 16720418, () -> {
                    this.playButtonClick();
                    this.sendButton(9);
                    this.tempCatTotals.clear();
                    this.lastDraw = null;
                    this.table.data = null;
                    this.cardChoices.clear();
                }));
    }

    private void sendButton(int id) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private boolean hasInk() {
        ItemStack tools = this.table.getStackInSlot(0);
        return !tools.isEmpty() && tools.getDamageValue() != tools.getMaxDamage();
    }

    private boolean hasPaper() {
        return !this.table.getStackInSlot(1).isEmpty();
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        // drawGuiContainerForegroundLayer оригинала пуст
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float par3) {
        super.render(g, mx, my, par3);
        int xx = this.leftPos;
        int yy = this.topPos;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.table.data == null) {
            if (!this.currentAids.isEmpty()) {
                int side = Math.min(this.currentAids.size(), 6);
                int c = 0;
                int r = 0;
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.2F);
                for (String key : this.currentAids) {
                    ITheorycraftAid mutator = TheorycraftManager.aids.get(key);
                    if (mutator != null) {
                        int x = xx + 128 + 20 * c - side * 10;
                        int y = yy + 85 + 35 * r;
                        if (this.isHovering(x - xx, y - yy, 16, 16, mx, my) && !this.selectedAids.contains(key)) {
                            g.pose().pushPose();
                            g.pose().translate(0.0F, 0.0F, 300.0F);
                            g.blit(txBase, x, y, 0, 96, 16, 16);
                            g.pose().popPose();
                        }
                        if (++c >= side) {
                            r++;
                            c = 0;
                        }
                    }
                }
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        } else {
            int sx = 128;
            int cw = 110;
            int sz = this.cardChoices.size();
            int a = 0;
            if (!this.cardSelected) {
                for (ResearchTableData.CardChoice cardChoice : this.cardChoices) {
                    if (this.cardZoomOut[a] >= 1.0F) {
                        float dx = 55 + sx - 55 * sz + cw * a - 65;
                        float fx = 65.0F + dx * this.cardZoomOut[a];
                        float qx = 191.0F - fx;
                        if (this.cardActive[a]) {
                            fx += qx * this.cardZoomIn[a];
                        }
                        this.drawSheetOverlay(g, fx, 100.0, cardChoice, mx, my);
                        a++;
                    }
                }
            }
            int qq = 0;
            if (!this.hasInk()) {
                sx = Math.max(this.font.width(I18n.get("tile.researchtable.noink.0")), this.font.width(I18n.get("tile.researchtable.noink.1"))) / 2;
                UtilsFX.drawCustomTooltip(g, this.font, Arrays.asList(I18n.get("tile.researchtable.noink.0"), I18n.get("tile.researchtable.noink.1")),
                        xx - sx + 116, yy + 60 + qq, 11, true);
                qq += 40;
            }
            if (!this.hasPaper()) {
                sx = this.font.width(I18n.get("tile.researchtable.nopaper.0")) / 2;
                UtilsFX.drawCustomTooltip(g, this.font, List.of(I18n.get("tile.researchtable.nopaper.0")), xx - sx + 116, yy + 60 + qq, 11, true);
            }
        }
        this.renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTicks, int mx, int my) {
        this.checkButtons();
        int xx = this.leftPos;
        int yy = this.topPos;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(txBackground, xx, yy, 0, 0, 255, 255);
        if (this.table.data == null) {
            if (this.nextCheck < this.player.tickCount) {
                this.currentAids = this.table.checkSurroundingAids();
                this.dummyInspirationStart = ResearchTableData.getAvailableInspiration(this.player);
                this.nextCheck = this.player.tickCount + 100;
            }
            g.pose().pushPose();
            g.pose().translate(xx + 128 - this.dummyInspirationStart * 5, yy + 55, 0.0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            for (int a = 0; a < this.dummyInspirationStart; a++) {
                g.blit(txBase, 20 * a, 0, this.dummyInspirationStart - this.selectedAids.size() <= a ? 48 : 32, 96, 16, 16);
            }
            g.pose().popPose();
            if (!this.currentAids.isEmpty()) {
                int side = Math.min(this.currentAids.size(), 6);
                int c = 0;
                int r = 0;
                for (String key : this.currentAids) {
                    ITheorycraftAid mutator = TheorycraftManager.aids.get(key);
                    if (mutator != null) {
                        int x = xx + 128 + 20 * c - side * 10;
                        int y = yy + 85 + 35 * r;
                        if (this.selectedAids.contains(key)) {
                            g.blit(txBase, x, y, 0, 96, 16, 16);
                        }
                        if (mutator.getAidObject() instanceof ItemStack || mutator.getAidObject() instanceof Block) {
                            ItemStack s = mutator.getAidObject() instanceof ItemStack is ? is : new ItemStack((Block) mutator.getAidObject());
                            g.renderItem(s, x, y);
                        }
                        if (++c >= side) {
                            r++;
                            c = 0;
                        }
                    }
                }
            }
        } else {
            this.checkCards();
            for (int a = 0; this.table.data != null && a < this.table.data.bonusDraws; a++) {
                g.blit(txBase, xx + 15 + a * 2, yy + 150 + a, 64, 96, 16, 16);
            }
            if (this.table.data == null) {
                return;
            }
            g.pose().pushPose();
            g.pose().translate(xx + 128 - this.table.data.inspirationStart * 5, yy + 16, 0.0);
            g.pose().scale(0.5F, 0.5F, 1.0F);
            for (int a = 0; a < this.table.data.inspirationStart; a++) {
                g.blit(txBase, 20 * a, 0, this.table.data.inspiration <= a ? 48 : 32, 96, 16, 16);
            }
            g.pose().popPose();
            int sheets = 0;
            if (this.hasPaper()) {
                sheets = 1 + this.table.getStackInSlot(1).getCount() / 4;
            }
            Random r = new Random(55L);
            if (sheets > 0 && !this.table.data.isComplete()) {
                for (int a = 0; a < sheets; a++) {
                    this.drawSheet(g, xx + 65, yy + 100, 6.0, r, 1.0F, 1.0F, null);
                }
                boolean highlight = false;
                int var7 = mx - (25 + xx);
                int var8 = my - (55 + yy);
                if (this.cardChoices.isEmpty() && var7 >= 0 && var8 >= 0 && var7 < 75 && var8 < 90) {
                    highlight = true;
                }
                g.pose().pushPose();
                RenderSystem.enableBlend();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, highlight ? 1.0F : 0.5F);
                RenderSystem.setShaderTexture(0, txQuestion);
                g.pose().translate(xx + 65, yy + 100, 0.0);
                float sc = highlight ? 1.75F : 1.5F;
                g.pose().scale(sc, sc, 1.0F);
                UtilsFX.drawTexturedQuadFull(g.pose(), -8.0F, -8.0F, 0.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                g.pose().popPose();
            }
            for (Long seed : this.table.data.savedCards) {
                r = new Random(seed);
                this.drawSheet(g, xx + 191, yy + 100, 6.0, r, 1.0F, 1.0F, null);
            }
            if (this.lastDraw != null) {
                r = new Random(this.lastDraw.card.getSeed());
                this.drawSheet(g, xx + 191, yy + 100, 6.0, r, 1.0F, 1.0F, this.lastDraw);
            }
            List<String> sparkle = new ArrayList<>();
            if (this.nexCatCheck < this.player.tickCount) {
                for (String cat : ResearchCategories.researchCategories.keySet()) {
                    int t0 = this.table.data.categoryTotals.getOrDefault(cat, 0);
                    int t1 = this.tempCatTotals.getOrDefault(cat, 0);
                    if (t0 == 0 && t1 == 0) {
                        this.tempCatTotals.remove(cat);
                    } else {
                        if (t1 > t0) {
                            t1--;
                        }
                        if (t1 < t0) {
                            t1++;
                            sparkle.add(cat);
                        }
                        this.tempCatTotals.put(cat, t1);
                    }
                }
                this.nexCatCheck = this.player.tickCount + 1;
            }
            HashMap<String, Integer> unsortedMap = new HashMap<>();
            for (String cat : this.tempCatTotals.keySet()) {
                int cf = this.tempCatTotals.get(cat);
                if (cf != 0) {
                    unsortedMap.put(cat, cf);
                }
            }
            Comparator<Map.Entry<String, Integer>> valueComparator = (e1, e2) -> e2.getValue().compareTo(e1.getValue());
            Map<String, Integer> sortedMap = unsortedMap.entrySet().stream().sorted(valueComparator)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            int i = 0;
            for (String field : sortedMap.keySet()) {
                ResearchCategory rc = ResearchCategories.getResearchCategory(field);
                int rowY = yy + 16 + i * 18 + (i > 0 ? 4 : 0);
                if (rc != null) {
                    g.pose().pushPose();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    g.pose().translate(xx + 253, rowY, 0.0F);
                    g.pose().scale(0.0625F, 0.0625F, 0.0625F);
                    g.blit(rc.icon, 0, 0, 0, 0, 255, 255);
                    g.pose().popPose();
                }
                String s = sortedMap.get(field) + "%";
                if (i > this.table.data.penaltyStart) {
                    int q = sortedMap.get(field) / 3;
                    s = s + " (-" + q + ")";
                }
                int textY = yy + 20 + i * 18 + (i > this.table.data.penaltyStart ? 4 : 0);
                g.pose().pushPose();
                g.pose().translate(0.0F, 0.0F, 5.0F);
                g.drawString(this.font, s, xx + 276, textY,
                        this.table.data.categoriesBlocked.contains(field) ? 6316128 : (i <= this.table.data.penaltyStart ? 57536 : 16777215), true);
                g.pose().popPose();
                if (sparkle.contains(field)) {
                    for (int q = 0; q < 2; q++) {
                        float rr = 1.0F;
                        float gg = Mth.randomBetweenInclusive(this.player.getRandom(), 189, 255) / 255.0F;
                        float bb = Mth.randomBetweenInclusive(this.player.getRandom(), 64, 255) / 255.0F;
                        FXDispatcher.INSTANCE.drawSimpleSparkleGui(this.player.getRandom(),
                                xx + 276 + this.player.getRandom().nextFloat() * this.font.width(s), textY + this.player.getRandom().nextFloat() * 8.0F,
                                this.player.level().random.nextGaussian() * 0.5, this.player.level().random.nextGaussian() * 0.5, 24.0F, rr, gg, bb, 0, 0.9F,
                                -1.0F);
                    }
                }
                int var7 = mx - (xx + 256);
                int var8 = my - (yy + 16 + i * 18 + (i > this.table.data.penaltyStart ? 4 : 0));
                if (var7 >= 0 && var8 >= 0 && var7 < 16 && var8 < 16) {
                    UtilsFX.drawCustomTooltip(g, this.font, List.of(I18n.get("tc.research_category." + field)), mx + 8, my + 8, 11);
                }
                i++;
            }
            int sx = 128;
            int cw = 110;
            int sz = this.cardChoices.size();
            int a = 0;
            for (ResearchTableData.CardChoice cardChoice : this.cardChoices) {
                r = new Random(cardChoice.card.getSeed());
                int var7 = mx - (5 + sx - 55 * sz + xx + cw * a);
                int var8 = my - (100 + yy - 60);
                if (this.cardZoomOut[a] >= 0.95 && !this.cardSelected) {
                    if (var7 >= 0 && var8 >= 0 && var7 < 100 && var8 < 120) {
                        this.cardHover[a] = this.cardHover[a] + Math.max((0.25F - this.cardHover[a]) / 3.0F * partialTicks, 0.0025F);
                    } else {
                        this.cardHover[a] = this.cardHover[a] - 0.1F * partialTicks;
                    }
                }
                if (a == sz - 1 || this.cardZoomOut[a + 1] > 0.6) {
                    float f = this.cardZoomOut[a];
                    this.cardZoomOut[a] = this.cardZoomOut[a] + Math.max((1.0F - this.cardZoomOut[a]) / 5.0F * partialTicks, 0.0025F);
                    if (this.cardZoomOut[a] > 0.0F && f == 0.0F) {
                        this.playButtonPageFlip();
                    }
                }
                float prevZoomIn = this.cardZoomIn[a];
                if (this.cardSelected) {
                    this.cardZoomIn[a] = (float) (this.cardZoomIn[a]
                            + (this.cardActive[a] ? Math.max((1.0F - this.cardZoomIn[a]) / 3.0F * partialTicks, 0.0025) : 0.3F * partialTicks));
                    this.cardHover[a] = 1.0F - this.cardZoomIn[a];
                }
                this.cardZoomIn[a] = Mth.clamp(this.cardZoomIn[a], 0.0F, 1.0F);
                this.cardHover[a] = Mth.clamp(this.cardHover[a], 0.0F, 0.25F);
                this.cardZoomOut[a] = Mth.clamp(this.cardZoomOut[a], 0.0F, 1.0F);
                float dx = 55 + sx - 55 * sz + xx + cw * a - (xx + 65);
                float fx = xx + 65 + dx * this.cardZoomOut[a];
                float qx = xx + 191 - fx;
                if (this.cardActive[a]) {
                    fx += qx * this.cardZoomIn[a];
                }
                this.drawSheet(g, fx, yy + 100, 6.0F + this.cardZoomOut[a] * 2.0F - this.cardZoomIn[a] * 2.0F + this.cardHover[a], r,
                        this.cardActive[a] ? 1.0F : 1.0F - this.cardZoomIn[a], Math.max(1.0F - this.cardZoomOut[a], this.cardZoomIn[a]), cardChoice);
                if (this.cardSelected && this.cardActive[a] && this.cardZoomIn[a] >= 1.0F && prevZoomIn < 1.0F) {
                    this.playButtonWrite();
                    this.cardChoices.clear();
                    this.cardSelected = false;
                    this.lastDraw = this.table.data.lastDraw;
                    break;
                }
                a++;
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Лист бумаги (карточка): случайный сдвиг, наклон и переворот по seed; у карточки — категория, название, текст, цена, предметы. */
    private void drawSheet(GuiGraphics g, double x, double y, double scale, Random r, float alpha, float tilt, ResearchTableData.CardChoice cardChoice) {
        g.pose().pushPose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        g.pose().translate(x + r.nextGaussian(), y + r.nextGaussian(), 0.0);
        g.pose().scale((float) scale, (float) scale, 1.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees((float) (r.nextGaussian() * tilt)));
        g.pose().pushPose();
        RenderSystem.setShaderTexture(0, cardChoice != null && cardChoice.fromAid ? txPaperGilded : txPaper);
        if (r.nextBoolean()) {
            g.pose().mulPose(Axis.ZP.rotationDegrees(180.0F));
        }
        if (r.nextBoolean()) {
            g.pose().mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        RenderSystem.disableCull();
        UtilsFX.drawTexturedQuadFull(g.pose(), -8.0F, -8.0F, 0.0F);
        RenderSystem.enableCull();
        g.pose().popPose();
        if (cardChoice != null && alpha == 1.0F) {
            if (cardChoice.card.getResearchCategory() != null) {
                ResearchCategory rc = ResearchCategories.getResearchCategory(cardChoice.card.getResearchCategory());
                if (rc != null) {
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha / 6.0F);
                    g.pose().pushPose();
                    g.pose().scale(0.5F, 0.5F, 1.0F);
                    RenderSystem.setShaderTexture(0, rc.icon);
                    UtilsFX.drawTexturedQuadFull(g.pose(), -8.0F, -8.0F, 0.0F);
                    g.pose().popPose();
                }
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().pushPose();
            g.pose().scale(0.0625F, 0.0625F, 1.0F);
            String name = ChatFormatting.BOLD + cardChoice.card.getLocalizedName() + ChatFormatting.RESET;
            int sz = this.font.width(name);
            g.drawString(this.font, name, -sz / 2, -65, 0, false);
            CardBodyLayout body = this.cardBodyLayout(cardChoice);
            g.pose().pushPose();
            g.pose().translate(0.0F, -48.0F, 0.0F);
            g.pose().scale(body.scale(), body.scale(), 1.0F);
            for (int line = 0; line < body.lines().size(); line++) {
                g.drawString(this.font, body.lines().get(line), -body.width() / 2, line * this.font.lineHeight, 0, false);
            }
            g.pose().popPose();
            g.pose().popPose();
            g.pose().pushPose();
            g.pose().scale(0.0625F, 0.0625F, 1.0F);
            int cc = cardChoice.card.getInspirationCost();
            boolean add = false;
            if (cc < 0) {
                add = true;
                cc = Math.abs(cc) + 1;
            }
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            for (int a = 0; a < cc; a++) {
                if (a == 0 && add) {
                    g.blit(txBase, -10 * cc + 20 * a, -95, 48, 0, 16, 16);
                } else {
                    g.blit(txBase, -10 * cc + 20 * a, -95, 32, 96, 16, 16);
                }
            }
            g.pose().popPose();
            if (cardChoice.card.getRequiredItems() != null) {
                ItemStack[] items = cardChoice.card.getRequiredItems();
                for (int a = 0; a < items.length; a++) {
                    if (items[a] != null && !items[a].isEmpty()) {
                        g.pose().pushPose();
                        g.pose().scale(0.125F, 0.125F, 1.0F);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        g.renderItem(items[a], -9 * items.length + 18 * a, 35);
                        g.pose().popPose();
                        try {
                            if (cardChoice.card.getRequiredItemsConsumed()[a]) {
                                g.pose().pushPose();
                                g.pose().scale(0.125F, 0.125F, 1.0F);
                                float s = (float) Math.sin((this.player.tickCount + a * 2 + this.minecraft.getTimer().getGameTimeDeltaPartialTick(true)) / 2.0F)
                                        * 0.03F;
                                g.pose().translate(-2 - 9 * items.length + 18 * a, 45.0F + s * 10.0F, 200.0);
                                g.pose().scale(0.5F, 0.5F + s, 1.0F);
                                g.blit(txBase, 0, 0, 64, 120, 16, 16);
                                g.pose().popPose();
                            }
                        } catch (Exception ignored) {
                        }
                    } else {
                        g.pose().pushPose();
                        RenderSystem.setShaderTexture(0, txQuestion);
                        g.pose().scale(0.125F, 0.125F, 1.0F);
                        RenderSystem.setShaderColor(0.75F, 0.75F, 0.75F, alpha);
                        g.pose().translate(-9 * items.length + 18 * a, 35.0, 0.0);
                        UtilsFX.drawTexturedQuadFull(g.pose(), 0.0F, 0.0F, 0.0F);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        g.pose().popPose();
                    }
                }
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().popPose();
    }

    /** Подсказки предметов карточки. */
    private void drawSheetOverlay(GuiGraphics g, double x, double y, ResearchTableData.CardChoice cardChoice, int mx, int my) {
        if (cardChoice != null && cardChoice.card.getRequiredItems() != null) {
            ItemStack[] items = cardChoice.card.getRequiredItems();
            for (int a = 0; a < items.length; a++) {
                if (this.isHovering((int) (x - 9 * items.length + 18 * a), (int) (y + 36.0), 15, 15, mx, my)) {
                    if (items[a] != null && !items[a].isEmpty()) {
                        g.renderTooltip(this.font, items[a], mx, my);
                    } else {
                        g.renderTooltip(this.font, Component.translatable("tc.card.unknown"), mx, my);
                    }
                }
            }
        }
    }

    private void drawCards() {
        this.cardSelected = false;
        this.cardHover = new float[] {0.0F, 0.0F, 0.0F};
        this.cardZoomOut = new float[] {0.0F, 0.0F, 0.0F};
        this.cardZoomIn = new float[] {0.0F, 0.0F, 0.0F};
        this.cardActive = new boolean[] {true, true, true};
        int draw = 2;
        if (this.table.data.bonusDraws > 0) {
            draw++;
            this.table.data.bonusDraws--;
        }
        this.sendButton(draw);
        this.cardChoices.clear();
    }

    private void checkButtons() {
        if (this.buttonCreate == null) {
            return;
        }
        this.buttonComplete.active = false;
        this.buttonComplete.visible = false;
        this.buttonScrap.active = false;
        this.buttonScrap.visible = false;
        if (this.table.data != null) {
            this.buttonCreate.active = false;
            this.buttonCreate.visible = false;
            if (this.table.data.isComplete()) {
                this.buttonComplete.active = true;
                this.buttonComplete.visible = true;
            } else {
                this.buttonScrap.active = true;
                this.buttonScrap.visible = true;
            }
        } else {
            this.buttonCreate.visible = true;
            // в оригинале наличие бумаги проверялось сравнением с null — кнопка была доступна и без бумаги
            this.buttonCreate.active = this.hasPaper() && this.hasInk();
        }
    }

    @Override
    public boolean mouseClicked(double mxd, double myd, int button) {
        boolean handled = super.mouseClicked(mxd, myd, button);
        int mx = (int) mxd;
        int my = (int) myd;
        int xx = (this.width - this.imageWidth) / 2;
        int yy = (this.height - this.imageHeight) / 2;
        if (this.table.data == null) {
            if (!this.currentAids.isEmpty()) {
                int side = Math.min(this.currentAids.size(), 6);
                int c = 0;
                int r = 0;
                for (String key : this.currentAids) {
                    ITheorycraftAid mutator = TheorycraftManager.aids.get(key);
                    if (mutator != null) {
                        int x = 128 + 20 * c - side * 10;
                        int y = 85 + 35 * r;
                        if (this.isHovering(x, y, 16, 16, mx, my)) {
                            if (this.selectedAids.contains(key)) {
                                this.selectedAids.remove(key);
                            } else if (this.selectedAids.size() + 1 < this.dummyInspirationStart) {
                                this.selectedAids.add(key);
                            }
                        }
                        if (++c >= side) {
                            r++;
                            c = 0;
                        }
                    }
                }
            }
        } else {
            int sx = 128;
            int cw = 110;
            if (!this.cardChoices.isEmpty()) {
                int pressed = -1;
                for (int a = 0; a < this.cardChoices.size(); a++) {
                    int var7 = mx - (5 + sx - 55 * this.cardChoices.size() + xx + cw * a);
                    int var8 = my - (100 + yy - 60);
                    if (this.cardZoomOut[a] >= 0.95 && !this.cardSelected && var7 >= 0 && var8 >= 0 && var7 < 100 && var8 < 120) {
                        pressed = a;
                        break;
                    }
                }
                if (pressed >= 0 && this.hasInk()) {
                    this.sendButton(4 + pressed);
                }
            } else {
                int var7 = mx - (25 + xx);
                int var8 = my - (55 + yy);
                if (var7 >= 0 && var8 >= 0 && var7 < 75 && var8 < 90 && this.hasPaper()) {
                    this.drawCards();
                }
            }
        }
        return handled || true;
    }

    void checkCards() {
        if (!this.table.data.cardChoices.isEmpty() && this.cardChoices.isEmpty()) {
            this.syncFromTableChoices();
        }
        if (!this.cardSelected) {
            for (int a = 0; a < this.cardChoices.size(); a++) {
                try {
                    if (this.table.data != null && this.table.data.cardChoices.size() > a && this.table.data.cardChoices.get(a).selected) {
                        for (int q = 0; q < this.cardChoices.size(); q++) {
                            this.cardActive[q] = this.table.data.cardChoices.get(q).selected;
                        }
                        this.cardSelected = true;
                        this.playButtonPageSelect();
                        this.sendButton(1);
                        break;
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void playButtonPageFlip() {
        this.player.playSound(SoundsTC.page.get(), 1.0F, 1.0F);
    }

    private void playButtonPageSelect() {
        this.player.playSound(SoundsTC.pageturn.get(), 1.0F, 1.0F);
    }

    private void playButtonClick() {
        this.player.playSound(SoundsTC.clack.get(), 0.4F, 1.0F);
    }

    private void playButtonWrite() {
        this.player.playSound(SoundsTC.write.get(), 0.3F, 1.0F);
    }
}
