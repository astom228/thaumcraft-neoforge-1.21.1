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
 * Порт ModelLeaderArmor (сгенерировано tools/models/convert-armor-models.js — не править вручную). Части — как в оригинале;
 * legs = true — вариант для поножей (ModelBiped(0.5F)), иначе — для шлема, нагрудника и ботинок (1.0F).
 * Части, которых нет в варианте, — null. Анимацию и скрытие частей задаёт код предмета (render оригинала).
 */
public class ModelLeaderArmor extends HumanoidModel<LivingEntity> {
    public final ModelPart Helmet;
    public final ModelPart CollarF;
    public final ModelPart CollarB;
    public final ModelPart CollarR;
    public final ModelPart CollarL;
    public final ModelPart BeltR;
    public final ModelPart Mbelt;
    public final ModelPart MbeltL;
    public final ModelPart MbeltR;
    public final ModelPart BeltL;
    public final ModelPart CloakTL;
    public final ModelPart Cloak3;
    public final ModelPart CloakTR;
    public final ModelPart Cloak1;
    public final ModelPart Cloak2;
    public final ModelPart Chestplate;
    public final ModelPart ChestOrnament;
    public final ModelPart ChestClothL;
    public final ModelPart ChestClothR;
    public final ModelPart Backplate;
    public final ModelPart GauntletR;
    public final ModelPart GauntletL;
    public final ModelPart GauntletstrapL1;
    public final ModelPart GauntletstrapL2;
    public final ModelPart GauntletstrapR1;
    public final ModelPart GauntletstrapR2;
    public final ModelPart GauntletR2;
    public final ModelPart GauntletL2;
    public final ModelPart ShoulderR;
    public final ModelPart ShoulderR1;
    public final ModelPart ShoulderR2;
    public final ModelPart ShoulderR5;
    public final ModelPart ShoulderR3;
    public final ModelPart ShoulderR4;
    public final ModelPart ShoulderL;
    public final ModelPart ShoulderL1;
    public final ModelPart ShoulderL2;
    public final ModelPart ShoulderL3;
    public final ModelPart ShoulderL5;
    public final ModelPart ShoulderL4;
    public final ModelPart LegClothR;
    public final ModelPart LegClothL;
    public final ModelPart BackpanelR1;
    public final ModelPart BackpanelR2;
    public final ModelPart BackpanelR3;
    public final ModelPart BackpanelR4;
    public final ModelPart BackpanelL1;
    public final ModelPart BackpanelL4;
    public final ModelPart BackpanelL2;
    public final ModelPart BackpanelL3;

    public ModelLeaderArmor(ModelPart root) {
        super(root);
        this.Helmet = find(root, "head", "Helmet");
        this.CollarF = find(root, "body", "CollarF");
        this.CollarB = find(root, "body", "CollarB");
        this.CollarR = find(root, "body", "CollarR");
        this.CollarL = find(root, "body", "CollarL");
        this.BeltR = find(root, "body", "BeltR");
        this.Mbelt = find(root, "body", "Mbelt");
        this.MbeltL = find(root, "body", "MbeltL");
        this.MbeltR = find(root, "body", "MbeltR");
        this.BeltL = find(root, "body", "BeltL");
        this.CloakTL = find(root, "body", "CloakTL");
        this.Cloak3 = find(root, "body", "Cloak3");
        this.CloakTR = find(root, "body", "CloakTR");
        this.Cloak1 = find(root, "body", "Cloak1");
        this.Cloak2 = find(root, "body", "Cloak2");
        this.Chestplate = find(root, "body", "Chestplate");
        this.ChestOrnament = find(root, "body", "ChestOrnament");
        this.ChestClothL = find(root, "body", "ChestClothL");
        this.ChestClothR = find(root, "body", "ChestClothR");
        this.Backplate = find(root, "body", "Backplate");
        this.GauntletR = find(root, "right_arm", "GauntletR");
        this.GauntletL = find(root, "left_arm", "GauntletL");
        this.GauntletstrapL1 = find(root, "left_arm", "GauntletstrapL1");
        this.GauntletstrapL2 = find(root, "left_arm", "GauntletstrapL2");
        this.GauntletstrapR1 = find(root, "right_arm", "GauntletstrapR1");
        this.GauntletstrapR2 = find(root, "right_arm", "GauntletstrapR2");
        this.GauntletR2 = find(root, "right_arm", "GauntletR2");
        this.GauntletL2 = find(root, "left_arm", "GauntletL2");
        this.ShoulderR = find(root, "right_arm", "ShoulderR");
        this.ShoulderR1 = find(root, "right_arm", "ShoulderR1");
        this.ShoulderR2 = find(root, "right_arm", "ShoulderR2");
        this.ShoulderR5 = find(root, "right_arm", "ShoulderR5");
        this.ShoulderR3 = find(root, "right_arm", "ShoulderR3");
        this.ShoulderR4 = find(root, "right_arm", "ShoulderR4");
        this.ShoulderL = find(root, "left_arm", "ShoulderL");
        this.ShoulderL1 = find(root, "left_arm", "ShoulderL1");
        this.ShoulderL2 = find(root, "left_arm", "ShoulderL2");
        this.ShoulderL3 = find(root, "left_arm", "ShoulderL3");
        this.ShoulderL5 = find(root, "left_arm", "ShoulderL5");
        this.ShoulderL4 = find(root, "left_arm", "ShoulderL4");
        this.LegClothR = find(root, "body", "LegClothR");
        this.LegClothL = find(root, "body", "LegClothL");
        this.BackpanelR1 = find(root, "right_leg", "BackpanelR1");
        this.BackpanelR2 = find(root, "right_leg", "BackpanelR2");
        this.BackpanelR3 = find(root, "right_leg", "BackpanelR3");
        this.BackpanelR4 = find(root, "right_leg", "BackpanelR4");
        this.BackpanelL1 = find(root, "left_leg", "BackpanelL1");
        this.BackpanelL4 = find(root, "left_leg", "BackpanelL4");
        this.BackpanelL2 = find(root, "left_leg", "BackpanelL2");
        this.BackpanelL3 = find(root, "left_leg", "BackpanelL3");
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
        right_arm.addOrReplaceChild("ShoulderR1", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-4.3F, -1.5F, -3.0F, 3.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        right_arm.addOrReplaceChild("ShoulderR2", CubeListBuilder.create().texOffs(0, 19).mirror(false).addBox(-3.3F, 3.5F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        right_arm.addOrReplaceChild("ShoulderR3", CubeListBuilder.create().texOffs(0, 11).mirror(false).addBox(-2.3F, 3.5F, -3.0F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        right_arm.addOrReplaceChild("ShoulderR4", CubeListBuilder.create().texOffs(18, 4).mirror(false).addBox(-2.3F, -1.5F, -4.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        right_arm.addOrReplaceChild("ShoulderR5", CubeListBuilder.create().texOffs(18, 4).mirror(false).addBox(-2.3F, -1.5F, 3.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7853982F));
        right_arm.addOrReplaceChild("GauntletR", CubeListBuilder.create().texOffs(100, 26).mirror(false).addBox(-3.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("GauntletR2", CubeListBuilder.create().texOffs(102, 37).mirror(false).addBox(-5.0F, 3.5F, -2.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1675516F));
        right_arm.addOrReplaceChild("GauntletstrapR1", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("GauntletstrapR2", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("ShoulderL1", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(1.3F, -1.5F, -3.0F, 3.0F, 5.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        left_arm.addOrReplaceChild("ShoulderL2", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(2.3F, 3.5F, -2.5F, 1.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        left_arm.addOrReplaceChild("ShoulderL3", CubeListBuilder.create().texOffs(0, 11).mirror(false).addBox(1.3F, 3.5F, -3.0F, 1.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        left_arm.addOrReplaceChild("ShoulderL4", CubeListBuilder.create().texOffs(18, 4).mirror(false).addBox(1.3F, -1.5F, -4.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        left_arm.addOrReplaceChild("ShoulderL5", CubeListBuilder.create().texOffs(18, 4).mirror(false).addBox(1.3F, -1.5F, 3.0F, 1.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.7853982F));
        left_arm.addOrReplaceChild("GauntletL", CubeListBuilder.create().texOffs(114, 26).mirror(false).addBox(1.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("GauntletL2", CubeListBuilder.create().texOffs(102, 37).mirror(false).addBox(4.0F, 3.5F, -2.0F, 1.0F, 5.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1675516F));
        left_arm.addOrReplaceChild("GauntletstrapL1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("GauntletstrapL2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("BackpanelR1", CubeListBuilder.create().texOffs(0, 25).mirror(false).addBox(-3.0F, -0.5F, 2.5F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0698132F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("BackpanelR2", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-3.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        right_leg.addOrReplaceChild("BackpanelR3", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F));
        right_leg.addOrReplaceChild("BackpanelR4", CubeListBuilder.create().texOffs(0, 25).mirror().addBox(-3.0F, -0.5F, -3.5F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0349066F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("BackpanelL1", CubeListBuilder.create().texOffs(0, 25).mirror(false).addBox(-2.0F, -0.5F, 2.5F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0698132F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("BackpanelL2", CubeListBuilder.create().texOffs(96, 14).mirror(false).addBox(-2.0F, -0.5F, -2.5F, 5.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        left_leg.addOrReplaceChild("BackpanelL3", CubeListBuilder.create().texOffs(116, 13).mirror(false).addBox(2.0F, 2.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F));
        left_leg.addOrReplaceChild("BackpanelL4", CubeListBuilder.create().texOffs(0, 25).mirror(false).addBox(-2.0F, -0.5F, -3.5F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.0349066F, 0.0F, 0.0F));
        if (legs) {
            body.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(56, 55).mirror(false).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        } else {
            body.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(56, 55).mirror(false).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("BeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 4.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("BeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 4.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Chestplate", CubeListBuilder.create().texOffs(56, 45).mirror(false).addBox(-4.0F, 1.0F, -3.8F, 8.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("ChestOrnament", CubeListBuilder.create().texOffs(76, 53).mirror(false).addBox(-2.5F, 3.0F, -4.8F, 5.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("ChestClothR", CubeListBuilder.create().texOffs(20, 47).mirror(false).addBox(-4.5F, 1.2F, -4.5F, 3.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0663225F, 0.0F, 0.0F));
            body.addOrReplaceChild("ChestClothL", CubeListBuilder.create().texOffs(20, 47).mirror().addBox(1.5F, 1.2F, -4.5F, 3.0F, 9.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0663225F, 0.0F, 0.0F));
            body.addOrReplaceChild("LegClothR", CubeListBuilder.create().texOffs(20, 55).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-4.5F, 10.4F, -3.9F, -0.0349066F, 0.0F, 0.0F));
            body.addOrReplaceChild("LegClothL", CubeListBuilder.create().texOffs(20, 55).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(1.5F, 10.4F, -3.9F, -0.0349066F, 0.0F, 0.0F));
            body.addOrReplaceChild("Backplate", CubeListBuilder.create().texOffs(36, 45).mirror(false).addBox(-4.0F, 1.0F, 2.0F, 8.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("CollarB", CubeListBuilder.create().texOffs(17, 26).mirror(false).addBox(-4.5F, -1.5F, 7.0F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.2268928F, 0.0F, 0.0F));
            body.addOrReplaceChild("CollarR", CubeListBuilder.create().texOffs(17, 11).mirror(false).addBox(-5.5F, -1.5F, -3.0F, 1.0F, 4.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.2268928F, 0.0F, 0.0F));
            body.addOrReplaceChild("CollarL", CubeListBuilder.create().texOffs(17, 11).mirror(false).addBox(4.5F, -1.5F, -3.0F, 1.0F, 4.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.2268928F, 0.0F, 0.0F));
            body.addOrReplaceChild("CollarF", CubeListBuilder.create().texOffs(17, 31).mirror(false).addBox(-4.5F, -1.5F, -3.0F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, -2.5F, 0.2268928F, 0.0F, 0.0F));
            body.addOrReplaceChild("Cloak1", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(-4.5F, 2.0F, 1.0F, 9.0F, 12.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.1396263F, 0.0F, 0.0F));
            body.addOrReplaceChild("Cloak2", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(-4.5F, 14.0F, -1.3F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.3069452F, 0.0F, 0.0F));
            body.addOrReplaceChild("Cloak3", CubeListBuilder.create().texOffs(0, 59).mirror(false).addBox(-4.5F, 17.0F, -3.7F, 9.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.4465716F, 0.0F, 0.0F));
            body.addOrReplaceChild("CloakTL", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(2.5F, 1.0F, -1.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.1396263F, 0.0F, 0.0F));
            body.addOrReplaceChild("CloakTR", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(-4.5F, 1.0F, -1.0F, 2.0F, 1.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, 0.1396263F, 0.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }
}
