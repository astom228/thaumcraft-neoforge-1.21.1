package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.entities.monster.EntityWisp;

/**
 * Рендер огонька (порт RenderWisp): три повёрнутых к камере квадрата, аддитивно, без записи глубины, яркость 220 —
 * вспышка (кадры 512–527 частиц), ореол (320–335, прозрачность 0.25) и ядро цвета аспекта (кадры 800–815 auranodes).
 */
public class RenderWisp extends EntityRenderer<EntityWisp> {
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");
    private static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/auranodes.png");
    private static final Vector3f SCRATCH = new Vector3f();

    public RenderWisp(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityWisp entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        if (entity.getHealth() <= 0.0F) {
            return;
        }
        Aspect a = Aspect.getAspect(entity.getWispType());
        int color = a != null ? a.getColor() : 0;
        Quaternionf q = this.entityRenderDispatcher.cameraOrientation();
        int t = entity.tickCount % 16;
        quad(buffers.getBuffer(TCRenderTypes.glowNoDepth(PARTICLES)), pose, q, 64, 64, 512 + t, 0.4F, 0xFFFFFF, 1.0F);
        quad(buffers.getBuffer(TCRenderTypes.glowNoDepth(PARTICLES)), pose, q, 64, 64, 320 + t, 0.75F, 0xFFFFFF, 0.25F);
        quad(buffers.getBuffer(TCRenderTypes.glowNoDepth(NODES)), pose, q, 32, 32, 800 + t, 0.75F, color, 0.5F);
    }

    /** renderFacingQuad оригинала: кадр frame сетки gridX×gridY, вершины (−,−) f2f4, (−,+) f2f3, (+,+) f1f3, (+,−) f1f4. */
    private static void quad(VertexConsumer vc, PoseStack pose, Quaternionf q, int gridX, int gridY, int frame, float scale, int color, float alpha) {
        int xm = frame % gridX;
        int ym = frame / gridY;
        float f1 = (float) xm / gridX;
        float f2 = f1 + 1.0F / gridX;
        float f3 = (float) ym / gridY;
        float f4 = f3 + 1.0F / gridY;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        var m = pose.last().pose();
        v(vc, m, q, -scale, -scale, f2, f4, r, g, b, alpha);
        v(vc, m, q, -scale, scale, f2, f3, r, g, b, alpha);
        v(vc, m, q, scale, scale, f1, f3, r, g, b, alpha);
        v(vc, m, q, scale, -scale, f1, f4, r, g, b, alpha);
    }

    private static void v(VertexConsumer vc, org.joml.Matrix4f m, Quaternionf q, float x, float y, float u, float v, float r, float g, float b, float a) {
        Vector3f p = SCRATCH.set(x, y, 0.0F).rotate(q);
        vc.addVertex(m, p.x(), p.y(), p.z()).setColor(r, g, b, a).setUv(u, v).setLight(220);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityWisp entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
