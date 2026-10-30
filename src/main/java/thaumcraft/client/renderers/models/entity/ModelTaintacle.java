package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;

/**
 * Модель заражённого щупальца (порт ModelTaintacle + ModelRendererTaintacle): основание и цепочка из length − 1 звеньев
 * (каждое следующее ×0.88), на конце — шар и «головка» 12×12×12. Звенья извиваются, сильнее при ударе (flailIntensity).
 * Появившись, щупальце вырастает из земли за height·10 тиков.
 */
public class ModelTaintacle extends EntityModel<EntityTaintacle> {
    private static final float SCALE = 0.88F;
    private final int length;
    private final ModelPart root;
    private final ModelPart[] tents;
    private final ModelPart orb;
    private final float[] rotX;
    private final float[] rotZ;
    private EntityTaintacle entity;

    public ModelTaintacle(ModelPart part, int length) {
        this.length = length;
        this.root = part.getChild("tentacle");
        this.tents = new ModelPart[length];
        for (int k = 0; k < length; k++) {
            this.tents[k] = part.getChild("tent" + k);
        }
        this.orb = part.getChild("orb");
        this.rotX = new float[length];
        this.rotZ = new float[length];
    }

    /** Звенья без опорных точек и поворотов — их делает renderChain, как в ModelRendererTaintacle. */
    public static LayerDefinition create(int length) {
        MeshDefinition md = new MeshDefinition();
        var r = md.getRoot();
        r.addOrReplaceChild("tentacle", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8), PartPose.ZERO);
        for (int k = 0; k < length - 1; k++) {
            r.addOrReplaceChild("tent" + k, CubeListBuilder.create().texOffs(0, 16).addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8), PartPose.ZERO);
        }
        r.addOrReplaceChild("tent" + (length - 1), CubeListBuilder.create().texOffs(0, 32).addBox(-6.0F, -6.0F, -6.0F, 12, 12, 12), PartPose.ZERO);
        r.addOrReplaceChild("orb", CubeListBuilder.create().texOffs(0, 56).addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4), PartPose.ZERO);
        return LayerDefinition.create(md, 64, 64);
    }

    @Override
    public void setupAnim(EntityTaintacle t, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.entity = t;
        float flail = t.flailIntensity;
        float ht = t.hurtTime;
        float mod = 0.0625F * 0.2F;
        float fs = flail > 1.0F ? 3.0F : 1.0F - mod;
        float fi = flail + (!(ht > 0.0F) ? -mod : mod);
        for (int k = 0; k < this.length - 1; k++) {
            this.rotX[k] = 0.15F * fi * Mth.sin(ageInTicks * 0.1F * fs - k / 2.0F);
            this.rotZ[k] = 0.1F / fi * Mth.sin(ageInTicks * 0.15F - k / 2.0F);
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        if (this.entity == null) {
            return;
        }
        float h = this.entity.getBbHeight();
        float height = 0.0F;
        float hc = h * 10.0F;
        if (this.entity.tickCount < hc) {
            height = (hc - this.entity.tickCount) / hc * h;
        }
        pose.pushPose();
        pose.translate(0.0F, (h == 3.0F ? 0.6F : 1.2F) + height, 0.0F);
        pose.scale(h / 3.0F, h / 3.0F, h / 3.0F);
        pose.translate(0.0F, 12.0F / 16.0F, 0.0F);
        this.root.render(pose, vc, light, overlay, color);
        this.renderChain(pose, vc, light, overlay, color, 0);
        pose.popPose();
    }

    /** Звено k (масштаб ×0.88 до опорной точки); у предпоследнего — ещё шар и головка. */
    private void renderChain(PoseStack pose, VertexConsumer vc, int light, int overlay, int color, int k) {
        pose.pushPose();
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(0.0F, -8.0F / 16.0F, 0.0F);
        if (this.rotZ[k] != 0.0F) {
            pose.mulPose(Axis.ZP.rotation(this.rotZ[k]));
        }
        if (this.rotX[k] != 0.0F) {
            pose.mulPose(Axis.XP.rotation(this.rotX[k]));
        }
        this.tents[k].render(pose, vc, light, overlay, color);
        if (k < this.length - 2) {
            this.renderChain(pose, vc, light, overlay, color, k + 1);
        } else {
            // шар и головка — дети предпоследнего звена, без поворота
            for (ModelPart tip : new ModelPart[] {this.orb, this.tents[this.length - 1]}) {
                pose.pushPose();
                pose.scale(SCALE, SCALE, SCALE);
                pose.translate(0.0F, -8.0F / 16.0F, 0.0F);
                tip.render(pose, vc, light, overlay, color);
                pose.popPose();
            }
        }
        pose.popPose();
    }
}
