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

    private TCRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload,
                          Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
}
