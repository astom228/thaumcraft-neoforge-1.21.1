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
import net.minecraft.util.Mth;
import thaumcraft.common.entities.projectile.EntityGolemDart;

/**
 * Дротик голема (порт RenderDart): ванильная стрела, уменьшенная до 0.033 (у стрелы 0.05625) и сдвинутая на 4 единицы
 * назад — как в оригинале.
 */
public class RenderDart extends EntityRenderer<EntityGolemDart> {
    private static final ResourceLocation ARROW = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public RenderDart(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityGolemDart entity) {
        return ARROW;
    }

    @Override
    public void render(EntityGolemDart arrow, float entityYaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, arrow.yRotO, arrow.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, arrow.xRotO, arrow.getXRot())));
        float shake = arrow.shakeTime - pt;
        if (shake > 0.0F) {
            pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake * 3.0F) * shake));
        }
        pose.mulPose(Axis.XP.rotationDegrees(45.0F));
        pose.scale(0.033F, 0.033F, 0.033F);
        pose.translate(-4.0F, 0.0F, 0.0F);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(ARROW));
        PoseStack.Pose last = pose.last();
        float f6 = 0.0F, f7 = 0.15625F, f8 = 5.0F / 32.0F, f9 = 10.0F / 32.0F;
        float f2 = 0.0F, f3 = 0.5F, f4 = 0.0F, f5 = 5.0F / 32.0F;
        vertex(last, vc, -7, -2, -2, f6, f8, -1, 0, 0, light);
        vertex(last, vc, -7, -2, 2, f7, f8, -1, 0, 0, light);
        vertex(last, vc, -7, 2, 2, f7, f9, -1, 0, 0, light);
        vertex(last, vc, -7, 2, -2, f6, f9, -1, 0, 0, light);
        vertex(last, vc, -7, 2, -2, f6, f8, 1, 0, 0, light);
        vertex(last, vc, -7, 2, 2, f7, f8, 1, 0, 0, light);
        vertex(last, vc, -7, -2, 2, f7, f9, 1, 0, 0, light);
        vertex(last, vc, -7, -2, -2, f6, f9, 1, 0, 0, light);
        for (int i = 0; i < 4; i++) {
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            last = pose.last();
            vertex(last, vc, -8, -2, 0, f2, f4, 0, 0, 1, light);
            vertex(last, vc, 8, -2, 0, f3, f4, 0, 0, 1, light);
            vertex(last, vc, 8, 2, 0, f3, f5, 0, 0, 1, light);
            vertex(last, vc, -8, 2, 0, f2, f5, 0, 0, 1, light);
        }
        pose.popPose();
        super.render(arrow, entityYaw, pt, pose, buffers, light);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, int x, int y, int z, float u, float v, int nx, int ny, int nz, int light) {
        vc.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
