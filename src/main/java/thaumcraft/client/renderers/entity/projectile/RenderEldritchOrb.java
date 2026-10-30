package thaumcraft.client.renderers.entity.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.entities.projectile.EntityEldritchOrb;

import java.util.Random;

/**
 * Рендер потусторонней сферы (порт RenderEldritchOrb): 12 белых лучей-пирамид со случайными поворотами (семя 187),
 * аддитивно, вырастают за первые 10 тиков и вращаются; поверх — повёрнутый к камере кадр тёмной дымки (строка 3 частиц).
 */
public class RenderEldritchOrb extends EntityRenderer<EntityEldritchOrb> {
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");
    private final Random random = new Random();

    public RenderEldritchOrb(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityEldritchOrb entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        this.random.setSeed(187L);
        float f1 = entity.tickCount / 80.0F;
        float grow = Math.min(entity.tickCount, 10) / 10.0F;
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.HALO);
        pose.pushPose();
        for (int i = 0; i < 12; i++) {
            pose.mulPose(Axis.XP.rotationDegrees(this.random.nextFloat() * 360.0F));
            pose.mulPose(Axis.YP.rotationDegrees(this.random.nextFloat() * 360.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(this.random.nextFloat() * 360.0F));
            pose.mulPose(Axis.XP.rotationDegrees(this.random.nextFloat() * 360.0F));
            pose.mulPose(Axis.YP.rotationDegrees(this.random.nextFloat() * 360.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(this.random.nextFloat() * 360.0F + f1 * 360.0F));
            float fa = (this.random.nextFloat() * 20.0F + 5.0F) / (30.0F / grow);
            float f4 = (this.random.nextFloat() * 2.0F + 1.0F) / (30.0F / grow);
            Matrix4f m = pose.last().pose();
            // веер GL_TRIANGLE_FAN (центр + 4 точки) — три треугольника, каждый вырожденным четырёхугольником;
            // цвет краёв в оригинале 64.0F (зажимается до 1) — белый, непрозрачный
            float[][] pts = {{-0.866F * f4, fa, -0.5F * f4}, {0.866F * f4, fa, -0.5F * f4}, {0.0F, fa, 1.0F * f4}, {-0.866F * f4, fa, -0.5F * f4}};
            for (int k = 0; k < 3; k++) {
                vc.addVertex(m, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, 255);
                vc.addVertex(m, pts[k][0], pts[k][1], pts[k][2]).setColor(255, 255, 255, 255);
                vc.addVertex(m, pts[k + 1][0], pts[k + 1][1], pts[k + 1][2]).setColor(255, 255, 255, 255);
                vc.addVertex(m, pts[k + 1][0], pts[k + 1][1], pts[k + 1][2]).setColor(255, 255, 255, 255);
            }
        }
        pose.popPose();

        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        float u0 = entity.tickCount % 13 / 64.0F;
        float u1 = u0 + 0.015625F;
        float v0 = 0.046875F;
        float v1 = v0 + 0.015625F;
        VertexConsumer q = buffers.getBuffer(RenderType.entityTranslucent(PARTICLES));
        PoseStack.Pose p = pose.last();
        v(q, p, -0.5F, -0.5F, u0, v1, light);
        v(q, p, 0.5F, -0.5F, u1, v1, light);
        v(q, p, 0.5F, 0.5F, u1, v0, light);
        v(q, p, -0.5F, 0.5F, u0, v0, light);
        pose.popPose();
        super.render(entity, yaw, pt, pose, buffers, light);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float u, float v, int light) {
        vc.addVertex(p, x, y, 0.0F).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEldritchOrb entity) {
        return PARTICLES;
    }
}
