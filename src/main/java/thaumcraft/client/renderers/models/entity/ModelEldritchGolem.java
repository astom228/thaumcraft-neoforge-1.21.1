package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.boss.EntityEldritchGolem;

/**
 * Потусторонний голем (порт ModelEldritchGolem): плащ и полы одежды колышутся при ходьбе, руки бьют сверху вниз;
 * пока идёт таймер появления, голова опущена; без головы — малая голова-ядро (Head2) смотрит прямо на цель.
 */
public class ModelEldritchGolem extends EntityModel<EntityEldritchGolem> {
    /** Порядок отрисовки оригинала (Frontcloth1 рисовался дважды). */
    private static final String[] BEFORE_HEAD = {"Frontcloth1", "CollarL", "CollarBlack", "Cloak1", "CloakCL", "CloakCR", "Cloak3", "Cloak2"};
    private static final String[] AFTER_HEAD = {"Frontcloth0", "CollarB", "Torso", "CollarR", "CollarF", "Frontcloth1", "ArmL", "ArmR", "BackpanelR1", "WaistR1",
            "WaistR2", "WaistR3", "LegR", "WaistL1", "WaistL2", "WaistL3", "Frontcloth2", "BackpanelL1", "LegL"};
    private final ModelPart[] before = new ModelPart[BEFORE_HEAD.length];
    private final ModelPart[] after = new ModelPart[AFTER_HEAD.length];
    private final ModelPart head, head2, frontcloth1, frontcloth2, cloak1, cloak2, cloak3, legR, legL, armR, armL;
    private boolean headless;

    public ModelEldritchGolem(ModelPart root) {
        for (int i = 0; i < BEFORE_HEAD.length; i++) {
            this.before[i] = root.getChild(BEFORE_HEAD[i]);
        }
        for (int i = 0; i < AFTER_HEAD.length; i++) {
            this.after[i] = root.getChild(AFTER_HEAD[i]);
        }
        this.head = root.getChild("Head");
        this.head2 = root.getChild("Head2");
        this.frontcloth1 = root.getChild("Frontcloth1");
        this.frontcloth2 = root.getChild("Frontcloth2");
        this.cloak1 = root.getChild("Cloak1");
        this.cloak2 = root.getChild("Cloak2");
        this.cloak3 = root.getChild("Cloak3");
        this.legR = root.getChild("LegR");
        this.legL = root.getChild("LegL");
        this.armR = root.getChild("ArmR");
        this.armL = root.getChild("ArmL");
    }

    @Override
    public void prepareMobModel(EntityEldritchGolem golem, float limbSwing, float limbSwingAmount, float pt) {
        int i = golem.getAttackTimer();
        if (i > 0) {
            this.armR.xRot = -2.0F + 1.5F * doAbs(i - pt, 10.0F);
            this.armL.xRot = -2.0F + 1.5F * doAbs(i - pt, 10.0F);
        } else {
            this.armR.xRot = Mth.cos(limbSwing * 0.4F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
            this.armL.xRot = Mth.cos(limbSwing * 0.4F) * 2.0F * limbSwingAmount * 0.5F;
        }
    }

    private static float doAbs(float a, float b) {
        return (Math.abs(a % b - b * 0.5F) - b * 0.25F) / (b * 0.25F);
    }

    @Override
    public void setupAnim(EntityEldritchGolem golem, float f, float f1, float age, float yaw, float pitch) {
        this.headless = golem.isHeadless();
        if (golem.getSpawnTimer() > 0) {
            this.head.yRot = 0.0F;
            this.head.xRot = golem.getSpawnTimer() / 2 / (180.0F / (float) Math.PI);
        } else {
            this.head.yRot = yaw / 4.0F / (180.0F / (float) Math.PI);
            this.head.xRot = pitch / 2.0F / (180.0F / (float) Math.PI);
            this.head2.yRot = yaw / (180.0F / (float) Math.PI);
            this.head2.xRot = pitch / (180.0F / (float) Math.PI);
        }
        this.legR.xRot = Mth.cos(f * 0.4662F) * 1.4F * f1;
        this.legL.xRot = Mth.cos(f * 0.4662F + (float) Math.PI) * 1.4F * f1;
        float a = Mth.cos(f * 0.44F) * 1.4F * f1;
        float b = Mth.cos(f * 0.44F + (float) Math.PI) * 1.4F * f1;
        float c = Math.min(a, b);
        this.frontcloth1.xRot = c - 0.1047198F;
        this.frontcloth2.xRot = c - 0.3316126F;
        this.cloak1.xRot = -c / 3.0F + 0.1396263F;
        this.cloak2.xRot = -c / 3.0F + 0.3069452F;
        this.cloak3.xRot = -c / 3.0F + 0.4465716F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        for (ModelPart p : this.before) {
            p.render(pose, vc, light, overlay, color);
        }
        (this.headless ? this.head2 : this.head).render(pose, vc, light, overlay, color);
        for (ModelPart p : this.after) {
            p.render(pose, vc, light, overlay, color);
        }
    }
}
