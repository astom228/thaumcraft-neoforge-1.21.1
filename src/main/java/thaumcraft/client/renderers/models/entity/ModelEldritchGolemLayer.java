package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Геометрия ModelEldritchGolem (сгенерировано tools/models/convert-entity-models.js — не править вручную). Имена частей — поля оригинала. */
@SuppressWarnings("unused")
public final class ModelEldritchGolemLayer {
    private ModelEldritchGolemLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("Cloak1", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(-5.0F, 1.5F, 4.0F, 10.0F, 12.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1396263F, 0.0F, 0.0F));
        root.addOrReplaceChild("Cloak3", CubeListBuilder.create().texOffs(0, 37).mirror(false).addBox(-5.0F, 17.5F, -0.8F, 10.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.4465716F, 0.0F, 0.0F));
        root.addOrReplaceChild("Cloak2", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(-5.0F, 13.5F, 1.7F, 10.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.3069452F, 0.0F, 0.0F));
        root.addOrReplaceChild("CloakCL", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(3.0F, 0.5F, 2.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1396263F, 0.0F, 0.0F));
        root.addOrReplaceChild("CloakCR", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(-5.0F, 0.5F, 2.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1396263F, 0.0F, 0.0F));
        root.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(47, 12).mirror(false).addBox(-3.5F, -6.0F, -2.5F, 7.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 4.5F, -3.8F, -0.1047198F, 0.0F, 0.0F));
        root.addOrReplaceChild("Head2", CubeListBuilder.create().texOffs(26, 16).mirror(false).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -5.0F, -0.1047198F, 0.0F, 0.0F));
        root.addOrReplaceChild("CollarL", CubeListBuilder.create().texOffs(75, 50).mirror(false).addBox(3.5F, -0.5F, -7.0F, 1.0F, 4.0F, 10.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        root.addOrReplaceChild("CollarR", CubeListBuilder.create().texOffs(67, 50).mirror(false).addBox(-4.5F, -0.5F, -7.0F, 1.0F, 4.0F, 10.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        root.addOrReplaceChild("CollarB", CubeListBuilder.create().texOffs(77, 59).mirror(false).addBox(-3.5F, -0.5F, 2.0F, 7.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        root.addOrReplaceChild("CollarF", CubeListBuilder.create().texOffs(77, 59).mirror(false).addBox(-3.5F, -0.5F, -7.0F, 7.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        root.addOrReplaceChild("CollarBlack", CubeListBuilder.create().texOffs(22, 0).mirror(false).addBox(-3.5F, 0.0F, -6.0F, 7.0F, 1.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.837758F, 0.0F, 0.0F));
        root.addOrReplaceChild("Frontcloth0", CubeListBuilder.create().texOffs(114, 52).mirror(false).addBox(-3.0F, 3.2F, -3.5F, 6.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1745329F, 0.0F, 0.0F));
        root.addOrReplaceChild("Frontcloth1", CubeListBuilder.create().texOffs(114, 39).mirror(false).addBox(-1.0F, 1.5F, -3.5F, 6.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, -0.1047198F, 0.0F, 0.0F));
        root.addOrReplaceChild("Frontcloth2", CubeListBuilder.create().texOffs(114, 47).mirror(false).addBox(-1.0F, 8.5F, -1.5F, 6.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-2.0F, 11.0F, 0.0F, -0.3316126F, 0.0F, 0.0F));
        root.addOrReplaceChild("Torso", CubeListBuilder.create().texOffs(34, 45).mirror(false).addBox(-5.0F, 2.5F, -3.0F, 10.0F, 10.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.1745329F, 0.0F, 0.0F));
        {
            PartDefinition ArmR = root.addOrReplaceChild("ArmR", CubeListBuilder.create().texOffs(78, 32).mirror(false).addBox(-3.5F, 1.5F, -2.0F, 4.0F, 13.0F, 5.0F), PartPose.offsetAndRotation(-5.0F, 3.0F, -2.0F, 0.0F, 0.0F, 0.1047198F));
            ArmR.addOrReplaceChild("ShoulderR", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-4.3F, -1.0F, -3.0F, 4.0F, 5.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.186824F));
            ArmR.addOrReplaceChild("ShoulderR0", CubeListBuilder.create().texOffs(56, 31).mirror(false).addBox(-4.5F, -1.5F, -2.5F, 5.0F, 6.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            ArmR.addOrReplaceChild("ShoulderR1", CubeListBuilder.create().texOffs(0, 23).mirror(false).addBox(-3.3F, 4.0F, -2.5F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.186824F));
            ArmR.addOrReplaceChild("ShoulderR2", CubeListBuilder.create().texOffs(0, 12).mirror(false).addBox(-2.3F, 4.0F, -3.0F, 2.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.186824F));
        }
        {
            PartDefinition ArmL = root.addOrReplaceChild("ArmL", CubeListBuilder.create().texOffs(78, 32).mirror().addBox(-0.5F, 1.5F, -2.0F, 4.0F, 13.0F, 5.0F), PartPose.offsetAndRotation(5.0F, 3.0F, -2.0F, 0.0F, 0.0F, -0.1047198F));
            ArmL.addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(0.3F, -1.0F, -3.0F, 4.0F, 5.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.186824F));
            ArmL.addOrReplaceChild("ShoulderL0", CubeListBuilder.create().texOffs(56, 31).mirror().addBox(-0.5F, -1.5F, -2.5F, 5.0F, 6.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            ArmL.addOrReplaceChild("ShoulderL1", CubeListBuilder.create().texOffs(0, 23).mirror().addBox(2.3F, 4.0F, -2.5F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.186824F));
            ArmL.addOrReplaceChild("ShoulderL2", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(0.3F, 4.0F, -3.0F, 2.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.186824F));
        }
        root.addOrReplaceChild("BackpanelR1", CubeListBuilder.create().texOffs(96, 7).mirror(false).addBox(0.0F, 2.5F, -2.5F, 2.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        root.addOrReplaceChild("WaistR1", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-3.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        root.addOrReplaceChild("WaistR2", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        root.addOrReplaceChild("WaistR3", CubeListBuilder.create().texOffs(114, 5).mirror().addBox(-2.0F, 2.5F, -2.5F, 2.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(-2.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        root.addOrReplaceChild("LegR", CubeListBuilder.create().texOffs(79, 19).mirror(false).addBox(-2.5F, 2.5F, -2.0F, 4.0F, 9.0F, 4.0F), PartPose.offsetAndRotation(-2.0F, 12.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("WaistL1", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-2.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        root.addOrReplaceChild("WaistL2", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(2.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        root.addOrReplaceChild("WaistL3", CubeListBuilder.create().texOffs(114, 5).mirror(false).addBox(0.0F, 2.5F, -2.5F, 2.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        root.addOrReplaceChild("BackpanelL1", CubeListBuilder.create().texOffs(96, 7).mirror(false).addBox(-2.0F, 2.5F, -2.5F, 2.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(2.0F, 12.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        root.addOrReplaceChild("LegL", CubeListBuilder.create().texOffs(79, 19).mirror(false).addBox(-1.5F, 2.5F, -2.0F, 4.0F, 9.0F, 4.0F), PartPose.offsetAndRotation(2.0F, 12.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
