package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import thaumcraft.common.entities.monster.IArmLifting;

/**
 * Потусторонний страж/надзиратель (порт ModelEldritchGuardian): капюшон смотрит за взглядом, руки подняты (выше — при
 * атаке, armLift), плащ, боковые и передняя панели колышутся. Глаз капюшона (HoodEye) рисует рендер надзирателя отдельно.
 */
public class ModelEldritchGuardian extends EntityModel<Mob> {
    private static final String[] PARTS = {"BeltR", "Mbelt", "MbeltL", "MbeltR", "BeltL", "Chestplate", "Hood1", "Backplate", "ShoulderplateTopR",
            "ShoulderplateR1", "ShoulderplateR2", "ShoulderplateR3", "ShoulderR", "ArmL1", "ArmR1", "ShoulderL", "ShoulderplateLtop", "ShoulderplateL1",
            "ShoulderplateL2", "ShoulderplateL3", "LegpanelR4", "LegpanelR5", "LegpanelR6", "BackpanelR1", "BackpanelR2", "BackpanelR3", "BackpanelL3",
            "LegpanelL4", "LegpanelL5", "LegpanelL6", "BackpanelL1", "BackpanelL2", "Cloak1", "SidepanelR1", "SidepanelL1", "SidepanelL2", "SidepanelR2",
            "LegpanelC1"};
    private final ModelPart[] parts = new ModelPart[PARTS.length];
    public final ModelPart hoodEye;
    /** Общая прозрачность (задаёт рендер по расстоянию до камеры). */
    public float alpha = 1.0F;
    private final ModelPart hood1, armL1, armR1, legC1, legC2, legC3, cloak1, cloak2, cloak3, sideL2, sideL3, sideL4, sideR2, sideR3, sideR4;

    public ModelEldritchGuardian(ModelPart root) {
        for (int i = 0; i < PARTS.length; i++) {
            this.parts[i] = root.getChild(PARTS[i]);
        }
        this.hoodEye = root.getChild("HoodEye");
        this.hood1 = root.getChild("Hood1");
        this.armL1 = root.getChild("ArmL1");
        this.armR1 = root.getChild("ArmR1");
        this.legC1 = root.getChild("LegpanelC1");
        this.legC2 = this.legC1.getChild("LegpanelC2");
        this.legC3 = this.legC2.getChild("LegpanelC3");
        this.cloak1 = root.getChild("Cloak1");
        this.cloak2 = this.cloak1.getChild("Cloak2");
        this.cloak3 = this.cloak2.getChild("Cloak3");
        this.sideL2 = root.getChild("SidepanelL2");
        this.sideL3 = this.sideL2.getChild("SidepanelL3");
        this.sideL4 = this.sideL3.getChild("SidepanelL4");
        this.sideR2 = root.getChild("SidepanelR2");
        this.sideR3 = this.sideR2.getChild("SidepanelR3");
        this.sideR4 = this.sideR3.getChild("SidepanelR4");
    }

    @Override
    public void setupAnim(Mob entity, float limbSwing, float limbSwingAmount, float age, float yaw, float pitch) {
        this.hood1.yRot = yaw * Mth.DEG_TO_RAD;
        this.hood1.xRot = pitch * Mth.DEG_TO_RAD;
        this.hoodEye.xRot = this.hood1.xRot;
        this.hoodEye.yRot = this.hood1.yRot;
        float alr = 0.0F;
        float all = 0.0F;
        if (entity instanceof IArmLifting a) {
            alr = a.getArmLiftR();
            all = a.getArmLiftL();
        }
        // ageInTicks = tickCount + partialTicks (как ticksExisted + partialTicks оригинала)
        this.armL1.xRot = -1.0F - all + Mth.sin((age + 20) / 10.0F) * 0.08F;
        this.armR1.xRot = -1.0F - alr + Mth.sin(age / 10.0F) * 0.08F;
        this.legC1.xRot = -0.15F + Mth.sin(age / 8.0F) * 0.12F;
        this.legC2.xRot = Mth.sin((age - 5.0F) / 8.0F) * 0.13F;
        this.legC3.xRot = Mth.sin((age - 10.0F) / 8.0F) * 0.14F;
        this.cloak1.xRot = 0.2F + Mth.sin(age / 7.0F) * 0.08F;
        this.cloak2.xRot = Mth.sin((age - 5.0F) / 7.0F) * 0.1F;
        this.cloak3.xRot = Mth.sin((age - 10.0F) / 7.0F) * 0.12F;
        this.sideL2.zRot = -0.2F + Mth.sin((age + 10) / 8.0F) * 0.12F;
        this.sideL3.zRot = Mth.sin((age + 5.0F) / 8.0F) * 0.13F;
        this.sideL4.zRot = Mth.sin(age / 8.0F) * 0.14F;
        this.sideR2.zRot = 0.2F + Mth.sin((age - 5) / 8.0F) * 0.12F;
        this.sideR3.zRot = Mth.sin((age - 10.0F) / 8.0F) * 0.13F;
        this.sideR4.zRot = Mth.sin((age - 15.0F) / 8.0F) * 0.14F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        int a = (int) ((color >>> 24) * this.alpha);
        color = (a << 24) | (color & 0xFFFFFF);
        for (ModelPart p : this.parts) {
            p.render(pose, vc, light, overlay, color);
        }
    }
}
