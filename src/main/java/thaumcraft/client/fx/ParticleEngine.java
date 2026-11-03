package thaumcraft.client.fx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fStack;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.particles.ICustomRenderParticle;
import thaumcraft.client.fx.particles.IEngineParticle;

/**
 * Движок частиц Thaumcraft (порт ParticleEngine). Свои списки частиц по слоям и измерениям, тик на клиентском тике,
 * отрисовка после ванильных частиц с явным состоянием (как onRenderWorldLast оригинала):
 * слой 0 — аддитивное смешивание, 1 — обычное, 2 — аддитивное без глубины, 3 — обычное без глубины.
 * Слои 4–5 — частицы поверх интерфейса (FXGenericGui), рисуются после кадра (renderGuiParticles).
 */
@EventBusSubscriber(modid = Thaumcraft.MODID, value = Dist.CLIENT)
public class ParticleEngine {
    public static final ResourceLocation particleTexture = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");
    /** Шейдер частиц с порогом прозрачности 1/255 (как glAlphaFunc оригинала); регистрируется в ThaumcraftClient. */
    public static ShaderInstance particleShader;

    @SuppressWarnings("unchecked")
    private static final Map<ResourceKey<Level>, List<Particle>>[] particles = new Map[]{
            new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>()};
    private static final List<ParticleDelay> particlesDelayed = new ArrayList<>();

    private record ParticleDelay(Particle particle, ResourceKey<Level> dim, int[] delay) {
    }

    private static int layerOf(Particle p) {
        return p instanceof IEngineParticle g ? g.getFXLayer() : 0;
    }

    public static void addEffect(Level world, Particle fx) {
        addEffect(world.dimension(), fx);
    }

    /** Как в оригинале: учитываются настройка частиц и FPS < 30, лимит 2000/1000/500 на слой (самые старые удаляются). */
    public static void addEffect(ResourceKey<Level> dim, Particle fx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        int ps = mc.options.particles().get().getId();
        if (mc.getFps() < 30) {
            ps++;
        }
        if (mc.level.random.nextInt(3) >= ps) {
            List<Particle> parts = particles[layerOf(fx)].computeIfAbsent(dim, k -> new ArrayList<>());
            if (parts.size() >= getParticleLimit()) {
                parts.remove(0);
            }
            parts.add(fx);
        }
    }

    public static void addEffectWithDelay(Level world, Particle fx, int delay) {
        particlesDelayed.add(new ParticleDelay(fx, world.dimension(), new int[]{delay}));
    }

    private static int getParticleLimit() {
        ParticleStatus status = Minecraft.getInstance().options.particles().get();
        return status == ParticleStatus.MINIMAL ? 500 : status == ParticleStatus.DECREASED ? 1000 : 2000;
    }

    /** Раздел 7.2: при выходе из мира — очистить частицы и отложенные эффекты (в оригинале не очищались). */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        thaumcraft.api.internal.CommonInternals.clearSide(net.neoforged.fml.LogicalSide.CLIENT);
        thaumcraft.client.lib.events.EssentiaSourceFXClient.clear();
        thaumcraft.client.lib.events.WarpFog.clear();
        thaumcraft.client.lib.ClientSeals.clear();
        for (Map<ResourceKey<Level>, List<Particle>> layer : particles) {
            layer.clear();
        }
        particlesDelayed.clear();
        thaumcraft.client.lib.events.HudHandler.clear();
        thaumcraft.common.items.casters.CasterManager.clearClient();
        thaumcraft.client.lib.events.WandRenderingHandler.clear();
    }

    @SubscribeEvent
    public static void updateParticles(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        ResourceKey<Level> dim = mc.level.dimension();
        Iterator<ParticleDelay> i = particlesDelayed.iterator();
        while (i.hasNext()) {
            ParticleDelay pd = i.next();
            pd.delay()[0]--;
            if (pd.delay()[0] <= 0) {
                if (pd.dim() == dim) {
                    addEffect(pd.dim(), pd.particle());
                }
                i.remove();
            }
        }
        for (int layer = 0; layer < 6; layer++) {
            List<Particle> parts = particles[layer].get(dim);
            if (parts == null) {
                continue;
            }
            for (int j = 0; j < parts.size(); j++) {
                Particle p = parts.get(j);
                try {
                    if (p != null) {
                        p.tick();
                    }
                } catch (Exception e) {
                    if (p != null) {
                        p.remove();
                    }
                }
                if (p == null || !p.isAlive()) {
                    parts.remove(j--);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || particleShader == null) {
            return;
        }
        ResourceKey<Level> dim = mc.level.dimension();
        Camera camera = event.getCamera();
        float frame = event.getPartialTick().getGameTimeDeltaPartialTick(false);

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        mc.gameRenderer.lightTexture().turnOnLightLayer();
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(() -> particleShader);
        RenderSystem.setShaderTexture(0, particleTexture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        for (int layer = 3; layer >= 0; layer--) {
            List<Particle> parts = particles[layer].get(dim);
            if (parts == null || parts.isEmpty()) {
                continue;
            }
            switch (layer) {
                case 0, 2 -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                default -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            if (layer >= 2) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
            for (Particle p : parts) {
                try {
                    if (p instanceof IEngineParticle g) {
                        g.renderParticle(buffer, camera, frame);
                    }
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Particle");
                    crashreport.addCategory("Particle being rendered").setDetail("Particle", p::toString);
                    throw new ReportedException(crashreport);
                }
            }
            MeshData mesh = buffer.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
            // частицы со своей отрисовкой (в 1.12 — сброс буфера посреди пачки); затем вернуть шейдер и текстуру частиц
            boolean custom = false;
            for (Particle p : parts) {
                if (p instanceof ICustomRenderParticle c && p.isAlive()) {
                    c.renderCustom(new PoseStack(), camera, frame);
                    custom = true;
                }
            }
            if (custom) {
                RenderSystem.setShader(() -> particleShader);
                RenderSystem.setShaderTexture(0, particleTexture);
            }
        }

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        mc.gameRenderer.lightTexture().turnOffLightLayer();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    /**
     * Слои 4–5 — частицы поверх интерфейса (renderTick оригинала): 4 — аддитивные, 5 — обычные.
     * Вызывается из RenderEventHandler после отрисовки кадра, в ортопроекции UtilsFX.beginFrameOverlay.
     */
    public static void renderGuiParticles(float frame) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        ResourceKey<Level> dim = mc.level.dimension();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, particleTexture);
        for (int layer = 5; layer >= 4; layer--) {
            List<Particle> parts = particles[layer].get(dim);
            if (parts == null || parts.isEmpty()) {
                continue;
            }
            if (layer == 4) {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            } else {
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (Particle p : parts) {
                try {
                    if (p instanceof IEngineParticle g) {
                        g.renderParticle(buffer, null, frame);
                    }
                } catch (Throwable throwable) {
                    CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering Particle");
                    crashreport.addCategory("Particle being rendered").setDetail("Particle", p::toString);
                    throw new ReportedException(crashreport);
                }
            }
            MeshData mesh = buffer.build();
            if (mesh != null) {
                BufferUploader.drawWithShader(mesh);
            }
        }
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }
}
