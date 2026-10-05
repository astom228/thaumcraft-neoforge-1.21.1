package thaumcraft.client.lib.events;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.items.tools.ItemThaumometer;
import thaumcraft.Thaumcraft;
import thaumcraft.api.capabilities.IPlayerKnowledge;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.client.fx.FXDispatcher;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.world.aura.AuraChunk;

/**
 * Порт HudHandler: эффект получения знаний (пока открыт любой экран тоже), HUD предметов — с предметами
 * (таумометр — 7.3, кастер — этап 12, проверка рассудка — с ней). Только клиент.
 */
public class HudHandler {
    static final ResourceLocation HUD = rl("textures/gui/hud.png");
    private static final DecimalFormat SECONDS_FORMATTER = new DecimalFormat("#######.#");
    public static final ResourceLocation BOOK = rl("textures/items/thaumonomicon.png");
    public static final ResourceLocation[] KNOW_TYPE = new ResourceLocation[] {
            rl("textures/research/knowledge_theory.png"), rl("textures/research/knowledge_observation.png")};
    /** Последняя полученная аура (PacketAuraToClient). */
    public static AuraChunk currentAura = new AuraChunk(null, (short) 0, 0.0F, 0.0F);
    public static final LinkedBlockingQueue<KnowledgeGainTracker> knowledgeGainTrackers = new LinkedBlockingQueue<>();
    static float kgFade = 0.0F;

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, path);
    }

    /** renderHuds оригинала (вызывается после кадра в ортопроекции UtilsFX.beginFrameOverlay). */
    static void renderHuds(Minecraft mc, float renderTickTime, Player player, int ww, int hh) {
        PoseStack pose = new PoseStack();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (kgFade > 0.0F) {
            renderKnowledgeGains(mc, pose, renderTickTime, player, ww, hh);
        }
        if (mc.mouseHandler.isMouseGrabbed() && !mc.options.hideGui) {
            RenderSystem.setShaderTexture(0, HUD);
            ItemStack handStack = player.getMainHandItem();
            boolean rT = false;
            for (int a = 0; a < 2; a++) {
                if (!handStack.isEmpty()) {
                    // TODO этап 12: кастер (ICaster); проверка рассудка — с предметом
                    if (!rT && handStack.getItem() instanceof ItemThaumometer) {
                        renderThaumometerHud(mc, pose, renderTickTime, player);
                        rT = true;
                    }
                }
                handStack = player.getOffhandItem();
            }
        }
        RenderSystem.disableBlend();
    }

    /** Шкалы Вис и Выброса в чанке (aura 525 = полная шкала), база — метка; с Shift — числа. */
    static void renderThaumometerHud(Minecraft mc, PoseStack pose, float partialTicks, Player player) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        float base = Mth.clamp(currentAura.getBase() / 525.0F, 0.0F, 1.0F);
        float vis = Mth.clamp(currentAura.getVis() / 525.0F, 0.0F, 1.0F);
        float flux = Mth.clamp(currentAura.getFlux() / 525.0F, 0.0F, 1.0F);
        float count = mc.getCameraEntity().tickCount + partialTicks;
        float count2 = mc.getCameraEntity().tickCount / 3.0F + partialTicks;
        if (flux + vis > 1.0F) {
            float m = 1.0F / (flux + vis);
            base *= m;
            vis *= m;
            flux *= m;
        }
        float start = 10.0F + (1.0F - vis) * 64.0F;
        if (vis > 0.0F) {
            pose.pushPose();
            RenderSystem.setShaderColor(0.7F, 0.4F, 0.9F, 1.0F);
            pose.translate(5.0, start, 0.0);
            pose.scale(1.0F, vis, 1.0F);
            UtilsFX.drawTexturedQuad(pose, 0.0F, 0.0F, 88.0F, 56.0F, 8.0F, 64.0F, -90.0F);
            pose.popPose();
            pose.pushPose();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
            pose.translate(5.0, start, 0.0);
            UtilsFX.drawTexturedQuad(pose, 0.0F, 0.0F, 96.0F, 56.0F + count % 64.0F, 8.0F, vis * 64.0F, -90.0F);
            RenderSystem.defaultBlendFunc();
            pose.popPose();
            if (player.isShiftKeyDown()) {
                pose.pushPose();
                pose.translate(16.0, start, 0.0);
                pose.scale(0.5F, 0.5F, 0.5F);
                drawStringWithShadow(mc, pose, SECONDS_FORMATTER.format(currentAura.getVis()), 15641343);
                pose.popPose();
                RenderSystem.setShaderTexture(0, HUD);
            }
        }
        if (flux > 0.0F) {
            start = 10.0F + (1.0F - flux - vis) * 64.0F;
            pose.pushPose();
            RenderSystem.setShaderColor(0.25F, 0.1F, 0.3F, 1.0F);
            pose.translate(5.0, start, 0.0);
            pose.scale(1.0F, flux, 1.0F);
            UtilsFX.drawTexturedQuad(pose, 0.0F, 0.0F, 88.0F, 56.0F, 8.0F, 64.0F, -90.0F);
            pose.popPose();
            pose.pushPose();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(0.7F, 0.4F, 1.0F, 0.5F);
            pose.translate(5.0, start, 0.0);
            UtilsFX.drawTexturedQuad(pose, 0.0F, 0.0F, 104.0F, 120.0F - count2 % 64.0F, 8.0F, flux * 64.0F, -90.0F);
            RenderSystem.defaultBlendFunc();
            pose.popPose();
            if (player.isShiftKeyDown()) {
                pose.pushPose();
                pose.translate(16.0, start - 4.0F, 0.0);
                pose.scale(0.5F, 0.5F, 0.5F);
                drawStringWithShadow(mc, pose, SECONDS_FORMATTER.format(currentAura.getFlux()), 11145659);
                pose.popPose();
                RenderSystem.setShaderTexture(0, HUD);
            }
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        UtilsFX.drawTexturedQuad(pose, 1.0F, 1.0F, 72.0F, 48.0F, 16.0F, 80.0F, -90.0F);
        start = 8.0F + (1.0F - base) * 64.0F;
        UtilsFX.drawTexturedQuad(pose, 2.0F, start, 117.0F, 61.0F, 14.0F, 5.0F, -90.0F);
    }

    /** Gui.drawString 1.12 (с тенью) в текущей матрице. */
    private static void drawStringWithShadow(Minecraft mc, PoseStack pose, String s, int color) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        mc.font.drawInBatch(s, 0, 0, color, true, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        buffers.endBatch();
    }

    /** MathHelper.getInt(Random, min, max) 1.12: при min >= max случайное число не берётся. */
    private static int getInt(Random rand, int min, int max) {
        return min >= max ? min : rand.nextInt(max - min + 1) + min;
    }

    static void renderKnowledgeGains(Minecraft mc, PoseStack pose, float renderTickTime, Player player, int ww, int hh) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, kgFade / 40.0F);
        RenderSystem.setShaderTexture(0, BOOK);
        UtilsFX.drawTexturedQuadFull(pose, ww - 17, hh - 17, -90.0F);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        LinkedBlockingQueue<KnowledgeGainTracker> temp = new LinkedBlockingQueue<>();
        for (int a = 0; !knowledgeGainTrackers.isEmpty(); a++) {
            KnowledgeGainTracker current = knowledgeGainTrackers.poll();
            if (current == null) {
                continue;
            }
            RenderSystem.setShaderTexture(0, KNOW_TYPE[current.type.ordinal()]);
            Random rand = new Random(current.seed);
            pose.pushPose();
            float s = 16.0F;
            float x = ww / 4 + rand.nextInt(32);
            float y = hh / 3 + rand.nextInt(32);
            float wot = 0.0F;
            if (current.progress < current.max * 0.66F) {
                float q = (current.progress - renderTickTime) / (current.max * 0.66F);
                s *= q;
                float m = (float) Math.sin(q * Math.PI - Math.PI / 2) * 0.5F + 0.5F;
                y *= m;
                float d = (float) Math.sin(m * Math.PI * 0.5);
                x *= d;
            } else {
                wot = current.max - current.progress + renderTickTime;
                float wot2 = wot / (current.max * 0.33F);
                float m = (float) Math.sin(wot2 * Math.PI * 2.0 - Math.PI / 2) * 0.5F + 1.5F;
                if (wot2 < 0.5) {
                    s *= wot2 * 2.0F;
                }
                s *= m;
            }
            float xx = ww - 12 + rand.nextInt(8) - x;
            float yy = hh - 12 + rand.nextInt(8) - y;
            if (current.sparks && player.getRandom().nextInt((int) (1.0F + (float) current.progress / current.max * 10.0F)) == 0) {
                float r = getIntRs(player, 255, 255) / 255.0F;
                float g = getIntRs(player, 189, 255) / 255.0F;
                float b = getIntRs(player, 64, 255) / 255.0F;
                FXDispatcher.INSTANCE.drawSimpleSparkleGui(player.level().random,
                        xx + player.level().random.nextGaussian() * 5.0, yy + player.level().random.nextGaussian() * 5.0,
                        player.level().random.nextGaussian(), player.level().random.nextGaussian(), 24.0F, r, g, b,
                        player.level().random.nextInt(5), 0.9F, -1.0F);
            }
            pose.translate(xx, yy, -80 + a);
            pose.mulPose(Axis.ZN.rotationDegrees(84 + rand.nextInt(12)));
            UtilsFX.renderQuadCentered(pose, 1, 1, 0, s, 1.0F, 1.0F, 1.0F, 200, 771, 1.0F);
            if (current.category != null) {
                RenderSystem.setShaderTexture(0, current.category.icon);
                pose.translate(0.0F, 0.0F, 1.0F);
                UtilsFX.renderQuadCentered(pose, 1, 1, 0, s * 0.75F, 1.0F, 1.0F, 1.0F, 200, 771, 1.0F);
            }
            if (current.progress > current.max * 0.9F) {
                float wot3 = wot / (current.max * 0.1F);
                float m2 = (float) Math.sin(wot3 * Math.PI * 2.0 - Math.PI / 2) * 0.25F + 0.25F;
                float size = 64.0F * m2;
                pose.mulPose(Axis.ZN.rotationDegrees(rand.nextInt(360)));
                RenderSystem.setShaderTexture(0, ParticleEngine.particleTexture);
                float r = getInt(rand, 255, 255) / 255.0F;
                float g = getInt(rand, 189, 255) / 255.0F;
                float b = getInt(rand, 64, 255) / 255.0F;
                UtilsFX.renderQuadCentered(pose, 64, 64, 320 + rand.nextInt(16), size, r, g, b, 200, 1, 1.0F);
            }
            if (current.progress < current.max * 0.1F) {
                float wot3 = 1.0F - (current.progress - renderTickTime) / (current.max * 0.1F);
                float m2 = (float) Math.sin(wot3 * Math.PI * 2.0 - Math.PI / 2) * 0.25F + 0.25F;
                float size = 32.0F * m2;
                pose.mulPose(Axis.ZN.rotationDegrees(rand.nextInt(360)));
                RenderSystem.setShaderTexture(0, ParticleEngine.particleTexture);
                float r = getInt(rand, 255, 255) / 255.0F;
                float g = getInt(rand, 189, 255) / 255.0F;
                float b = getInt(rand, 64, 255) / 255.0F;
                UtilsFX.renderQuadCentered(pose, 64, 64, 320 + rand.nextInt(16), size, r, g, b, 200, 1, 1.0F);
            }
            temp.offer(current);
            pose.popPose();
        }
        while (!temp.isEmpty()) {
            knowledgeGainTrackers.offer(temp.poll());
        }
    }

    private static int getIntRs(Player player, int min, int max) {
        return min >= max ? min : player.level().random.nextInt(max - min + 1) + min;
    }

    /** Ход эффектов получения знаний (тик клиента, как в RenderEventHandler оригинала). */
    static void tickKnowledgeGains() {
        if (knowledgeGainTrackers.isEmpty()) {
            if (kgFade > 0.0F) {
                kgFade--;
            }
            return;
        }
        kgFade += 10.0F;
        if (kgFade > 40.0F) {
            kgFade = 40.0F;
        }
        LinkedBlockingQueue<KnowledgeGainTracker> temp = new LinkedBlockingQueue<>();
        while (!knowledgeGainTrackers.isEmpty()) {
            KnowledgeGainTracker current = knowledgeGainTrackers.poll();
            if (current != null && current.progress > 0) {
                current.progress--;
                temp.offer(current);
            }
        }
        while (!temp.isEmpty()) {
            knowledgeGainTrackers.offer(temp.poll());
        }
    }

    /** При выходе из мира. */
    public static void clear() {
        currentAura = new AuraChunk(null, (short) 0, 0.0F, 0.0F);
        knowledgeGainTrackers.clear();
        kgFade = 0.0F;
    }

    public static class KnowledgeGainTracker {
        IPlayerKnowledge.EnumKnowledgeType type;
        ResearchCategory category;
        int progress;
        int max;
        long seed;
        boolean sparks = false;

        public KnowledgeGainTracker(IPlayerKnowledge.EnumKnowledgeType type, ResearchCategory category, int progress, long seed) {
            this.type = type;
            this.category = category;
            if (type == IPlayerKnowledge.EnumKnowledgeType.THEORY) {
                progress += 10;
            }
            this.progress = progress;
            this.max = progress;
            this.seed = seed;
        }
    }
}
