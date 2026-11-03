package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

/**
 * Арбалет-турель (порт ModelCrossbow): ложе поворачивается за взглядом, плечи дуги сходятся при выстреле, рычаг
 * перезарядки качается; в вагонетке ножки подгибаются и опускаются (offsetY −0.5 блока).
 */
public class ModelCrossbow extends EntityModel<EntityTurretCrossbow> {
    private final ModelPart crossbow, tripod, leg1, leg2, leg3, leg4;
    private final ModelPart loadbarcross, loadbarl, loadbarr, crossl1, crossl2, crossl3, crossr1, crossr2, crossr3;

    public ModelCrossbow(ModelPart root) {
        this.crossbow = root.getChild("crossbow");
        this.tripod = root.getChild("tripod");
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.loadbarcross = this.crossbow.getChild("loadbarcross");
        this.loadbarl = this.crossbow.getChild("loadbarl");
        this.loadbarr = this.crossbow.getChild("loadbarr");
        this.crossl1 = this.crossbow.getChild("crossl1");
        this.crossl2 = this.crossbow.getChild("crossl2");
        this.crossl3 = this.crossbow.getChild("crossl3");
        this.crossr1 = this.crossbow.getChild("crossr1");
        this.crossr2 = this.crossbow.getChild("crossr2");
        this.crossr3 = this.crossbow.getChild("crossr3");
    }

    @Override
    public void setupAnim(EntityTurretCrossbow entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch) {
        this.crossbow.yRot = headYaw / (180.0F / (float) Math.PI);
        this.crossbow.xRot = headPitch / (180.0F / (float) Math.PI);
        if (this.attackTime > -9990.0F) {
            float f6 = this.attackTime;
            float l = -0.2F + Mth.sin(Mth.sqrt(f6) * (float) Math.PI * 2.0F) * 0.2F;
            float r = 0.2F - Mth.sin(Mth.sqrt(f6) * (float) Math.PI * 2.0F) * 0.2F;
            this.crossl1.yRot = this.crossl2.yRot = this.crossl3.yRot = l;
            this.crossr1.yRot = this.crossr2.yRot = this.crossr3.yRot = r;
        }
        float lp = entity.loadProgressForRender;
        this.loadbarcross.xRot = this.loadbarl.xRot = this.loadbarr.xRot = -0.5F + Mth.sin(Mth.sqrt(lp) * (float) Math.PI * 2.0F) * 0.5F;
        boolean cart = entity.getVehicle() instanceof AbstractMinecart;
        float y = cart ? 12.0F - 8.0F : 12.0F;
        float rx = cart ? 0.1F : 0.5F;
        for (ModelPart leg : new ModelPart[] {this.leg1, this.leg2, this.leg3, this.leg4}) {
            leg.y = y;
            leg.xRot = rx;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        this.crossbow.render(pose, vc, light, overlay, color);
        this.leg2.render(pose, vc, light, overlay, color);
        this.tripod.render(pose, vc, light, overlay, color);
        this.leg3.render(pose, vc, light, overlay, color);
        this.leg4.render(pose, vc, light, overlay, color);
        this.leg1.render(pose, vc, light, overlay, color);
    }
}
