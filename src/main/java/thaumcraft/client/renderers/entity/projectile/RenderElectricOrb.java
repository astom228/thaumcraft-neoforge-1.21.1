package thaumcraft.client.renderers.entity.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.entities.projectile.EntityGolemOrb;

/**
 * Рендер электрической сферы (порт RenderElectricOrb): повёрнутый к камере кадр из частиц (6 кадров, строка 7, красная —
 * строка 6), аддитивно, яркость 220, пульсирует в размере.
 */
public class RenderElectricOrb<T extends Entity> extends EntityRenderer<T> {
    private static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/particles.png");

    public RenderElectricOrb(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(T entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        float f2 = (1 + entity.tickCount % 6) / 8.0F;
        float f3 = f2 + 0.125F;
        float f4 = entity instanceof EntityGolemOrb orb && orb.red ? 0.75F : 0.875F;
        float f5 = f4 + 0.125F;
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        float bob = Mth.sin(entity.tickCount / 5.0F) * 0.2F + 0.2F;
        pose.scale(1.0F + bob, 1.0F + bob, 1.0F + bob);
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.glow(PARTICLES));
        PoseStack.Pose p = pose.last();
        int lm = 220 << 16;
        vc.addVertex(p, -0.5F, -0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f2, f5).setLight(lm);
        vc.addVertex(p, 0.5F, -0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f3, f5).setLight(lm);
        vc.addVertex(p, 0.5F, 0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f3, f4).setLight(lm);
        vc.addVertex(p, -0.5F, 0.5F, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f2, f4).setLight(lm);
        pose.popPose();
        super.render(entity, yaw, pt, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return PARTICLES;
    }
}
