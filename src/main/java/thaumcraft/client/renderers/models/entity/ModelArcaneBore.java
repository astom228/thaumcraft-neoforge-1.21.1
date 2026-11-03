package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import thaumcraft.common.entities.construct.EntityArcaneBore;

/** Магический бур (порт ModelArcaneBore): треножник и поворотная голова с кристаллом под куполом. */
public class ModelArcaneBore extends EntityModel<EntityArcaneBore> {
    private final ModelPart leg1, leg2, leg3, leg4, tripod, base;

    public ModelArcaneBore(ModelPart root) {
        this.leg1 = root.getChild("leg1");
        this.leg2 = root.getChild("leg2");
        this.leg3 = root.getChild("leg3");
        this.leg4 = root.getChild("leg4");
        this.tripod = root.getChild("tripod");
        this.base = root.getChild("base");
    }

    @Override
    public void setupAnim(EntityArcaneBore entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch) {
        this.base.yRot = headYaw / (180.0F / (float) Math.PI);
        this.base.xRot = headPitch / (180.0F / (float) Math.PI);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        this.leg2.render(pose, vc, light, overlay, color);
        this.tripod.render(pose, vc, light, overlay, color);
        this.leg3.render(pose, vc, light, overlay, color);
        this.leg4.render(pose, vc, light, overlay, color);
        this.leg1.render(pose, vc, light, overlay, color);
        this.base.render(pose, vc, light, overlay, color);
    }
}
