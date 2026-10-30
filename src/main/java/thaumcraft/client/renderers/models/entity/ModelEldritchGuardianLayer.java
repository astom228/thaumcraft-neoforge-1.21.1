package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Геометрия ModelEldritchGuardian (сгенерировано tools/models/convert-entity-models.js — не править вручную). Имена частей — поля оригинала. */
@SuppressWarnings("unused")
public final class ModelEldritchGuardianLayer {
    private ModelEldritchGuardianLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("BeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 4.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(56, 55).mirror(false).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("BeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 4.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Chestplate", CubeListBuilder.create().texOffs(56, 45).mirror(false).addBox(-4.0F, 1.0F, -4.0F, 8.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("HoodEye", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        {
            PartDefinition Hood1 = root.addOrReplaceChild("Hood1", CubeListBuilder.create().texOffs(40, 12).mirror(false).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            Hood1.addOrReplaceChild("Hood2", CubeListBuilder.create().texOffs(36, 28).mirror(false).addBox(-3.5F, -8.7F, 2.0F, 7.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2268928F, 0.0F, 0.0F));
            Hood1.addOrReplaceChild("Hood3", CubeListBuilder.create().texOffs(22, 19).mirror(false).addBox(-3.0F, -9.0F, 2.5F, 6.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3490659F, 0.0F, 0.0F));
            Hood1.addOrReplaceChild("Hood4", CubeListBuilder.create().texOffs(40, 4).mirror(false).addBox(-2.5F, -9.7F, 3.5F, 5.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5759587F, 0.0F, 0.0F));
        }
        root.addOrReplaceChild("Backplate", CubeListBuilder.create().texOffs(36, 45).mirror(false).addBox(-4.0F, 1.0F, 2.0F, 8.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, -6.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("ShoulderplateTopR", CubeListBuilder.create().texOffs(110, 37).mirror(false).addBox(-5.5F, -2.5F, -3.5F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(-5.0F, -4.0F, 0.0F, -0.3665191F, 0.3141593F, 0.4363323F));
        root.addOrReplaceChild("ShoulderplateR1", CubeListBuilder.create().texOffs(110, 45).mirror(false).addBox(3.5F, -1.5F, -3.5F, 1.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(5.0F, -4.0F, 0.0F, -0.3665191F, -0.3141593F, -0.4363323F));
        root.addOrReplaceChild("ShoulderplateR2", CubeListBuilder.create().texOffs(94, 45).mirror(false).addBox(-3.5F, 1.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(-5.0F, -4.0F, 0.0F, -0.3665191F, 0.3141593F, 0.4363323F));
        root.addOrReplaceChild("ShoulderplateR3", CubeListBuilder.create().texOffs(94, 45).mirror(false).addBox(-2.5F, 3.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(-5.0F, -4.0F, 0.0F, -0.3665191F, 0.3141593F, 0.4363323F));
        root.addOrReplaceChild("ShoulderR", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-3.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(-5.0F, -4.0F, 0.0F, -0.3665191F, 0.122173F, 0.0349066F));
        {
            PartDefinition ArmL1 = root.addOrReplaceChild("ArmL1", CubeListBuilder.create().texOffs(72, 8).mirror(false).addBox(-1.0F, 2.5F, -1.5F, 4.0F, 10.0F, 5.0F), PartPose.offsetAndRotation(5.0F, -4.0F, 0.0F, -0.9599311F, -0.1047198F, -0.1919862F));
            ArmL1.addOrReplaceChild("ArmL2", CubeListBuilder.create().texOffs(76, 28).mirror(false).addBox(-1.0F, 9.5F, 3.5F, 4.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            ArmL1.addOrReplaceChild("ArmL3", CubeListBuilder.create().texOffs(76, 23).mirror(false).addBox(-1.0F, 6.5F, 3.5F, 4.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        }
        {
            PartDefinition ArmR1 = root.addOrReplaceChild("ArmR1", CubeListBuilder.create().texOffs(72, 8).mirror(false).addBox(-3.0F, 2.5F, -1.5F, 4.0F, 10.0F, 5.0F), PartPose.offsetAndRotation(-5.0F, -4.0F, 0.0F, -0.9599311F, 0.1047198F, 0.1919862F));
            ArmR1.addOrReplaceChild("ArmR2", CubeListBuilder.create().texOffs(76, 28).mirror(false).addBox(-3.0F, 9.5F, 3.5F, 4.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            ArmR1.addOrReplaceChild("ArmR3", CubeListBuilder.create().texOffs(76, 23).mirror(false).addBox(-3.0F, 6.5F, 3.5F, 4.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        }
        root.addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(5.0F, -4.0F, 0.0F, -0.3665191F, -0.122173F, -0.0349066F));
        root.addOrReplaceChild("ShoulderplateLtop", CubeListBuilder.create().texOffs(110, 37).mirror(false).addBox(3.5F, -2.5F, -3.5F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(5.0F, -4.0F, 0.0F, -0.3665191F, -0.3141593F, -0.4363323F));
        root.addOrReplaceChild("ShoulderplateL1", CubeListBuilder.create().texOffs(110, 45).mirror(false).addBox(-4.5F, -1.5F, -3.5F, 1.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(-5.0F, -4.0F, 0.0F, -0.3665191F, 0.3141593F, 0.4363323F));
        root.addOrReplaceChild("ShoulderplateL2", CubeListBuilder.create().texOffs(94, 45).mirror(false).addBox(2.5F, 1.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(5.0F, -4.0F, 0.0F, -0.3665191F, -0.3141593F, -0.4363323F));
        root.addOrReplaceChild("ShoulderplateL3", CubeListBuilder.create().texOffs(94, 45).mirror(false).addBox(1.5F, 3.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(5.0F, -4.0F, 0.0F, -0.3665191F, -0.3141593F, -0.4363323F));
        root.addOrReplaceChild("LegpanelR4", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(-3.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("LegpanelR5", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("LegpanelR6", CubeListBuilder.create().texOffs(6, 43).mirror(false).addBox(-3.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("BackpanelR1", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-3.0F, 0.5F, 2.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("BackpanelR2", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-3.0F, 2.5F, 1.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("BackpanelR3", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-3.0F, 4.5F, 0.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("BackpanelL3", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-2.0F, 4.5F, 0.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("LegpanelL4", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(1.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("LegpanelL5", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(1.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("LegpanelL6", CubeListBuilder.create().texOffs(6, 43).mirror(false).addBox(1.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("BackpanelL1", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-2.0F, 0.5F, 2.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("BackpanelL2", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-2.0F, 2.5F, 1.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        root.addOrReplaceChild("SidepanelL1", CubeListBuilder.create().texOffs(0, 22).mirror(false).addBox(1.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(2.0F, 6.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        root.addOrReplaceChild("SidepanelR1", CubeListBuilder.create().texOffs(0, 22).mirror(false).addBox(-2.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(-2.0F, 6.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        {
            PartDefinition SidepanelR2 = root.addOrReplaceChild("SidepanelR2", CubeListBuilder.create().texOffs(0, 54).mirror(false).addBox(0.0F, 0.0F, -0.5F, 1.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(-4.5F, 9.5F, -2.0F, 0.0F, 0.0F, 0.122173F));
            {
                PartDefinition SidepanelR3 = SidepanelR2.addOrReplaceChild("SidepanelR3", CubeListBuilder.create().texOffs(0, 35).mirror(false).addBox(0.0F, 0.0F, -0.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, 0.296706F));
                SidepanelR3.addOrReplaceChild("SidepanelR4", CubeListBuilder.create().texOffs(24, 35).mirror(false).addBox(0.0F, 0.0F, -0.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
            }
        }
        {
            PartDefinition SidepanelL2 = root.addOrReplaceChild("SidepanelL2", CubeListBuilder.create().texOffs(0, 54).mirror(false).addBox(0.0F, 0.0F, -0.5F, 1.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(4.5F, 9.5F, -2.0F, 0.0F, 0.0F, -0.122173F));
            {
                PartDefinition SidepanelL3 = SidepanelL2.addOrReplaceChild("SidepanelL3", CubeListBuilder.create().texOffs(0, 35).mirror(false).addBox(0.0F, 0.0F, -0.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.0F, 0.0F, -0.296706F));
                SidepanelL3.addOrReplaceChild("SidepanelL4", CubeListBuilder.create().texOffs(24, 35).mirror(false).addBox(0.0F, 0.0F, -0.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 3.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
            }
        }
        {
            PartDefinition LegpanelC1 = root.addOrReplaceChild("LegpanelC1", CubeListBuilder.create().texOffs(16, 45).mirror(false).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 5.5F, -3.0F, 0.0F, 0.0F, 0.0F));
            {
                PartDefinition LegpanelC2 = LegpanelC1.addOrReplaceChild("LegpanelC2", CubeListBuilder.create().texOffs(16, 54).mirror(false).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                LegpanelC2.addOrReplaceChild("LegpanelC3", CubeListBuilder.create().texOffs(32, 59).mirror(false).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            }
        }
        {
            PartDefinition Cloak1 = root.addOrReplaceChild("Cloak1", CubeListBuilder.create().texOffs(106, 0).mirror(false).addBox(0.0F, 0.0F, -0.5F, 10.0F, 18.0F, 1.0F), PartPose.offsetAndRotation(-5.0F, -6.0F, 4.0F, 0.0F, 0.0F, 0.0F));
            {
                PartDefinition Cloak2 = Cloak1.addOrReplaceChild("Cloak2", CubeListBuilder.create().texOffs(106, 19).mirror(false).addBox(0.0F, 0.0F, -0.5F, 10.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F));
                Cloak2.addOrReplaceChild("Cloak3", CubeListBuilder.create().texOffs(106, 24).mirror(false).addBox(0.0F, 0.0F, -0.5F, 10.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }
}
