package thaumcraft.client.lib;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;

/** Собственные типы рендера Thaumcraft (доступ к состояниям RenderStateShard — через наследование). */
public final class TCRenderTypes extends RenderType {
    private static final ResourceLocation TUNNEL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/tunnel.png");
    private static final ResourceLocation PARTICLEFIELD = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particlefield.png");

    /**
     * Плоскость связанного зеркала (drawPlane* TileMirrorRenderer): в 1.12 — 16 слоёв с glTexGen, как у портала Края того же
     * времени. В 1.21 этот эффект воспроизводит штатный шейдер end_portal; здесь он с текстурами оригинала
     * (tunnel — первый слой, particlefield — остальные), без отсечения граней.
     */
    public static final RenderType MIRROR_PORTAL = create("thaumcraft_mirror_portal", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 1536,
            false, false, CompositeState.builder().setShaderState(RENDERTYPE_END_PORTAL_SHADER)
                    .setTextureState(MultiTextureStateShard.builder().add(TUNNEL, false, false).add(PARTICLEFIELD, false, false).build())
                    .setCullState(NO_CULL).createCompositeState(false));

    private static final java.util.function.Function<ResourceLocation, RenderType> ADDITIVE_ENTITY = net.minecraft.Util.memoize(tex -> create(
            "thaumcraft_additive_entity", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true,
            CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(true)));

    /** Модель с текстурой, аддитивно (glBlendFunc(SRC_ALPHA, ONE)) — свечение кубиков рунической матрицы. */
    public static RenderType additiveEntity(ResourceLocation tex) {
        return ADDITIVE_ENTITY.apply(tex);
    }

    /** Лучи ореола матрицы (drawHalo): цвет без текстуры, аддитивно, без записи глубины. */
    public static final RenderType HALO = create("thaumcraft_halo", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
            CompositeState.builder().setShaderState(POSITION_COLOR_SHADER).setTransparencyState(LIGHTNING_TRANSPARENCY).setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));

    private static final java.util.function.Function<ResourceLocation, RenderType> ARCHITECT = net.minecraft.Util.memoize(tex -> create(
            "thaumcraft_architect", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
            CompositeState.builder().setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader)).setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)));

    /** Рамки и стрелки «Плана»: текстура, аддитивно (SRC_ALPHA, ONE), сквозь блоки, обе стороны, без записи глубины. */
    public static RenderType architect(ResourceLocation tex) {
        return ARCHITECT.apply(tex);
    }

    private static final java.util.function.Function<ResourceLocation, RenderType> TUBE = net.minecraft.Util.memoize(tex -> create(
            "thaumcraft_tube", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1024, false, true,
            CompositeState.builder().setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionTexColorShader)).setTextureState(new TextureStateShard(tex, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).createCompositeState(false)));

    /** Трубки PolyCone (верёвка крюкомёта и т.п.): текстура, обычное смешивание, обе стороны. */
    public static RenderType tube(ResourceLocation tex) {
        return TUBE.apply(tex);
    }

    private static final java.util.function.Function<ResourceLocation, RenderType> GLOW = net.minecraft.Util.memoize(tex -> create(
            "thaumcraft_glow", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 1024, false, true,
            CompositeState.builder().setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionColorTexLightmapShader))
                    .setTextureState(new TextureStateShard(tex, false, false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setLightmapState(LIGHTMAP)
                    .setCullState(NO_CULL).createCompositeState(false)));

    private static final java.util.function.Function<ResourceLocation, RenderType> GLOW_NO_DEPTH = net.minecraft.Util.memoize(tex -> create(
            "thaumcraft_glow_no_depth", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true,
            CompositeState.builder().setShaderState(new ShaderStateShard(net.minecraft.client.renderer.GameRenderer::getPositionColorTexLightmapShader))
                    .setTextureState(new TextureStateShard(tex, false, false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setLightmapState(LIGHTMAP)
                    .setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false)));

    /** Как glow, но без записи глубины (renderFacingQuad оригинала: depthMask(false)) — огонёк. */
    public static RenderType glowNoDepth(ResourceLocation tex) {
        return GLOW_NO_DEPTH.apply(tex);
    }

    /** Текстура × цвет вершины × карта освещения, аддитивно (SRC_ALPHA, ONE), обе стороны: сетка и стенки диоптры. */
    public static RenderType glow(ResourceLocation tex) {
        return GLOW.apply(tex);
    }

    private static final ResourceLocation END_SKY = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    private static final ResourceLocation END_PORTAL = ResourceLocation.withDefaultNamespace("textures/entity/end_portal.png");

    private static RenderType rift(String name, TransparencyStateShard transparency, boolean depthWrite, boolean depthTest) {
        CompositeState.CompositeStateBuilder b = CompositeState.builder().setShaderState(RENDERTYPE_END_PORTAL_SHADER)
                .setTextureState(MultiTextureStateShard.builder().add(END_SKY, false, false).add(END_PORTAL, false, false).build())
                .setTransparencyState(transparency).setCullState(NO_CULL).setWriteMaskState(depthWrite ? COLOR_DEPTH_WRITE : COLOR_WRITE);
        if (!depthTest) {
            b.setDepthTestState(NO_DEPTH_TEST);
        }
        return create(name, DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 4096, false, false, b.createCompositeState(false));
    }

    /**
     * Разлом Выброса (RenderFluxRift + ShaderHelper.endShader): в 1.12 — свой шейдер звёздного неба Края; здесь — штатный шейдер
     * end_portal. Три светящихся слоя — аддитивно, без записи глубины (первый с очками — сквозь стены), сердцевина — обычно.
     */
    public static final RenderType RIFT_GLOW = rift("thaumcraft_rift_glow", LIGHTNING_TRANSPARENCY, false, true);
    public static final RenderType RIFT_GLOW_XRAY = rift("thaumcraft_rift_glow_xray", LIGHTNING_TRANSPARENCY, false, false);
    public static final RenderType RIFT_CORE = rift("thaumcraft_rift_core", TRANSLUCENT_TRANSPARENCY, true, true);

    private TCRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                          Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
