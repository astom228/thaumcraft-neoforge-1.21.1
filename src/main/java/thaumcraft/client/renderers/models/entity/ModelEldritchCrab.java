package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.EntityEldritchCrab;

/** Потусторонний краб (порт ModelEldritchCrab): хвост в шлеме или голый, ноги семенят, клешни щёлкают. */
public class ModelEldritchCrab extends EntityModel<EntityEldritchCrab> {
    private static final String[] PARTS = {"RFLeg1", "RClaw1", "Head1", "RClaw0", "RClaw2", "LClaw2", "LClaw1", "RArm", "Torso", "RRLeg1", "Head0",
            "LRLeg1", "LFLeg1", "RRLeg0", "RFLeg0", "LFLeg0", "LRLeg0", "LClaw0", "LArm"};
    private final ModelPart tailHelm;
    private final ModelPart tailBare;
    private final ModelPart[] parts = new ModelPart[PARTS.length];
    private final ModelPart rrLeg1, rfLeg1, lrLeg1, lfLeg1, rrLeg0, rfLeg0, lfLeg0, lrLeg0, rClaw1, rClaw2, lClaw1, lClaw2;
    private boolean helm;

    public ModelEldritchCrab(ModelPart root) {
        this.tailHelm = root.getChild("TailHelm");
        this.tailBare = root.getChild("TailBare");
        for (int i = 0; i < PARTS.length; i++) {
            this.parts[i] = root.getChild(PARTS[i]);
        }
        this.rrLeg1 = root.getChild("RRLeg1");
        this.rfLeg1 = root.getChild("RFLeg1");
        this.lrLeg1 = root.getChild("LRLeg1");
        this.lfLeg1 = root.getChild("LFLeg1");
        this.rrLeg0 = root.getChild("RRLeg0");
        this.rfLeg0 = root.getChild("RFLeg0");
        this.lfLeg0 = root.getChild("LFLeg0");
        this.lrLeg0 = root.getChild("LRLeg0");
        this.rClaw1 = root.getChild("RClaw1");
        this.rClaw2 = root.getChild("RClaw2");
        this.lClaw1 = root.getChild("LClaw1");
        this.lClaw2 = root.getChild("LClaw2");
    }

    private static void rot(ModelPart p, float x, float y, float z) {
        p.xRot = x;
        p.yRot = y;
        p.zRot = z;
    }

    @Override
    public void setupAnim(EntityEldritchCrab crab, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        this.helm = crab.hasHelm();
        rot(this.rrLeg1, 0.0F, 0.2094395F, 0.4363323F);
        rot(this.rfLeg1, 0.0F, -0.2094395F, 0.4363323F);
        rot(this.lrLeg1, 0.0F, -0.2094395F, -0.4363323F);
        rot(this.lfLeg1, 0.0F, 0.2094395F, -0.4363323F);
        rot(this.rrLeg0, 0.0F, 0.2094395F, 0.4363323F);
        rot(this.rfLeg0, 0.0F, -0.2094395F, 0.4363323F);
        rot(this.lfLeg0, 0.0F, 0.2094395F, -0.4363323F);
        rot(this.lrLeg0, 0.0F, -0.2094395F, -0.4363323F);
        float f9 = -(Mth.cos(limbSwing * 0.6662F * 2.0F) * 0.4F) * limbSwingAmount;
        float f10 = -(Mth.cos(limbSwing * 0.6662F * 2.0F + (float) Math.PI) * 0.4F) * limbSwingAmount;
        this.rrLeg1.yRot += f9;
        this.rrLeg0.yRot += f9;
        this.lrLeg1.yRot += -f9;
        this.lrLeg0.yRot += -f9;
        this.rfLeg1.yRot += f10;
        this.rfLeg0.yRot += f10;
        this.lfLeg1.yRot += -f10;
        this.lfLeg0.yRot += -f10;
        this.rrLeg1.zRot += f9;
        this.rrLeg0.zRot += f9;
        this.lrLeg1.zRot += -f9;
        this.lrLeg0.zRot += -f9;
        this.rfLeg1.zRot += f10;
        this.rfLeg0.zRot += f10;
        this.lfLeg1.zRot += -f10;
        this.lfLeg0.zRot += -f10;
        this.tailBare.yRot = this.tailHelm.yRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.125F;
        this.tailBare.zRot = this.tailHelm.zRot = Mth.cos(limbSwing * 0.6662F) * limbSwingAmount * 0.125F;
        this.rClaw2.xRot = 0.3141593F - Mth.sin(crab.tickCount / 4.0F) * 0.25F;
        this.lClaw2.xRot = 0.3141593F + Mth.sin(crab.tickCount / 4.1F) * 0.25F;
        this.rClaw1.xRot = Mth.sin(crab.tickCount / 4.0F) * 0.125F;
        this.lClaw1.xRot = -Mth.sin(crab.tickCount / 4.1F) * 0.125F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        (this.helm ? this.tailHelm : this.tailBare).render(pose, vc, light, overlay, color);
        for (ModelPart p : this.parts) {
            p.render(pose, vc, light, overlay, color);
        }
    }
}
