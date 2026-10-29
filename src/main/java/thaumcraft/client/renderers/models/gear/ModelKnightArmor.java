package thaumcraft.client.renderers.models.gear;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * Порт ModelKnightArmor (сгенерировано tools/models/convert-armor-models.js — не править вручную). Части — как в оригинале;
 * legs = true — вариант для поножей (ModelBiped(0.5F)), иначе — для шлема, нагрудника и ботинок (1.0F).
 * Части, которых нет в варианте, — null. Анимацию и скрытие частей задаёт код предмета (render оригинала).
 */
public class ModelKnightArmor extends HumanoidModel<LivingEntity> {
    public final ModelPart Helmet;
    public final ModelPart Mbelt;
    public final ModelPart MbeltL;
    public final ModelPart MbeltR;
    public final ModelPart Tabbard;
    public final ModelPart CloakAtL;
    public final ModelPart Backplate;
    public final ModelPart Cloak1;
    public final ModelPart Cloak2;
    public final ModelPart Cloak3;
    public final ModelPart CloakAtR;
    public final ModelPart Chestplate;
    public final ModelPart ShoulderR1;
    public final ModelPart GauntletR;
    public final ModelPart GauntletstrapR1;
    public final ModelPart GauntletstrapR2;
    public final ModelPart ShoulderR;
    public final ModelPart ShoulderR0;
    public final ModelPart ShoulderR2;
    public final ModelPart ShoulderL1;
    public final ModelPart GauntletL;
    public final ModelPart GauntletstrapL1;
    public final ModelPart GauntletstrapL2;
    public final ModelPart ShoulderL;
    public final ModelPart ShoulderL0;
    public final ModelPart ShoulderL2;
    public final ModelPart SidepanelR3;
    public final ModelPart SidepanelR2;
    public final ModelPart SidepanelL2;
    public final ModelPart SidepanelR0;
    public final ModelPart SidepanelL0;
    public final ModelPart SidepanelR1;
    public final ModelPart SidepanelL3;
    public final ModelPart SidepanelL1;
    public final ModelPart Frontcloth1;
    public final ModelPart Frontcloth2;

    public ModelKnightArmor(ModelPart root) {
        super(root);
        this.Helmet = find(root, "head", "Helmet");
        this.Mbelt = find(root, "body", "Mbelt");
        this.MbeltL = find(root, "body", "MbeltL");
        this.MbeltR = find(root, "body", "MbeltR");
        this.Tabbard = find(root, "body", "Tabbard");
        this.CloakAtL = find(root, "body", "CloakAtL");
        this.Backplate = find(root, "body", "Backplate");
        this.Cloak1 = find(root, "body", "Cloak1");
        this.Cloak2 = find(root, "body", "Cloak2");
        this.Cloak3 = find(root, "body", "Cloak3");
        this.CloakAtR = find(root, "body", "CloakAtR");
        this.Chestplate = find(root, "body", "Chestplate");
        this.ShoulderR1 = find(root, "right_arm", "ShoulderR1");
        this.GauntletR = find(root, "right_arm", "GauntletR");
        this.GauntletstrapR1 = find(root, "right_arm", "GauntletstrapR1");
        this.GauntletstrapR2 = find(root, "right_arm", "GauntletstrapR2");
        this.ShoulderR = find(root, "right_arm", "ShoulderR");
        this.ShoulderR0 = find(root, "right_arm", "ShoulderR0");
        this.ShoulderR2 = find(root, "right_arm", "ShoulderR2");
        this.ShoulderL1 = find(root, "left_arm", "ShoulderL1");
        this.GauntletL = find(root, "left_arm", "GauntletL");
        this.GauntletstrapL1 = find(root, "left_arm", "GauntletstrapL1");
        this.GauntletstrapL2 = find(root, "left_arm", "GauntletstrapL2");
        this.ShoulderL = find(root, "left_arm", "ShoulderL");
        this.ShoulderL0 = find(root, "left_arm", "ShoulderL0");
        this.ShoulderL2 = find(root, "left_arm", "ShoulderL2");
        this.SidepanelR3 = find(root, "right_leg", "SidepanelR3");
        this.SidepanelR2 = find(root, "right_leg", "SidepanelR2");
        this.SidepanelL2 = find(root, "left_leg", "SidepanelL2");
        this.SidepanelR0 = find(root, "right_leg", "SidepanelR0");
        this.SidepanelL0 = find(root, "left_leg", "SidepanelL0");
        this.SidepanelR1 = find(root, "right_leg", "SidepanelR1");
        this.SidepanelL3 = find(root, "left_leg", "SidepanelL3");
        this.SidepanelL1 = find(root, "left_leg", "SidepanelL1");
        this.Frontcloth1 = find(root, "body", "Frontcloth1");
        this.Frontcloth2 = find(root, "body", "Frontcloth2");
    }

    private static ModelPart find(ModelPart root, String... path) {
        ModelPart p = root;
        for (String n : path) {
            if (!p.hasChild(n)) {
                return null;
            }
            p = p.getChild(n);
        }
        return p;
    }

    public static LayerDefinition createLayer(boolean legs) {
        float f = legs ? 0.5F : 1.0F;
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition right_arm = root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        PartDefinition left_arm = root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        head.addOrReplaceChild("Helmet", CubeListBuilder.create().texOffs(41, 8).mirror(false).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 9.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("ShoulderR", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-3.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("ShoulderR0", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-4.3F, -1.5F, -3.0F, 3.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        right_arm.addOrReplaceChild("ShoulderR1", CubeListBuilder.create().texOffs(0, 19).mirror(false).addBox(-3.3F, 3.5F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        right_arm.addOrReplaceChild("ShoulderR2", CubeListBuilder.create().texOffs(0, 11).mirror(false).addBox(-2.3F, 3.5F, -3.0F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        right_arm.addOrReplaceChild("GauntletR", CubeListBuilder.create().texOffs(100, 26).mirror(false).addBox(-3.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("GauntletstrapR1", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("GauntletstrapR2", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("ShoulderL0", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(1.3F, -1.5F, -3.0F, 3.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        left_arm.addOrReplaceChild("ShoulderL1", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(2.3F, 3.5F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        left_arm.addOrReplaceChild("ShoulderL2", CubeListBuilder.create().texOffs(0, 11).mirror(false).addBox(1.3F, 3.5F, -3.0F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        left_arm.addOrReplaceChild("GauntletL", CubeListBuilder.create().texOffs(114, 26).mirror(false).addBox(1.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("GauntletstrapL1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("GauntletstrapL2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("SidepanelR0", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-3.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        right_leg.addOrReplaceChild("SidepanelR1", CubeListBuilder.create().texOffs(96, 7).mirror().addBox(0.0F, 2.5F, -2.5F, 2.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        right_leg.addOrReplaceChild("SidepanelR2", CubeListBuilder.create().texOffs(114, 5).mirror().addBox(-2.0F, 2.5F, -2.5F, 2.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        right_leg.addOrReplaceChild("SidepanelR3", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        left_leg.addOrReplaceChild("SidepanelL0", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-2.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        left_leg.addOrReplaceChild("SidepanelL1", CubeListBuilder.create().texOffs(96, 7).mirror(false).addBox(-2.0F, 2.5F, -2.5F, 2.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        left_leg.addOrReplaceChild("SidepanelL2", CubeListBuilder.create().texOffs(114, 5).mirror(false).addBox(0.0F, 2.5F, -2.5F, 2.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        left_leg.addOrReplaceChild("SidepanelL3", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(2.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        if (legs) {
            body.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(56, 55).mirror(false).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        } else {
            body.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(56, 55).mirror(false).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Chestplate", CubeListBuilder.create().texOffs(56, 45).mirror(false).addBox(-4.0F, 1.0F, -3.0F, 8.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Frontcloth1", CubeListBuilder.create().texOffs(120, 39).mirror(false).addBox(0.0F, 0.0F, 0.0F, 6.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 11.0F, -3.5F, -0.1047198F, 0.0F, 0.0F));
            body.addOrReplaceChild("Frontcloth2", CubeListBuilder.create().texOffs(100, 37).mirror(false).addBox(0.0F, 7.5F, 1.8F, 6.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 11.0F, -3.5F, -0.3316126F, 0.0F, 0.0F));
            body.addOrReplaceChild("Tabbard", CubeListBuilder.create().texOffs(114, 52).mirror(false).addBox(-3.0F, 1.2F, -3.5F, 6.0F, 10.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Backplate", CubeListBuilder.create().texOffs(36, 45).mirror(false).addBox(-4.0F, 1.0F, 2.0F, 8.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Cloak1", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(0.0F, 0.0F, 0.0F, 9.0F, 12.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, 0.1396263F, 0.0F, 0.0F));
            body.addOrReplaceChild("Cloak2", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(0.0F, 11.7F, -2.0F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, 0.3069452F, 0.0F, 0.0F));
            body.addOrReplaceChild("Cloak3", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(0.0F, 15.2F, -4.2F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 1.3F, 4.2F, 0.4465716F, 0.0F, 0.0F));
            body.addOrReplaceChild("CloakAtL", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(2.5F, 1.0F, 2.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1396263F, 0.0F, 0.0F));
            body.addOrReplaceChild("CloakAtR", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(-4.5F, 1.0F, 2.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1396263F, 0.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }
}
