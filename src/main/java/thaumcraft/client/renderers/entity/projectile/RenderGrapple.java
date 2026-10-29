package thaumcraft.client.renderers.entity.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.PolyCone;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.entities.projectile.EntityGrapple;

/**
 * Крюк и верёвка (порт RenderGrapple): модель крюка (ModelGrappler), пульсирующее свечение (кадр частиц, аддитивно) и верёвка
 * от руки бросившего до крюка — трубка PolyCone (в 1.12 — glePolyCone, tess 4) с волной, затухающей после зацепа.
 */
public class RenderGrapple extends EntityRenderer<EntityGrapple> {
    private static final ResourceLocation BEAM = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/grappler.png");
    private static final ResourceLocation ROPE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/rope.png");
    private final ModelPart model;

    public RenderGrapple(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        MeshDefinition md = new MeshDefinition();
        PartDefinition root = md.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("prong1", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5), PartPose.ZERO);
        root.addOrReplaceChild("prong2", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5),
                PartPose.rotation(0.0F, (float) (Math.PI / 2), 0.0F));
        root.addOrReplaceChild("prong3", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5),
                PartPose.rotation((float) (Math.PI / 2), (float) (Math.PI / 2), 0.0F));
        this.model = LayerDefinition.create(md, 64, 32).bakeRoot();
    }

    @Override
    public boolean shouldRender(EntityGrapple entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(EntityGrapple entity, float yaw, float pticks, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pticks, entity.yRotO, entity.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pticks, entity.xRotO, entity.getXRot())));
        this.model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(BEAM)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        // свечение: кадр 1–6 строки 7 сетки 32×32 текстуры частиц, обращён к камере
        float f2 = (1 + entity.tickCount % 6) / 32.0F;
        float f3 = f2 + 0.03125F;
        float f4 = 0.21875F;
        float f5 = f4 + 0.03125F;
        float f7 = 0.5F;
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float bob = Mth.sin(entity.tickCount / 5.0F) * 0.2F + 0.2F;
        pose.scale(1.0F + bob, 1.0F + bob, 1.0F + bob);
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.additiveEntity(ParticleEngine.particleTexture));
        PoseStack.Pose p = pose.last();
        vc.addVertex(p, -f7, -f7, 0.0F).setColor(1.0F, 1.0F, 1.0F, 0.21F).setUv(f2, f5).setOverlay(OverlayTexture.NO_OVERLAY).setLight(220).setNormal(p, 0, 1, 0);
        vc.addVertex(p, f7, -f7, 0.0F).setColor(1.0F, 1.0F, 1.0F, 0.21F).setUv(f3, f5).setOverlay(OverlayTexture.NO_OVERLAY).setLight(220).setNormal(p, 0, 1, 0);
        vc.addVertex(p, f7, f7, 0.0F).setColor(1.0F, 1.0F, 1.0F, 0.21F).setUv(f3, f4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(220).setNormal(p, 0, 1, 0);
        vc.addVertex(p, -f7, f7, 0.0F).setColor(1.0F, 1.0F, 1.0F, 0.21F).setUv(f2, f4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(220).setNormal(p, 0, 1, 0);
        pose.popPose();
        this.renderRope(entity, pticks, pose, buffers);
        super.render(entity, yaw, pticks, pose, buffers, light);
    }

    /** calcPoints + glePolyCone оригинала: точки верёвки относительно крюка. */
    private void renderRope(EntityGrapple grapple, float pt, PoseStack pose, MultiBufferSource buffers) {
        LivingEntity thrower = grapple.getThrower();
        if (thrower == null) {
            return;
        }
        double tx = Mth.lerp(pt, thrower.xo, thrower.getX());
        double ty = Mth.lerp(pt, thrower.yo, thrower.getY());
        double tz = Mth.lerp(pt, thrower.zo, thrower.getZ());
        if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            double yy = Mth.lerp(pt, thrower.yRotO, thrower.getYRot());
            double px = -Mth.cos((float) ((yy - 0.5) / 180.0 * 3.141593F)) * 0.1F * (grapple.hand == InteractionHand.MAIN_HAND ? 1 : -1);
            double pz = -Mth.sin((float) ((yy - 0.5) / 180.0 * 3.141593F)) * 0.1F * (grapple.hand == InteractionHand.MAIN_HAND ? 1 : -1);
            Vec3 vl = thrower.getLookAngle();
            tx += px + vl.x / 5.0;
            ty += thrower.getEyeHeight() / 2.6 + vl.y / 5.0;
            tz += pz + vl.z / 5.0;
        }
        double gx = Mth.lerp(pt, grapple.xo, grapple.getX());
        double gy = Mth.lerp(pt, grapple.yo, grapple.getY());
        double gz = Mth.lerp(pt, grapple.zo, grapple.getZ());
        List<Vec3> points = new ArrayList<>();
        Vec3 ve = new Vec3(tx - gx, ty - gy + thrower.getBbHeight() / 2.0F, tz - gz);
        float length = (float) (ve.length() * 5.0);
        int steps = (int) length;
        points.add(Vec3.ZERO);
        for (int a = 1; a < steps - 1; a++) {
            float dist = a * (length / steps);
            float ss = 1.0F - a / (steps * 1.25F);
            double dx = (tx - gx) / steps * a + Mth.sin(dist / 10.0F) * grapple.ampl * ss;
            double dy = (ty - gy + thrower.getBbHeight() / 2.0F) / steps * a + Mth.sin(dist / 8.0F) * grapple.ampl * ss;
            double dz = (tz - gz) / steps * a + Mth.sin(dist / 6.0F) * grapple.ampl * ss;
            points.add(new Vec3(dx, dy, dz));
        }
        points.add(ve);
        points.add(ve);
        if (points.size() <= 2) {
            return;
        }
        double[][] pp = new double[points.size()][3];
        float[][] colours = new float[points.size()][4];
        double[] radii = new double[points.size()];
        for (int a = 0; a < points.size(); a++) {
            pp[a][0] = points.get(a).x;
            pp[a][1] = points.get(a).y;
            pp[a][2] = points.get(a).z;
            colours[a][0] = colours[a][1] = colours[a][2] = colours[a][3] = 1.0F;
            radii[a] = 0.025;
        }
        Matrix4f m = pose.last().pose();
        PolyCone.draw(buffers.getBuffer(TCRenderTypes.tube(ROPE)), m, 4, pp, colours, radii, 2.0F, length - points.size());
    }

    @Override
    public ResourceLocation getTextureLocation(EntityGrapple entity) {
        return BEAM;
    }
}
