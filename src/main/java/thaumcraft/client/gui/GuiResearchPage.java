package thaumcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.ShapedArcaneRecipe;
import thaumcraft.api.crafting.ShapelessArcaneRecipe;
import thaumcraft.api.research.ResearchAddendum;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.ResearchEntry;
import thaumcraft.api.research.ResearchStage;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.client.lib.events.HudHandler;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.network.playerdata.PacketSyncProgressToServer;
import thaumcraft.common.lib.utils.InventoryUtils;

/**
 * Страница исследования (порт GuiResearchPage): текст стадии с разметкой (&lt;BR&gt;, &lt;LINE&gt;, &lt;DIV&gt;, &lt;PAGE&gt;, &lt;IMG&gt;),
 * требования стадии и кнопка «Завершить», закладки рецептов, вкладки аспектов и знаний, переход по рецептам ингредиентов.
 * Страницы рецептов: верстак — сейчас; магический верстак, тигель, инфузия, многоблочные конструкции — на их этапах.
 */
public class GuiResearchPage extends Screen {
    public static LinkedList<ResourceLocation> history = new LinkedList<>();
    protected int paneWidth = 256;
    protected int paneHeight = 181;
    protected double guiMapX;
    protected double guiMapY;
    private final ResearchEntry research;
    private int currentStage = 0;
    int lastStage = 0;
    boolean hold = false;
    private int page = 0;
    private int maxPages = 0;
    private int maxAspectPages = 0;
    AspectList knownPlayerAspects;
    IPlayerKnowledge playerKnowledge;
    int rhash = 0;
    long lastCheck = 0L;
    float pt;
    static final ResourceLocation tex1 = rl("textures/gui/gui_researchbook.png");
    static final ResourceLocation tex2 = rl("textures/gui/gui_researchbook_overlay.png");
    static final ResourceLocation tex3 = rl("textures/aspects/_back.png");
    static final ResourceLocation tex4 = rl("textures/gui/paper.png");
    static final ResourceLocation dummyResearch = rl("textures/aspects/_unknown.png");
    static final ResourceLocation dummyMap = rl("textures/research/rd_map.png");
    static final ResourceLocation dummyFlask = rl("textures/research/rd_flask.png");
    static final ResourceLocation dummyChest = rl("textures/research/rd_chest.png");
    int hrx = 0;
    int hry = 0;
    static ResourceLocation shownRecipe;
    int recipePage = 0;
    int recipePageMax = 0;
    private long lastCycle = 0L;
    private boolean showingAspects = false;
    private boolean showingKnowledge = false;
    private static int aspectsPage = 0;
    final LinkedHashMap<ResourceLocation, List<Object>> recipeLists = new LinkedHashMap<>();
    final LinkedHashMap<ResourceLocation, List<Object>> recipeOutputs = new LinkedHashMap<>();
    final LinkedHashMap<ResourceLocation, List<Object>> drilldownLists = new LinkedHashMap<>();
    boolean hasRecipePages;
    boolean renderingCompound = false;
    static boolean cycleMultiblockLines = false;
    /** Места ссылок на рецепты ингредиентов: x, y (позиция мыши при наведении, как в оригинале), рецепт, страница. */
    final List<Object[]> reference = new ArrayList<>();
    private int cycle = -1;
    boolean allowWithPagePopup = false;
    List<String> tipText = null;
    private static final PageImage PILINE = PageImage.parse("thaumcraft:textures/gui/gui_researchbook.png:24:184:95:6:1");
    private static final PageImage PIDIV = PageImage.parse("thaumcraft:textures/gui/gui_researchbook.png:28:192:140:6:1");
    private final List<Page> pages = new ArrayList<>();
    boolean isComplete = false;
    boolean hasAllRequisites = false;
    boolean[] hasItem = null;
    boolean[] hasCraft = null;
    boolean[] hasResearch = null;
    boolean[] hasKnow = null;
    public final HashMap<Integer, String> keyCache = new HashMap<>();

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }

    public GuiResearchPage(ResearchEntry research, ResourceLocation recipe, double x, double y) {
        super(Component.empty());
        this.research = research;
        this.guiMapX = x;
        this.guiMapY = y;
        this.minecraft = Minecraft.getInstance();
        this.font = this.minecraft.font;
        this.playerKnowledge = ThaumcraftCapabilities.getKnowledge(this.minecraft.player);
        this.parsePages();
        this.knownPlayerAspects = new AspectList();
        for (Aspect a : Aspect.aspects.values()) {
            if (ThaumcraftCapabilities.knowsResearch(this.minecraft.player, "!" + a.getTag().toLowerCase(Locale.ROOT))) {
                this.knownPlayerAspects.add(a, 1);
            }
        }
        this.maxAspectPages = Mth.ceil(this.knownPlayerAspects.size() / 5.0F);
        this.page = 0;
        if (recipe != null) {
            shownRecipe = recipe;
        }
    }

    @Override
    public void render(GuiGraphics g, int par1, int par2, float par3) {
        this.hasRecipePages = false;
        long nano = System.nanoTime();
        if (nano > this.lastCheck) {
            this.parsePages();
            if (this.hold) {
                this.lastCheck = nano + 250000000L;
            } else {
                this.lastCheck = nano + 2000000000L;
            }
            if (this.currentStage > this.lastStage) {
                this.hold = false;
            }
        }
        this.pt = par3;
        this.renderTransparentBackground(g);
        RenderSystem.disableDepthTest();
        this.genResearchBackground(g, par1, par2);
        int sw = (this.width - this.paneWidth) / 2;
        int sh = (this.height - this.paneHeight) / 2;
        if (!history.isEmpty()) {
            int mx = par1 - (sw + 118);
            int my = par2 - (sh + 190);
            if (mx >= 0 && my >= 0 && mx < 20 && my < 12) {
                g.drawString(this.font, I18n.get("recipe.return"), par1, par2, 16777215, true);
            }
        }
        RenderSystem.enableDepthTest();
    }

    protected void genResearchBackground(GuiGraphics g, int par1, int par2) {
        int sw = (this.width - this.paneWidth) / 2;
        int sh = (this.height - this.paneHeight) / 2;
        float var10 = (this.width - this.paneWidth * 1.3F) / 2.0F;
        float var11 = (this.height - this.paneHeight * 1.3F) / 2.0F;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        g.pose().translate(var10, var11, 0.0F);
        g.pose().scale(1.3F, 1.3F, 1.0F);
        g.blit(tex1, 0, 0, 0, 0, this.paneWidth, this.paneHeight);
        g.pose().popPose();
        this.reference.clear();
        int current = 0;
        for (Page p : this.pages) {
            if ((current == this.page || current == this.page + 1) && current < this.maxPages) {
                this.drawPage(g, p, current % 2, sw, sh - 10, par1, par2);
            }
            if (++current > this.page + 1) {
                break;
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        float bob = Mth.sin(this.minecraft.player.tickCount / 3.0F) * 0.2F + 0.1F;
        if (!history.isEmpty()) {
            this.drawTexturedModalRectScaled(g, tex1, sw + 118, sh + 190, 38, 202, 20, 12, bob);
        }
        if (this.page > 0 && shownRecipe == null) {
            this.drawTexturedModalRectScaled(g, tex1, sw - 16, sh + 190, 0, 184, 12, 8, bob);
        }
        if (this.page < this.maxPages - 2 && shownRecipe == null) {
            this.drawTexturedModalRectScaled(g, tex1, sw + 262, sh + 190, 12, 184, 12, 8, bob);
        }
        if (this.tipText != null) {
            UtilsFX.drawCustomTooltip(g, this.font, this.tipText, par1, par2 + 12, 11);
            this.tipText = null;
        }
    }

    private void drawPage(GuiGraphics g, Page pageParm, int side, int x, int y, int mx, int my) {
        if (this.lastCycle < System.currentTimeMillis()) {
            this.cycle++;
            this.lastCycle = System.currentTimeMillis() + 1000L;
            // TODO: многоблочные схемы — cycleMultiblockLines / blockAccess.sliceLine
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (this.page == 0 && side == 0) {
            g.blit(tex1, x + 4, y - 7, 24, 184, 96, 4);
            g.blit(tex1, x + 4, y + 10, 24, 184, 96, 4);
            String name = I18n.get(this.research.getName());
            int offset = this.font.width(name);
            if (offset <= 140) {
                g.drawString(this.font, name, x - 15 + 140 / 2 - offset / 2, y, 2105376, false);
            } else {
                float vv = 140.0F / offset;
                g.pose().pushPose();
                g.pose().translate(x - 15 + 140 / 2 - offset / 2 * vv, y + 1.0F * vv, 0.0F);
                g.pose().scale(vv, vv, vv);
                g.drawString(this.font, name, 0, 0, 2105376, false);
                g.pose().popPose();
            }
            y += 28;
        }
        for (Object content : pageParm.contents) {
            if (content instanceof String str) {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                String ss = str.replace("~B", "");
                g.drawString(this.font, ss, x - 15 + side * 152, y - 6, 0, false);
                y += this.font.lineHeight;
                if (str.endsWith("~B")) {
                    y = (int) (y + this.font.lineHeight * 0.66);
                }
            } else if (content instanceof PageImage pi) {
                g.pose().pushPose();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableBlend();
                int pad = (140 - pi.aw) / 2;
                g.pose().translate(x - 15 + side * 152 + pad, y - 5, 0.0F);
                g.pose().scale(pi.scale, pi.scale, pi.scale);
                g.blit(pi.loc, 0, 0, pi.x, pi.y, pi.w, pi.h);
                g.pose().popPose();
                y += pi.ah + 2;
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (this.playerKnowledge.isResearchComplete("FIRSTSTEPS")) {
            y = (this.height - this.paneHeight) / 2 + 9;
            int le = this.mouseInside(x - 48, y, 25, 16, mx, my) ? 0 : 3;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawPopupAt(x - 48, y, 25, 16, mx, my, "tc.aspect.name");
            g.blit(tex1, x - 48 + le, y, 76, 232, 24 - le, 16);
            g.blit(tex1, x - 28, y, 100, 232, 4, 16);
        }
        if (this.playerKnowledge.isResearchComplete("KNOWLEDGETYPES") && !this.research.getKey().equals("KNOWLEDGETYPES")) {
            y = (this.height - this.paneHeight) / 2 + 32;
            int le = this.mouseInside(x - 48, y, 25, 16, mx, my) ? 0 : 3;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawPopupAt(x - 48, y, 25, 16, mx, my, "tc.knowledge.name");
            g.blit(tex1, x - 49 + le, y, 44, 232, 24 - le, 16);
            g.blit(tex1, x - 29, y, 100, 232, 4, 16);
        }
        ResearchStage stage = this.research.getStages()[this.currentStage];
        if (stage.getRecipes() != null) {
            this.drawRecipeBookmarks(g, x, mx, my);
        }
        if (this.page == 0 && side == 0 && !this.isComplete) {
            this.drawRequirements(g, x, mx, my, stage);
        }
        if (this.playerKnowledge.isResearchComplete("KNOWLEDGETYPES") && this.research.getKey().equals("KNOWLEDGETYPES")) {
            this.drawKnowledges(g, x, (this.height - this.paneHeight) / 2 - 16 + 210, mx, my, true);
        }
        this.renderingCompound = false;
        if (this.showingAspects) {
            this.drawAspectsInsert(g, mx, my);
        } else if (this.showingKnowledge) {
            this.drawKnowledgesInsert(g, mx, my);
        } else if (shownRecipe != null) {
            this.drawRecipe(g, mx, my);
        } else if (stage.getWarp() > 0 && !this.isComplete) {
            int warp = Math.min(stage.getWarp(), 5);
            GuiResearchBrowser.drawForbidden(g, x - 57, y - 40);
            String s = I18n.get("tc.forbidden.level." + warp);
            g.drawString(this.font, s, x - 56 - this.font.width(s) / 2, y - 43, 11180543, false);
            String text = I18n.get("tc.warp.warn");
            this.drawPopupAtRaw(x - 67, y - 50, 20, 20, mx, my, text.replaceAll("%n", s));
        }
    }

    private void drawKnowledgesInsert(GuiGraphics g, int mx, int my) {
        this.allowWithPagePopup = true;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;
        g.blit(tex4, x, y, 0, 0, 255, 255);
        this.drawKnowledges(g, x + 60, (this.height - this.paneHeight) / 2 + 75, mx, my, false);
        this.allowWithPagePopup = false;
    }

    private void drawKnowledges(GuiGraphics g, int x, int y, int mx, int my, boolean inpage) {
        y -= 18;
        boolean drewSomething = false;
        int tc = 0;
        for (IPlayerKnowledge.EnumKnowledgeType type : IPlayerKnowledge.EnumKnowledgeType.values()) {
            int fc = 0;
            boolean b = false;
            for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                if (type.hasFields() || category == null) {
                    int amt = this.playerKnowledge.getKnowledge(type, category);
                    int par = this.playerKnowledge.getKnowledgeRaw(type, category) % type.getProgression();
                    if (amt > 0 || par > 0) {
                        drewSomething = true;
                        int px = x - 10 + (inpage ? 18 : 23) * fc;
                        int py = y - tc * (inpage ? 20 : 28);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        g.pose().pushPose();
                        g.pose().translate(px, py, 0.0F);
                        g.pose().scale(0.0625F, 0.0625F, 0.0625F);
                        g.blit(HudHandler.KNOW_TYPE[type.ordinal()], 0, 0, 0, 0, 255, 255);
                        if (type.hasFields() && category != null) {
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.75F);
                            g.pose().translate(0.0F, 0.0F, 1.0F);
                            g.pose().scale(0.66F, 0.66F, 0.66F);
                            g.blit(category.icon, 66, 66, 0, 0, 255, 255);
                        }
                        g.pose().popPose();
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        String s = "" + amt;
                        int m = this.font.width(s);
                        g.drawString(this.font, s, px + 16 - m, py + 8, 16777215, true);
                        s = I18n.get("tc.type." + type.toString().toLowerCase(Locale.ROOT));
                        if (type.hasFields() && category != null) {
                            s = s + ": " + I18n.get("tc.research_category." + category.key);
                        }
                        this.drawPopupAtRaw(px, py, 16, 16, mx, my, s);
                        if (par > 0) {
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.75F);
                            int l = (int) ((float) par / type.getProgression() * 16.0F);
                            g.blit(tex1, px, py + 17, 0, 232, l, 2);
                            g.blit(tex1, px + l, py + 17, l, 234, 16 - l, 2);
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        }
                        fc++;
                        b = true;
                    }
                }
            }
            if (b) {
                tc++;
            }
        }
        if (inpage && drewSomething) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.blit(tex1, x + 4, y - tc * 20 + 12, 24, 184, 96, 8);
        }
    }

    private void drawCheck(GuiGraphics g, int x, int y) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 200.0F);
        g.blit(tex1, x, y, 159, 207, 10, 10);
        g.pose().popPose();
    }

    private void drawRequirements(GuiGraphics g, int x, int mx, int my, ResearchStage stage) {
        int y = (this.height - this.paneHeight) / 2 - 16 + 210;
        boolean b = false;
        if (stage.getResearch() != null) {
            y -= 18;
            b = true;
            int shift = 24;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.25F);
            g.blit(tex1, x - 12, y - 1, 200, 232, 56, 16);
            this.drawPopupAt(x - 15, y, 16, 16, mx, my, "tc.need.research");
            if (this.hasResearch != null) {
                if (this.hasResearch.length != stage.getResearch().length) {
                    this.hasResearch = new boolean[stage.getResearch().length];
                }
                int ss = stage.getResearch().length > 6 ? 110 / stage.getResearch().length : 18;
                for (int a = 0; a < stage.getResearch().length; a++) {
                    String key = stage.getResearch()[a];
                    Object loc = dummyResearch;
                    String text = I18n.get("research." + key + ".text");
                    if (key.startsWith("!")) {
                        Aspect as = Aspect.aspects.get(key.replaceAll("!", ""));
                        if (as != null) {
                            loc = as;
                            text = as.getName();
                        }
                    }
                    ResearchEntry re = ResearchCategories.getResearch(key);
                    float[] color = {1.0F, 1.0F, 1.0F};
                    if (re != null && re.getIcons() != null) {
                        int idx = (int) (System.currentTimeMillis() / 1000L % re.getIcons().length);
                        loc = re.getIcons()[idx];
                        text = I18n.get(re.getName());
                    } else if (key.startsWith("m_")) {
                        loc = dummyMap;
                    } else if (key.startsWith("c_")) {
                        loc = dummyChest;
                    } else if (key.startsWith("f_")) {
                        loc = dummyFlask;
                    } else if (!(loc instanceof Aspect)) {
                        color = new float[] {0.5F, 0.75F, 1.0F};
                    }
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    if (loc instanceof Aspect asp) {
                        Color cc = new Color(asp.getColor());
                        RenderSystem.setShaderColor(cc.getRed() / 255.0F, cc.getGreen() / 255.0F, cc.getBlue() / 255.0F, 1.0F);
                        RenderSystem.setShaderTexture(0, asp.getImage());
                        UtilsFX.drawTexturedQuadFull(g.pose(), x - 15 + shift, y, 0.0F);
                    } else if (loc instanceof ResourceLocation rl) {
                        RenderSystem.setShaderColor(color[0], color[1], color[2], 1.0F);
                        RenderSystem.setShaderTexture(0, rl);
                        UtilsFX.drawTexturedQuadFull(g.pose(), x - 15 + shift, y, 0.0F);
                    } else if (loc instanceof ItemStack is) {
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        g.renderItem(is, x - 15 + shift, y);
                    }
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    if (this.hasResearch[a]) {
                        this.drawCheck(g, x - 15 + shift + 8, y);
                    }
                    this.drawPopupAtRaw(x - 15 + shift, y, 16, 16, mx, my, text);
                    shift += ss;
                }
            }
        }
        if (stage.getObtain() != null) {
            y -= 18;
            b = true;
            int shift = 24;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.25F);
            g.blit(tex1, x - 12, y - 1, 200, 216, 56, 16);
            this.drawPopupAt(x - 15, y, 16, 16, mx, my, "tc.need.obtain");
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.hasItem != null) {
                if (this.hasItem.length != stage.getObtain().length) {
                    this.hasItem = new boolean[stage.getObtain().length];
                }
                int ss = stage.getObtain().length > 6 ? 110 / stage.getObtain().length : 18;
                for (int idx = 0; idx < stage.getObtain().length; idx++) {
                    this.drawStackAt(g, stage.getObtain()[idx], x - 15 + shift, y, mx, my, true);
                    if (this.hasItem[idx]) {
                        this.drawCheck(g, x - 15 + shift + 8, y);
                    }
                    shift += ss;
                }
            }
        }
        if (stage.getCraft() != null) {
            y -= 18;
            b = true;
            int shift = 24;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.25F);
            g.blit(tex1, x - 12, y - 1, 200, 200, 56, 16);
            this.drawPopupAt(x - 15, y, 16, 16, mx, my, "tc.need.craft");
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.hasCraft != null) {
                if (this.hasCraft.length != stage.getCraft().length) {
                    this.hasCraft = new boolean[stage.getCraft().length];
                }
                int ss = stage.getCraft().length > 6 ? 110 / stage.getCraft().length : 18;
                for (int idx = 0; idx < stage.getCraft().length; idx++) {
                    this.drawStackAt(g, stage.getCraft()[idx], x - 15 + shift, y, mx, my, true);
                    if (this.hasCraft[idx]) {
                        this.drawCheck(g, x - 15 + shift + 8, y);
                    }
                    shift += ss;
                }
            }
        }
        if (stage.getKnow() != null) {
            y -= 18;
            b = true;
            int shift = 24;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.25F);
            g.blit(tex1, x - 12, y - 1, 200, 184, 56, 16);
            this.drawPopupAt(x - 15, y, 16, 16, mx, my, "tc.need.know");
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            if (this.hasKnow != null) {
                if (this.hasKnow.length != stage.getKnow().length) {
                    this.hasKnow = new boolean[stage.getKnow().length];
                }
                int ss = stage.getKnow().length > 6 ? 110 / stage.getKnow().length : 18;
                for (int idx = 0; idx < stage.getKnow().length; idx++) {
                    ResearchStage.Knowledge kn = stage.getKnow()[idx];
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    g.pose().pushPose();
                    g.pose().translate(x - 15 + shift, y, 0.0F);
                    g.pose().scale(0.0625F, 0.0625F, 0.0625F);
                    g.blit(HudHandler.KNOW_TYPE[kn.type.ordinal()], 0, 0, 0, 0, 255, 255);
                    if (kn.type.hasFields() && kn.category != null) {
                        g.pose().translate(32.0F, 32.0F, 1.0F);
                        g.pose().scale(0.75F, 0.75F, 0.75F);
                        g.blit(kn.category.icon, 0, 0, 0, 0, 255, 255);
                    }
                    g.pose().popPose();
                    String am = (!this.hasKnow[idx] ? ChatFormatting.RED.toString() : "") + kn.amount;
                    g.pose().pushPose();
                    g.pose().translate(x - 15 + shift + 16 - this.font.width(am) / 2, y + 12, 5.0F);
                    g.pose().scale(0.5F, 0.5F, 0.5F);
                    g.drawString(this.font, am, 0, 0, 16777215, true);
                    g.pose().popPose();
                    if (this.hasKnow[idx]) {
                        this.drawCheck(g, x - 15 + shift + 8, y);
                    }
                    String s = I18n.get("tc.type." + kn.type.toString().toLowerCase(Locale.ROOT));
                    if (kn.type.hasFields() && kn.category != null) {
                        s = s + ": " + I18n.get("tc.research_category." + kn.category.key);
                    }
                    this.drawPopupAtRaw(x - 15 + shift, y, 16, 16, mx, my, s);
                    shift += ss;
                }
            }
        }
        if (b) {
            y -= 12;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.blit(tex1, x + 4, y - 2, 24, 184, 96, 8);
            if (this.hasAllRequisites) {
                this.hrx = x + 20;
                this.hry = y - 6;
                if (this.hold) {
                    String s = I18n.get("tc.stage.hold");
                    int m = this.font.width(s);
                    g.drawString(this.font, s, (int) (x + 52 - m / 2.0F), y - 4, 16777215, true);
                } else {
                    if (this.mouseInside(this.hrx, this.hry, 64, 12, mx, my)) {
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    } else {
                        RenderSystem.setShaderColor(0.8F, 0.8F, 0.9F, 1.0F);
                    }
                    g.blit(tex1, this.hrx, this.hry, 84, 216, 64, 12);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    String s = I18n.get("tc.stage.complete");
                    int m = this.font.width(s);
                    g.drawString(this.font, s, (int) (x + 52 - m / 2.0F), y - 4, 16777215, true);
                }
            }
        }
    }

    private void drawRecipeBookmarks(GuiGraphics g, int x, int mx, int my) {
        Random rng = new Random(this.rhash);
        int y = (this.height - this.paneHeight) / 2 - 8;
        this.allowWithPagePopup = true;
        if (!this.recipeOutputs.isEmpty()) {
            int space = Math.min(25, 200 / this.recipeOutputs.size());
            for (ResourceLocation rk : this.recipeOutputs.keySet()) {
                List<Object> list = this.recipeOutputs.get(rk);
                if (list != null && !list.isEmpty()) {
                    int i = Math.floorMod(this.cycle, list.size());
                    if (list.get(i) != null) {
                        int sh = rng.nextInt(3);
                        int le = rng.nextInt(3) + (this.mouseInside(x + 280, y - 1, 30, 16, mx, my) ? 0 : 3);
                        if (rk.equals(shownRecipe)) {
                            RenderSystem.setShaderColor(1.0F, 0.5F, 0.5F, 1.0F);
                        } else {
                            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        }
                        RenderSystem.enableBlend();
                        g.blit(tex1, x + 280 + sh, y - 1, 120 + le, 232, 28, 16);
                        g.blit(tex1, x + 280 + sh, y - 1, 116, 232, 4, 16);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        if (list.get(i) instanceof ItemStack stack) {
                            this.drawStackAt(g, stack, x + 287 + sh - le, y - 1, mx, my, false, true);
                        }
                        // TODO: многоблочные схемы (Part[][][]) — с их этапами
                        y += space;
                    }
                }
            }
        }
        this.allowWithPagePopup = false;
    }

    private void generateRecipesLists(ResearchStage stage, ResearchAddendum[] addenda) {
        this.recipeLists.clear();
        this.recipeOutputs.clear();
        if (stage != null && stage.getRecipes() != null) {
            for (ResourceLocation rk : stage.getRecipes()) {
                this.addRecipesToList(rk, this.recipeLists, this.recipeOutputs, rk);
            }
            if (addenda != null) {
                for (ResearchAddendum addendum : addenda) {
                    if (addendum.getRecipes() != null && ThaumcraftCapabilities.knowsResearchStrict(this.minecraft.player, addendum.getResearch())) {
                        for (ResourceLocation rk : addendum.getRecipes()) {
                            this.addRecipesToList(rk, this.recipeLists, this.recipeOutputs, rk);
                        }
                    }
                }
            }
        }
    }

    private void addRecipesToList(ResourceLocation rk, LinkedHashMap<ResourceLocation, List<Object>> recipeLists2,
                                  LinkedHashMap<ResourceLocation, List<Object>> recipeOutputs2, ResourceLocation rkey) {
        Object recipe = ResearchRecipes.resolve(rk);
        if (recipe == null) {
            return;
        }
        if (recipe instanceof List<?> group) {
            for (Object rl : group) {
                this.addRecipesToList((ResourceLocation) rl, recipeLists2, recipeOutputs2, rk);
            }
            return;
        }
        List<Object> list = recipeLists2.computeIfAbsent(rkey, k -> new ArrayList<>());
        List<Object> outputs = recipeOutputs2.computeIfAbsent(rkey, k -> new ArrayList<>());
        // TODO этапы 9, 11: BluePrint, CrucibleRecipe, InfusionRecipe, RecipeMisc (с проверкой исследования)
        Recipe<?> r = recipe instanceof RecipeHolder<?> holder ? holder.value() : recipe instanceof Recipe<?> rr ? rr : null;
        if (r instanceof IArcaneRecipe re) {
            ItemStack is = re.getResultItem(this.minecraft.level.registryAccess());
            if (!is.isEmpty() && ThaumcraftCapabilities.knowsResearchStrict(this.minecraft.player, re.getResearch())) {
                list.add(re);
                outputs.add(is);
            }
        } else if (r != null) {
            list.add(r);
            outputs.add(r.getResultItem(this.minecraft.level.registryAccess()));
        }
    }

    private void drawRecipe(GuiGraphics g, int mx, int my) {
        this.allowWithPagePopup = true;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;
        g.blit(tex4, x, y, 0, 0, 255, 255);
        List<Object> list = this.recipeLists.get(shownRecipe);
        if (list == null || list.isEmpty()) {
            list = this.drilldownLists.get(shownRecipe);
        }
        if (list != null && !list.isEmpty()) {
            this.hasRecipePages = list.size() > 1;
            this.recipePageMax = list.size() - 1;
            if (this.recipePage > this.recipePageMax) {
                this.recipePage = this.recipePageMax;
            }
            Object recipe = list.get(this.recipePage % list.size());
            // TODO этапы 9, 11: тигель, инфузия, многоблочные конструкции
            if (recipe instanceof IArcaneRecipe ar) {
                this.drawArcaneCraftingPage(g, x + 128, y + 128, mx, my, ar);
            } else if (recipe instanceof Recipe<?> r) {
                this.drawCraftingPage(g, x + 128, y + 128, mx, my, r);
            }
            if (this.hasRecipePages) {
                float bob = Mth.sin(this.minecraft.player.tickCount / 3.0F) * 0.2F + 0.1F;
                if (this.recipePage > 0) {
                    this.drawTexturedModalRectScaled(g, tex1, x + 40, y + 232, 0, 184, 12, 8, bob);
                }
                if (this.recipePage < this.recipePageMax) {
                    this.drawTexturedModalRectScaled(g, tex1, x + 204, y + 232, 12, 184, 12, 8, bob);
                }
            }
        }
        this.allowWithPagePopup = false;
    }

    private void drawAspectsInsert(GuiGraphics g, int mx, int my) {
        this.allowWithPagePopup = true;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        int x = (this.width - 256) / 2;
        int y = (this.height - 256) / 2;
        g.blit(tex4, x, y, 0, 0, 255, 255);
        this.drawAspectPage(g, x + 60, y + 24, mx, my);
        this.allowWithPagePopup = false;
    }

    private void drawAspectPage(GuiGraphics g, int x, int y, int mx, int my) {
        if (this.knownPlayerAspects == null || this.knownPlayerAspects.size() <= 0) {
            return;
        }
        int count = -1;
        int start = aspectsPage * 5;
        for (Aspect aspect : this.knownPlayerAspects.getAspectsSortedByName()) {
            if (++count < start) {
                continue;
            }
            if (count > start + 4) {
                break;
            }
            if (aspect.getImage() == null) {
                continue;
            }
            int ty = y + count % 5 * 40;
            if (mx >= x && my >= ty && mx < x + 40 && my < ty + 40) {
                g.pose().pushPose();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                g.pose().translate(x - 2, ty - 2, 0.0);
                g.pose().scale(2.0F, 2.0F, 1.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
                RenderSystem.setShaderTexture(0, tex3);
                UtilsFX.drawTexturedQuadFull(g.pose(), 0.0F, 0.0F, 0.0F);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                g.pose().popPose();
            }
            g.pose().pushPose();
            g.pose().translate(x + 2, y + 2 + count % 5 * 40, 0.0);
            g.pose().scale(1.5F, 1.5F, 1.5F);
            UtilsFX.drawTag(g, 0, 0, aspect, 0.0F, 0);
            g.pose().popPose();
            this.drawSmallCentered(g, aspect.getName(), x + 16, y + 29 + count % 5 * 40);
            if (aspect.getComponents() != null) {
                for (int c = 0; c < 2; c++) {
                    Aspect comp = aspect.getComponents()[c];
                    g.pose().pushPose();
                    g.pose().translate(x + (c == 0 ? 60 : 102), y + 4 + count % 5 * 40, 0.0);
                    g.pose().scale(1.25F, 1.25F, 1.25F);
                    if (this.playerKnowledge.isResearchKnown("!" + comp.getTag().toLowerCase(Locale.ROOT))) {
                        UtilsFX.drawTag(g, 0, 0, comp, 0.0F, 0);
                    } else {
                        RenderSystem.enableBlend();
                        RenderSystem.setShaderTexture(0, dummyResearch);
                        RenderSystem.setShaderColor(0.8F, 0.8F, 0.8F, 1.0F);
                        UtilsFX.drawTexturedQuadFull(g.pose(), 0.0F, 0.0F, 0.0F);
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                    g.pose().popPose();
                }
                for (int c = 0; c < 2; c++) {
                    Aspect comp = aspect.getComponents()[c];
                    if (this.playerKnowledge.isResearchKnown("!" + comp.getTag().toLowerCase(Locale.ROOT))) {
                        this.drawSmallCentered(g, comp.getName(), x + 22 + (c == 0 ? 50 : 92), y + 29 + count % 5 * 40);
                    }
                }
                g.drawString(this.font, "=", x + 9 + 32, y + 12 + count % 5 * 40, 10066329, false);
                g.drawString(this.font, "+", x + 10 + 79, y + 12 + count % 5 * 40, 10066329, false);
            } else {
                g.drawString(this.font, I18n.get("tc.aspect.primal"), x + 54, y + 12 + count % 5 * 40, 7829367, false);
            }
        }
        float bob = Mth.sin(this.minecraft.player.tickCount / 3.0F) * 0.2F + 0.1F;
        if (aspectsPage > 0) {
            this.drawTexturedModalRectScaled(g, tex1, x - 20, y + 208, 0, 184, 12, 8, bob);
        }
        if (aspectsPage < this.maxAspectPages - 1) {
            this.drawTexturedModalRectScaled(g, tex1, x + 144, y + 208, 12, 184, 12, 8, bob);
        }
    }

    private void drawSmallCentered(GuiGraphics g, String text, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0);
        g.pose().scale(0.5F, 0.5F, 0.5F);
        g.drawString(this.font, text, -this.font.width(text) / 2, 0, 5263440, false);
        g.pose().popPose();
    }

    /** Ингредиент рецепта, сменяющийся раз в секунду (InventoryUtils.cycleItemStack). */
    private static ItemStack cycleItemStack(Ingredient ing, int counter) {
        ItemStack[] q = ing.getItems();
        if (q.length == 0) {
            return ItemStack.EMPTY;
        }
        int idx = (int) ((counter + System.currentTimeMillis() / 1000L) % q.length);
        return q[idx];
    }

    /** Страница рецепта магического верстака: сетка, стоимость в Вис (с подсказкой) и нужные кристаллы. */
    private void drawArcaneCraftingPage(GuiGraphics g, int x, int y, int mx, int my, IArcaneRecipe recipe) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.blit(tex2, -26, -26, 112, 15, 52, 52);
        g.blit(tex2, -8, -46, 20, 3, 16, 16);
        g.pose().popPose();
        g.pose().pushPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.4F);
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.blit(tex2, -6, 40, 68, 76, 12, 12);
        g.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        String text = "" + recipe.getVis();
        int offset = this.font.width(text);
        g.drawString(this.font, text, x - offset / 2, y + 90, 5263440, false);
        this.drawPopupAt(x - offset / 2 - 15, y + 75, 30, 30, mx, my, "wandtable.text1");
        this.drawStackAt(g, recipe.getResultItem(this.minecraft.level.registryAccess()), x - 8, y - 84, mx, my, false);
        AspectList crystals = recipe.getCrystals();
        if (crystals != null) {
            int a = 0;
            int sz = crystals.size();
            for (Aspect aspect : crystals.getAspects()) {
                this.drawStackAt(g, ThaumcraftApiHelper.makeCrystal(aspect, crystals.getAmount(aspect)), x + 4 - sz * 10 + a * 20, y + 59, mx, my, true);
                a++;
            }
        }
        if (recipe instanceof ShapedArcaneRecipe shaped) {
            text = I18n.get("recipe.type.arcane");
            offset = this.font.width(text);
            g.drawString(this.font, text, x - offset / 2, y - 104, 5263440, false);
            int rw = shaped.getWidth();
            int rh = shaped.getHeight();
            List<Ingredient> items = shaped.getIngredients();
            for (int i = 0; i < rw && i < 3; i++) {
                for (int j = 0; j < rh && j < 3; j++) {
                    Ingredient ing = items.get(i + j * rw);
                    if (ing != null) {
                        this.drawStackAt(g, cycleItemStack(ing, i + j * rw), x - 40 + i * 32, y - 40 + j * 32, mx, my, true);
                    }
                }
            }
        }
        if (recipe instanceof ShapelessArcaneRecipe) {
            text = I18n.get("recipe.type.arcane.shapeless");
            offset = this.font.width(text);
            g.drawString(this.font, text, x - offset / 2, y - 104, 5263440, false);
            List<Ingredient> items = recipe.getIngredients();
            for (int i = 0; i < items.size() && i < 9; i++) {
                if (items.get(i) != null) {
                    this.drawStackAt(g, cycleItemStack(items.get(i), i), x - 40 + i % 3 * 32, y - 40 + i / 3 * 32, mx, my, true);
                }
            }
        }
    }

    private void drawCraftingPage(GuiGraphics g, int x, int y, int mx, int my, Recipe<?> recipe) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.pose().pushPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(2.0F, 2.0F, 1.0F);
        g.blit(tex2, -26, -26, 60, 15, 51, 52);
        g.blit(tex2, -8, -46, 20, 3, 16, 16);
        g.pose().popPose();
        this.drawStackAt(g, recipe.getResultItem(this.minecraft.level.registryAccess()), x - 8, y - 84, mx, my, false);
        if (recipe instanceof ShapedRecipe shaped) {
            String text = I18n.get("recipe.type.workbench");
            int offset = this.font.width(text);
            g.drawString(this.font, text, x - offset / 2, y - 104, 5263440, false);
            int rw = shaped.getWidth();
            int rh = shaped.getHeight();
            List<Ingredient> items = shaped.getIngredients();
            for (int i = 0; i < rw && i < 3; i++) {
                for (int j = 0; j < rh && j < 3; j++) {
                    Ingredient ing = items.get(i + j * rw);
                    if (ing != null) {
                        this.drawStackAt(g, cycleItemStack(ing, i + j * rw), x - 40 + i * 32, y - 40 + j * 32, mx, my, true);
                    }
                }
            }
        }
        if (recipe instanceof ShapelessRecipe) {
            String text = I18n.get("recipe.type.workbenchshapeless");
            int offset = this.font.width(text);
            g.drawString(this.font, text, x - offset / 2, y - 104, 5263440, false);
            List<Ingredient> items = recipe.getIngredients();
            for (int i = 0; i < items.size() && i < 9; i++) {
                if (items.get(i) != null) {
                    this.drawStackAt(g, cycleItemStack(items.get(i), i), x - 40 + i % 3 * 32, y - 40 + i / 3 * 32, mx, my, true);
                }
            }
        }
    }

    // --- ввод -----------------------------------------------------------------------------------------------------------

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.minecraft.options.keyInventory.matches(keyCode, scanCode) || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            history.clear();
            if (shownRecipe == null && !this.showingAspects && !this.showingKnowledge) {
                this.minecraft.setScreen(new GuiResearchBrowser(this.guiMapX, this.guiMapY));
            } else {
                shownRecipe = null;
                this.showingAspects = false;
                this.showingKnowledge = false;
                this.minecraft.player.playSound(SoundsTC.page.get(), 0.4F, 1.1F);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            this.prevPage();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            this.nextPage();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            this.goBack();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void nextPage() {
        if (this.page < this.maxPages - 2) {
            this.page += 2;
            this.lastCycle = 0L;
            this.cycle = -1;
            this.minecraft.player.playSound(SoundsTC.page.get(), 0.66F, 1.0F);
        }
    }

    private void prevPage() {
        if (this.page >= 2) {
            this.page -= 2;
            this.lastCycle = 0L;
            this.cycle = -1;
            this.minecraft.player.playSound(SoundsTC.page.get(), 0.66F, 1.0F);
        }
    }

    private void goBack() {
        if (!history.isEmpty()) {
            this.minecraft.player.playSound(SoundsTC.page.get(), 0.66F, 1.0F);
            shownRecipe = history.pop();
        } else {
            shownRecipe = null;
        }
    }

    @Override
    public boolean mouseClicked(double mxd, double myd, int button) {
        int par1 = (int) mxd;
        int par2 = (int) myd;
        this.checkRequisites();
        int var4 = (this.width - this.paneWidth) / 2;
        int var5 = (this.height - this.paneHeight) / 2;
        int mx = par1 - this.hrx;
        int my = par2 - this.hry;
        if (shownRecipe == null && !this.hold && this.hasAllRequisites && mx >= 0 && my >= 0 && mx < 64 && my < 12) {
            PacketDistributor.sendToServer(new PacketSyncProgressToServer(this.research.getKey(), false, true, true));
            this.minecraft.player.playSound(SoundsTC.write.get(), 0.66F, 1.0F);
            this.lastCheck = 0L;
            this.lastStage = this.currentStage;
            this.hold = true;
            this.keyCache.clear();
            this.drilldownLists.clear();
        }
        if (this.knownPlayerAspects != null && this.playerKnowledge.isResearchComplete("FIRSTSTEPS")) {
            mx = par1 - (var4 - 48);
            my = par2 - (var5 + 8);
            if (mx >= 0 && my >= 0 && mx < 25 && my < 16) {
                shownRecipe = null;
                this.showingKnowledge = false;
                this.showingAspects = !this.showingAspects;
                history.clear();
                if (aspectsPage > this.maxAspectPages) {
                    aspectsPage = 0;
                }
                this.minecraft.player.playSound(SoundsTC.page.get(), 0.7F, 0.9F);
            }
        }
        if (this.playerKnowledge.isResearchComplete("KNOWLEDGETYPES") && !this.research.getKey().equals("KNOWLEDGETYPES")) {
            mx = par1 - (var4 - 48);
            my = par2 - (var5 + 31);
            if (mx >= 0 && my >= 0 && mx < 25 && my < 16) {
                shownRecipe = null;
                this.showingAspects = false;
                this.showingKnowledge = !this.showingKnowledge;
                history.clear();
                this.minecraft.player.playSound(SoundsTC.page.get(), 0.7F, 0.9F);
            }
        }
        mx = par1 - (var4 + 205);
        my = par2 - (var5 + 192);
        if (this.showingAspects && aspectsPage < this.maxAspectPages - 1 && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            aspectsPage++;
            this.minecraft.player.playSound(SoundsTC.page.get(), 0.7F, 0.9F);
        }
        mx = par1 - (var4 + 38);
        my = par2 - (var5 + 192);
        if (this.showingAspects && aspectsPage > 0 && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            aspectsPage--;
            this.minecraft.player.playSound(SoundsTC.page.get(), 0.7F, 0.9F);
        }
        if (!this.recipeLists.isEmpty()) {
            int aa = 0;
            int space = Math.min(25, 200 / this.recipeLists.size());
            for (ResourceLocation rk : this.recipeLists.keySet()) {
                mx = par1 - (var4 + 280);
                my = par2 - (var5 - 8 + aa * space);
                if (mx >= 0 && my >= 0 && mx < 30 && my < 16) {
                    if (rk.equals(shownRecipe)) {
                        shownRecipe = null;
                    } else {
                        shownRecipe = rk;
                    }
                    this.showingAspects = false;
                    this.showingKnowledge = false;
                    history.clear();
                    this.minecraft.player.playSound(SoundsTC.page.get(), 0.7F, 0.9F);
                    break;
                }
                aa++;
            }
        }
        mx = par1 - (var4 + 205);
        my = par2 - (var5 + 192);
        if (this.hasRecipePages && this.recipePage < this.recipePageMax && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            this.recipePage++;
            this.minecraft.player.playSound(SoundsTC.page.get(), 0.7F, 0.9F);
        }
        mx = par1 - (var4 + 38);
        my = par2 - (var5 + 192);
        if (this.hasRecipePages && this.recipePage > 0 && mx >= 0 && my >= 0 && mx < 14 && my < 14) {
            this.recipePage--;
            this.minecraft.player.playSound(SoundsTC.page.get(), 0.7F, 0.9F);
        }
        mx = par1 - (var4 + 261);
        my = par2 - (var5 + 189);
        if (shownRecipe == null && mx >= 0 && my >= 0 && mx < 14 && my < 10) {
            this.nextPage();
        }
        mx = par1 - (var4 - 17);
        my = par2 - (var5 + 189);
        if (shownRecipe == null && mx >= 0 && my >= 0 && mx < 14 && my < 10) {
            this.prevPage();
        }
        mx = par1 - (var4 + 118);
        my = par2 - (var5 + 190);
        if (mx >= 0 && my >= 0 && mx < 20 && my < 12) {
            this.goBack();
        }
        mx = par1 - (var4 + 210);
        my = par2 - (var5 + 190);
        if (this.renderingCompound && mx >= 0 && my >= 0 && mx < 10 && my < 10) {
            this.minecraft.player.playSound(SoundsTC.clack.get(), 0.66F, 1.0F);
            cycleMultiblockLines = !cycleMultiblockLines;
        }
        for (Object[] coords : this.reference) {
            int rx = (Integer) coords[0];
            int ry = (Integer) coords[1];
            if (par1 >= rx && par2 >= ry && par1 < rx + 16 && par2 < ry + 16) {
                this.minecraft.player.playSound(SoundsTC.page.get(), 0.66F, 1.0F);
                if (shownRecipe != null) {
                    history.push(shownRecipe);
                }
                shownRecipe = (ResourceLocation) coords[2];
                this.recipePage = Integer.parseInt((String) coords[3]);
                if (!this.drilldownLists.containsKey(shownRecipe)) {
                    this.addRecipesToList(shownRecipe, this.drilldownLists, new LinkedHashMap<>(), shownRecipe);
                }
                break;
            }
        }
        return super.mouseClicked(mxd, myd, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --- подсказки и предметы -------------------------------------------------------------------------------------------

    /** drawPopupAt с ключом перевода. */
    void drawPopupAt(int x, int y, int w, int h, int mx, int my, String key) {
        this.drawPopupAtRaw(x, y, w, h, mx, my, I18n.get(key));
    }

    /** drawPopupAt с готовым текстом (в оригинале translateToLocal от уже переведённой строки возвращал её же). */
    void drawPopupAtRaw(int x, int y, int w, int h, int mx, int my, String text) {
        if ((shownRecipe == null || this.allowWithPagePopup) && mx >= x && my >= y && mx < x + w && my < y + h) {
            List<String> s = new ArrayList<>();
            s.add(text);
            this.tipText = s;
        }
    }

    boolean mouseInside(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    void drawStackAt(GuiGraphics g, ItemStack itemstack, int x, int y, int mx, int my, boolean clickthrough) {
        this.drawStackAt(g, itemstack, x, y, mx, my, clickthrough, false);
    }

    /** UtilsFX.renderItemStack (z + 32, с надписью количества, если не hideOverlay) и подсказка предмета. */
    void drawStackAt(GuiGraphics g, ItemStack itemstack, int x, int y, int mx, int my, boolean clickthrough, boolean hideOverlay) {
        if (itemstack != null && !itemstack.isEmpty()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 32.0F);
            g.renderItem(itemstack, x, y);
            if (!hideOverlay) {
                g.renderItemDecorations(this.font, itemstack, x, y);
            }
            g.pose().popPose();
        }
        if ((shownRecipe == null || this.allowWithPagePopup) && mx >= x && my >= y && mx < x + 16 && my < y + 16 && itemstack != null
                && !itemstack.isEmpty()) {
            List<String> addtext = new ArrayList<>();
            for (Component c : itemstack.getTooltipLines(Item.TooltipContext.of(this.minecraft.level), this.minecraft.player,
                    this.minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL)) {
                addtext.add(UtilsFX.toLegacy(c));
            }
            if (clickthrough) {
                String ref = this.getCraftingRecipeKey(itemstack);
                if (ref != null) {
                    String[] sr = ref.split(";", 2);
                    if (sr.length > 1) {
                        addtext.add(ChatFormatting.BLUE + "" + ChatFormatting.ITALIC + I18n.get("recipe.clickthrough"));
                        this.reference.add(new Object[] {mx, my, ResourceLocation.parse(sr[0]), sr[1]});
                    } else if (sr[0].equals("UNKNOWN")) {
                        // в оригинале не выводилось: ключ без ";" не доходил до проверки (DEVIATIONS.md)
                        addtext.add(ChatFormatting.DARK_RED + "" + ChatFormatting.ITALIC + I18n.get("recipe.unknown"));
                    }
                }
            }
            this.tipText = addtext;
        }
    }

    public void drawTexturedModalRectScaled(GuiGraphics g, ResourceLocation tex, int par1, int par2, int par3, int par4, int par5, int par6,
                                            float scale) {
        g.pose().pushPose();
        float var7x = 0.00390625F;
        float var8 = 0.00390625F;
        g.pose().translate(par1 + par5 / 2.0F, par2 + par6 / 2.0F, 0.0F);
        g.pose().scale(1.0F + scale, 1.0F + scale, 1.0F);
        Matrix4f m = g.pose().last().pose();
        RenderSystem.setShaderTexture(0, tex);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(m, -par5 / 2.0F, par6 / 2.0F, 0.0F).setUv(par3 * var7x, (par4 + par6) * var8);
        b.addVertex(m, par5 / 2.0F, par6 / 2.0F, 0.0F).setUv((par3 + par5) * var7x, (par4 + par6) * var8);
        b.addVertex(m, par5 / 2.0F, -par6 / 2.0F, 0.0F).setUv((par3 + par5) * var7x, par4 * var8);
        b.addVertex(m, -par5 / 2.0F, -par6 / 2.0F, 0.0F).setUv(par3 * var7x, par4 * var8);
        BufferUploader.drawWithShader(b.buildOrThrow());
        g.pose().popPose();
    }

    // --- разбор страниц -------------------------------------------------------------------------------------------------

    private void parsePages() {
        this.checkRequisites();
        this.pages.clear();
        if (this.research.getStages() == null) {
            return;
        }
        boolean complete = false;
        this.currentStage = ThaumcraftCapabilities.getKnowledge(this.minecraft.player).getResearchStage(this.research.getKey()) - 1;
        while (this.currentStage >= this.research.getStages().length) {
            this.currentStage--;
            complete = true;
        }
        if (this.currentStage < 0) {
            this.currentStage = 0;
        }
        ResearchStage stage = this.research.getStages()[this.currentStage];
        ResearchAddendum[] addenda = null;
        if (this.research.getAddenda() != null && complete) {
            addenda = this.research.getAddenda();
        }
        this.generateRecipesLists(stage, addenda);
        String rawText = I18n.get(stage.getText());
        if (addenda != null) {
            int ac = 0;
            for (ResearchAddendum addendum : addenda) {
                if (ThaumcraftCapabilities.knowsResearchStrict(this.minecraft.player, addendum.getResearch())) {
                    String text = I18n.get("tc.addendumtext", ++ac);
                    rawText = rawText + "<PAGE>" + text + "<BR>" + I18n.get(addendum.getText());
                }
            }
        }
        rawText = rawText.replaceAll("<BR>", "~B\n\n");
        rawText = rawText.replaceAll("<BR/>", "~B\n\n");
        rawText = rawText.replaceAll("<LINE>", "~L");
        rawText = rawText.replaceAll("<LINE/>", "~L");
        rawText = rawText.replaceAll("<DIV>", "~D");
        rawText = rawText.replaceAll("<DIV/>", "~D");
        rawText = rawText.replaceAll("<PAGE>", "~P");
        rawText = rawText.replaceAll("<PAGE/>", "~P");
        List<PageImage> images = new ArrayList<>();
        String[] imgSplit = rawText.split("<IMG>");
        for (String s : imgSplit) {
            int i = s.indexOf("</IMG>");
            if (i >= 0) {
                String clean = s.substring(0, i);
                PageImage pi = PageImage.parse(clean);
                if (pi == null) {
                    rawText = rawText.replaceFirst(java.util.regex.Pattern.quote(clean), "\n");
                } else {
                    images.add(pi);
                    rawText = rawText.replaceFirst(java.util.regex.Pattern.quote(clean), "~I");
                }
            }
        }
        rawText = rawText.replaceAll("<IMG>", "");
        rawText = rawText.replaceAll("</IMG>", "");
        List<String> firstPassText = new ArrayList<>();
        String[] temp = rawText.split("~P");
        for (int a = 0; a < temp.length; a++) {
            String[] temp1 = temp[a].split("~D");
            for (int x = 0; x < temp1.length; x++) {
                String[] temp2 = temp1[x].split("~L");
                for (int b = 0; b < temp2.length; b++) {
                    String[] temp3 = temp2[b].split("~I");
                    for (int c = 0; c < temp3.length; c++) {
                        firstPassText.add(temp3[c]);
                        if (c != temp3.length - 1) {
                            firstPassText.add("~I");
                        }
                    }
                    if (b != temp2.length - 1) {
                        firstPassText.add("~L");
                    }
                }
                if (x != temp1.length - 1) {
                    firstPassText.add("~D");
                }
            }
            if (a != temp.length - 1) {
                firstPassText.add("~P");
            }
        }
        List<String> parsedText = new ArrayList<>();
        for (String s : firstPassText) {
            parsedText.addAll(UtilsFX.listFormattedStringToWidth(this.font, s, 140));
        }
        int lineHeight = this.font.lineHeight;
        int heightRemaining = 182;
        int dividerSpace = 0;
        if (this.research.getKey().equals("KNOWLEDGETYPES")) {
            heightRemaining -= 2;
            int tc = 0;
            for (IPlayerKnowledge.EnumKnowledgeType type : IPlayerKnowledge.EnumKnowledgeType.values()) {
                for (ResearchCategory category : ResearchCategories.researchCategories.values()) {
                    if (type.hasFields() || category == null) {
                        if (this.playerKnowledge.getKnowledgeRaw(type, category) > 0) {
                            tc++;
                            break;
                        }
                    }
                }
            }
            heightRemaining -= 20 * tc;
            dividerSpace = 12;
        }
        if (!this.isComplete) {
            if (stage.getCraft() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
            if (stage.getObtain() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
            if (stage.getKnow() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
            if (stage.getResearch() != null) {
                heightRemaining -= 18;
                dividerSpace = 15;
            }
        }
        heightRemaining -= dividerSpace;
        Page page1 = new Page();
        List<PageImage> tempImages = new ArrayList<>();
        for (String line : parsedText) {
            if (line.contains("~I")) {
                if (!images.isEmpty()) {
                    tempImages.add(images.remove(0));
                }
                line = "";
            }
            if (line.contains("~L")) {
                tempImages.add(PILINE);
                line = "";
            }
            if (line.contains("~D")) {
                tempImages.add(PIDIV);
                line = "";
            }
            if (line.contains("~P")) {
                heightRemaining = 210;
                this.pages.add(page1.copy());
                page1 = new Page();
                line = "";
            }
            if (!line.isEmpty()) {
                line = line.trim();
                page1.contents.add(line);
                heightRemaining -= lineHeight;
                if (line.endsWith("~B")) {
                    heightRemaining = (int) (heightRemaining - lineHeight * 0.66);
                }
            }
            while (!tempImages.isEmpty() && heightRemaining >= tempImages.get(0).ah + 2) {
                heightRemaining -= tempImages.get(0).ah + 2;
                page1.contents.add(tempImages.remove(0));
            }
            if (heightRemaining < lineHeight && !page1.contents.isEmpty()) {
                heightRemaining = 210;
                this.pages.add(page1.copy());
                page1 = new Page();
            }
        }
        if (!page1.contents.isEmpty()) {
            this.pages.add(page1.copy());
        }
        page1 = new Page();
        heightRemaining = 210;
        while (!tempImages.isEmpty()) {
            if (heightRemaining < tempImages.get(0).ah + 2) {
                heightRemaining = 210;
                this.pages.add(page1.copy());
                page1 = new Page();
            } else {
                heightRemaining -= tempImages.get(0).ah + 2;
                page1.contents.add(tempImages.remove(0));
            }
        }
        if (!page1.contents.isEmpty()) {
            this.pages.add(page1.copy());
        }
        this.rhash = this.research.getKey().hashCode() + this.currentStage * 50;
        this.maxPages = this.pages.size();
    }

    private void checkRequisites() {
        if (this.research.getStages() == null) {
            return;
        }
        this.isComplete = this.playerKnowledge.isResearchComplete(this.research.getKey());
        while (this.currentStage >= this.research.getStages().length) {
            this.currentStage--;
        }
        if (this.currentStage < 0) {
            return;
        }
        this.hasAllRequisites = true;
        this.hasItem = null;
        this.hasCraft = null;
        this.hasResearch = null;
        this.hasKnow = null;
        ResearchStage stage = this.research.getStages()[this.currentStage];
        ItemStack[] o = stage.getObtain();
        if (o != null) {
            this.hasItem = new boolean[o.length];
            for (int a = 0; a < o.length; a++) {
                this.hasItem[a] = InventoryUtils.isPlayerCarryingAmount(this.minecraft.player, o[a], false);
                if (!this.hasItem[a]) {
                    this.hasAllRequisites = false;
                }
            }
        }
        ItemStack[] c = stage.getCraft();
        if (c != null) {
            this.hasCraft = new boolean[c.length];
            for (int a = 0; a < c.length; a++) {
                this.hasCraft[a] = this.playerKnowledge.isResearchKnown("[#]" + stage.getCraftReference()[a]);
                if (!this.hasCraft[a]) {
                    this.hasAllRequisites = false;
                }
            }
        }
        String[] r = stage.getResearch();
        if (r != null) {
            this.hasResearch = new boolean[r.length];
            for (int a = 0; a < r.length; a++) {
                this.hasResearch[a] = ThaumcraftCapabilities.knowsResearchStrict(this.minecraft.player, r[a]);
                if (!this.hasResearch[a]) {
                    this.hasAllRequisites = false;
                }
            }
        }
        ResearchStage.Knowledge[] k = stage.getKnow();
        if (k != null) {
            this.hasKnow = new boolean[k.length];
            for (int a = 0; a < k.length; a++) {
                this.hasKnow[a] = this.playerKnowledge.getKnowledge(k[a].type, k[a].category) >= k[a].amount;
                if (!this.hasKnow[a]) {
                    this.hasAllRequisites = false;
                }
            }
        }
    }

    /** Номер страницы рецепта с результатом stack в рецепте/группе rk; -1 — нет; -99 — рецепт ещё неизвестен (этапы 8+). */
    private int findRecipePage(ResourceLocation rk, ItemStack stack, int start) {
        Object recipe = ResearchRecipes.resolve(rk);
        if (recipe == null) {
            return -1;
        }
        if (recipe instanceof List<?> group) {
            int gi = 0;
            for (Object rl : group) {
                int q = this.findRecipePage((ResourceLocation) rl, stack, gi);
                if (q >= 0) {
                    return q;
                }
                gi++;
            }
        }
        // TODO этапы 9, 11: CrucibleRecipe, InfusionRecipe, RecipeMisc
        Recipe<?> r = recipe instanceof RecipeHolder<?> h ? h.value() : recipe instanceof Recipe<?> rr ? rr : null;
        if (r != null && ItemStack.isSameItem(r.getResultItem(this.minecraft.level.registryAccess()), stack)) {
            return r instanceof IArcaneRecipe ar && !ThaumcraftCapabilities.knowsResearchStrict(this.minecraft.player, ar.getResearch()) ? -99 : start;
        }
        return -1;
    }

    /** "рецепт;страница", "UNKNOWN" или null — первый рецепт исследований с результатом stack (кешируется). */
    private String getCraftingRecipeKey(ItemStack stack) {
        int key = ItemStack.hashItemAndComponents(stack);
        if (this.keyCache.containsKey(key)) {
            return this.keyCache.get(key);
        }
        for (ResearchCategory rcl : ResearchCategories.researchCategories.values()) {
            for (ResearchEntry ri : rcl.research.values()) {
                if (ri.getStages() == null) {
                    continue;
                }
                for (ResearchStage stage : ri.getStages()) {
                    if (stage.getRecipes() == null) {
                        continue;
                    }
                    for (ResourceLocation rec : stage.getRecipes()) {
                        int result = this.findRecipePage(rec, stack, 0);
                        if (result != -1) {
                            String s = result == -99 ? "UNKNOWN" : rec + ";" + result;
                            this.keyCache.put(key, s);
                            return s;
                        }
                    }
                }
            }
        }
        this.keyCache.put(key, null);
        return null;
    }

    private static class Page {
        final List<Object> contents = new ArrayList<>();

        Page copy() {
            Page p = new Page();
            p.contents.addAll(this.contents);
            return p;
        }
    }

    /** Картинка в тексте: "домен:путь:x:y:ширина:высота:масштаб" (UV в единицах текстуры 256×256). */
    private static class PageImage {
        int x;
        int y;
        int w;
        int h;
        int aw;
        int ah;
        float scale;
        ResourceLocation loc;

        static PageImage parse(String text) {
            String[] s = text.split(":");
            if (s.length != 7) {
                return null;
            }
            try {
                PageImage pi = new PageImage();
                pi.loc = ResourceLocation.fromNamespaceAndPath(s[0], s[1]);
                pi.x = Integer.parseInt(s[2]);
                pi.y = Integer.parseInt(s[3]);
                pi.w = Integer.parseInt(s[4]);
                pi.h = Integer.parseInt(s[5]);
                pi.scale = Float.parseFloat(s[6]);
                pi.aw = (int) (pi.w * pi.scale);
                pi.ah = (int) (pi.h * pi.scale);
                return pi.ah <= 208 && pi.aw <= 140 ? pi : null;
            } catch (Exception e) {
                return null;
            }
        }
    }

}
