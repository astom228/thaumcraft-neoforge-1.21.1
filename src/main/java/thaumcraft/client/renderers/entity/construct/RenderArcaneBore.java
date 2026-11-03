package thaumcraft.client.renderers.entity.construct;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.client.renderers.models.entity.ModelArcaneBore;
import thaumcraft.common.entities.construct.EntityArcaneBore;

/**
 * Магический бур (порт RenderArcaneBore): модель с полупрозрачным куполом; во время копания — светящийся сгусток у
 * кристалла и вращающийся луч (три плоскости beam1 с бегущей текстурой) длиной 5 блоков, аддитивно.
 */
public class RenderArcaneBore extends MobRenderer<EntityArcaneBore, ModelArcaneBore> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "arcanebore"), "main");
    private static final ResourceLocation RL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/arcanebore.png");
    private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/beam1.png");
    private static final ResourceLocation NODES = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/auranodes.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    public RenderArcaneBore(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelArcaneBore(ctx.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityArcaneBore entity) {
        return RL;
    }

    @Override
    protected RenderType getRenderType(EntityArcaneBore e, boolean visible, boolean translucent, boolean glowing) {
        return visible ? RenderType.entityTranslucent(RL) : super.getRenderType(e, visible, translucent, glowing);
    }

    @Override
    public void render(EntityArcaneBore bore, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        bore.yBodyRot = 0.0F;
        bore.yBodyRotO = 0.0F;
        super.render(bore, yaw, pt, pose, buffers, light);
        if (!(bore.clientDigging && bore.isActive() && bore.validInventory())) {
            return;
        }
        int part = bore.tickCount % 32;
        Vec3 lv2 = new Vec3(0.5, 0.075, 0.0);
        lv2 = EntityArcaneBore.rotateAroundZ(lv2, bore.getXRot() / 180.0F * (float) Math.PI);
        lv2 = EntityArcaneBore.rotateAroundY(lv2, -((bore.getYHeadRot() + 90.0F) / 180.0F * (float) Math.PI));
        pose.pushPose();
        pose.translate(lv2.x, bore.getEyeHeight() + lv2.y, lv2.z);
        // сгусток (renderBillboardQuad: кадр 96+part сетки 32×32 auranodes, к камере)
        Quaternionf q = this.entityRenderDispatcher.cameraOrientation();
        billboard(buffers.getBuffer(TCRenderTypes.glowNoDepth(NODES)), pose, q, 0.5F, 96 + part, 0.0F, 1.0F, 0.4F, 0.8F);
        // луч
        pose.pushPose();
        float rot = (float) (bore.level().getDayTime() % 72L * 5L) + 5.0F * pt;
        float op = 0.4F;
        float var11 = -(bore.tickCount + pt);
        float var12 = -var11 * 0.2F - Mth.floor(-var11 * 0.1F);
        float ry = Mth.lerp(pt, bore.yRotO, bore.getYRot());
        float rp = Mth.lerp(pt, bore.xRotO, bore.getXRot());
        pose.mulPose(Axis.XN.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZN.rotationDegrees(180.0F + ry));
        pose.mulPose(Axis.XN.rotationDegrees(rp));
        pose.mulPose(Axis.YP.rotationDegrees(rot));
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.glowNoDepth(BEAM));
        double beamLength = 5.0;
        float w = 0.15F;
        for (int t = 0; t < 3; t++) {
            pose.mulPose(Axis.YP.rotationDegrees(60.0F));
            float v0 = -1.0F + var12 + t / 3.0F;
            float v1 = (float) beamLength + v0;
            Matrix4f m = pose.last().pose();
            vc.addVertex(m, 0.0F, (float) beamLength, 0.0F).setColor(0.0F, 1.0F, 0.4F, op).setUv(1.0F, v1).setLight(FULL_BRIGHT);
            vc.addVertex(m, -w, 0.0F, 0.0F).setColor(0.0F, 1.0F, 0.4F, op).setUv(1.0F, v0).setLight(FULL_BRIGHT);
            vc.addVertex(m, w, 0.0F, 0.0F).setColor(0.0F, 1.0F, 0.4F, op).setUv(0.0F, v0).setLight(FULL_BRIGHT);
            vc.addVertex(m, 0.0F, (float) beamLength, 0.0F).setColor(0.0F, 1.0F, 0.4F, op).setUv(0.0F, v1).setLight(FULL_BRIGHT);
        }
        pose.popPose();
        pose.popPose();
    }

    /** Квадрат к камере, кадр frame сетки 32×32. */
    private static void billboard(VertexConsumer vc, PoseStack pose, Quaternionf q, float scale, int frame, float r, float g, float b, float a) {
        float f1 = frame % 32 / 32.0F;
        float f2 = f1 + 1.0F / 32.0F;
        float f3 = frame / 32 / 32.0F;
        float f4 = f3 + 1.0F / 32.0F;
        Matrix4f m = pose.last().pose();
        v(vc, m, q, -scale, -scale, f2, f4, r, g, b, a);
        v(vc, m, q, -scale, scale, f2, f3, r, g, b, a);
        v(vc, m, q, scale, scale, f1, f3, r, g, b, a);
        v(vc, m, q, scale, -scale, f1, f4, r, g, b, a);
    }

    private static void v(VertexConsumer vc, Matrix4f m, Quaternionf q, float x, float y, float u, float t, float r, float g, float b, float a) {
        Vector3f p = new Vector3f(x, y, 0.0F).rotate(q);
        vc.addVertex(m, p.x(), p.y(), p.z()).setColor(r, g, b, a).setUv(u, t).setLight(FULL_BRIGHT);
    }
}
