package thaumcraft.client.renderers.entity.construct;

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
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.obj.WavefrontObject;
import thaumcraft.common.entities.construct.EntityTurretCrossbowAdvanced;

/**
 * Улучшенный арбалет (порт RenderTurretCrossbowAdvanced): OBJ — ноги (в вагонетке ниже), корпус с мозгом и щитом
 * поворачивается по взгляду, загрузчик ездит при перезарядке, плечи дуги расходятся при выстреле; раненый — красноватый
 * и трясётся.
 */
public class RenderTurretCrossbowAdvanced extends EntityRenderer<EntityTurretCrossbowAdvanced> {
    private static final ResourceLocation TURMODEL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "models/obj/crossbow_advanced.obj");
    private static final ResourceLocation RL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crossbow_advanced.png");
    private WavefrontObject model;

    public RenderTurretCrossbowAdvanced(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5F;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTurretCrossbowAdvanced e) {
        return RL;
    }

    @Override
    public void render(EntityTurretCrossbowAdvanced turret, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        if (this.model == null) {
            this.model = WavefrontObject.load(TURMODEL);
        }
        turret.loadProgressForRender = turret.getLoadProgress(pt);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(RL));
        int ov = OverlayTexture.NO_OVERLAY;
        pose.pushPose();
        pose.translate(0.0, 0.75, 0.0);
        pose.pushPose();
        if (turret.getVehicle() instanceof AbstractMinecart) {
            pose.scale(0.66F, 0.75F, 0.66F);
        }
        this.model.renderPartEntity("legs", vc, pose.last(), light, ov, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
        pose.pushPose();
        float g = 1.0F;
        float b = 1.0F;
        if (turret.hurtTime > 0) {
            g = 0.5F;
            b = 0.5F;
            float jiggle = turret.hurtTime / 500.0F;
            pose.translate(turret.getRandom().nextGaussian() * jiggle, turret.getRandom().nextGaussian() * jiggle, turret.getRandom().nextGaussian() * jiggle);
        }
        pose.mulPose(Axis.YN.rotationDegrees(Mth.lerp(pt, turret.yHeadRotO, turret.yHeadRot)));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, turret.xRotO, turret.getXRot())));
        for (String part : new String[] {"mech", "box", "shield", "brain"}) {
            this.model.renderPartEntity(part, vc, pose.last(), light, ov, 1.0F, g, b, 1.0F);
        }
        pose.pushPose();
        pose.translate(0.0, 0.0, Mth.sin(Mth.sqrt(turret.loadProgressForRender) * (float) Math.PI * 2.0F) / 12.0F);
        this.model.renderPartEntity("loader", vc, pose.last(), light, ov, 1.0F, g, b, 1.0F);
        pose.popPose();
        float swing = turret.getAttackAnim(pt);
        float sp = Mth.sin(Mth.sqrt(swing) * (float) Math.PI * 2.0F) * 20.0F;
        pose.translate(0.0, 0.0, 0.375);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(sp));
        this.model.renderPartEntity("bow1", vc, pose.last(), light, ov, 1.0F, g, b, 1.0F);
        pose.popPose();
        pose.pushPose();
        pose.mulPose(Axis.YN.rotationDegrees(sp));
        this.model.renderPartEntity("bow2", vc, pose.last(), light, ov, 1.0F, g, b, 1.0F);
        pose.popPose();
        pose.popPose();
        pose.popPose();
        super.render(turret, yaw, pt, pose, buffers, light);
    }
}
