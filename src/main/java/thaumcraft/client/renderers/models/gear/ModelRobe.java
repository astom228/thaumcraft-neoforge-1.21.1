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
 * Порт ModelRobe (сгенерировано tools/models/convert-armor-models.js — не править вручную). Части — как в оригинале;
 * legs = true — вариант для поножей (ModelBiped(0.5F)), иначе — для шлема, нагрудника и ботинок (1.0F).
 * Части, которых нет в варианте, — null. Анимацию и скрытие частей задаёт код предмета (render оригинала).
 */
public class ModelRobe extends HumanoidModel<LivingEntity> {
    public final ModelPart Hood1;
    public final ModelPart Hood2;
    public final ModelPart Hood3;
    public final ModelPart Hood4;
    public final ModelPart Chestthing;
    public final ModelPart Mbelt;
    public final ModelPart MbeltB;
    public final ModelPart ClothchestL;
    public final ModelPart ClothchestR;
    public final ModelPart Book;
    public final ModelPart Scroll;
    public final ModelPart Backplate;
    public final ModelPart MbeltL;
    public final ModelPart MbeltR;
    public final ModelPart Chestplate;
    public final ModelPart ShoulderplateR1;
    public final ModelPart ShoulderplateR2;
    public final ModelPart ShoulderplateR3;
    public final ModelPart ShoulderplateTopR;
    public final ModelPart RArm1;
    public final ModelPart RArm2;
    public final ModelPart RArm3;
    public final ModelPart ShoulderplateL1;
    public final ModelPart ShoulderplateL2;
    public final ModelPart ShoulderplateL3;
    public final ModelPart ShoulderplateTopL;
    public final ModelPart ShoulderR;
    public final ModelPart LArm1;
    public final ModelPart LArm2;
    public final ModelPart LArm3;
    public final ModelPart ShoulderL;
    public final ModelPart FrontclothR1;
    public final ModelPart FrontclothR2;
    public final ModelPart FrontclothL1;
    public final ModelPart FrontclothL2;
    public final ModelPart ClothBackR1;
    public final ModelPart ClothBackR2;
    public final ModelPart ClothBackR3;
    public final ModelPart ClothBackL1;
    public final ModelPart ClothBackL2;
    public final ModelPart ClothBackL3;
    public final ModelPart SideclothL2;
    public final ModelPart SideclothR1;
    public final ModelPart SideclothR2;
    public final ModelPart SideclothR3;
    public final ModelPart SidepanelR1;
    public final ModelPart LegpanelR6;
    public final ModelPart LegpanelR5;
    public final ModelPart LegpanelR4;
    public final ModelPart SideclothL3;
    public final ModelPart Focipouch;
    public final ModelPart SideclothL1;
    public final ModelPart LegpanelL4;
    public final ModelPart LegpanelL5;
    public final ModelPart LegpanelL6;
    public final ModelPart SidepanelL1;

    public ModelRobe(ModelPart root) {
        super(root);
        this.Hood1 = find(root, "head", "Hood1");
        this.Hood2 = find(root, "head", "Hood2");
        this.Hood3 = find(root, "head", "Hood3");
        this.Hood4 = find(root, "head", "Hood4");
        this.Chestthing = find(root, "body", "Chestthing");
        this.Mbelt = find(root, "body", "Mbelt");
        this.MbeltB = find(root, "body", "MbeltB");
        this.ClothchestL = find(root, "body", "ClothchestL");
        this.ClothchestR = find(root, "body", "ClothchestR");
        this.Book = find(root, "body", "Book");
        this.Scroll = find(root, "body", "Scroll");
        this.Backplate = find(root, "body", "Backplate");
        this.MbeltL = find(root, "body", "MbeltL");
        this.MbeltR = find(root, "body", "MbeltR");
        this.Chestplate = find(root, "body", "Chestplate");
        this.ShoulderplateR1 = find(root, "right_arm", "ShoulderplateR1");
        this.ShoulderplateR2 = find(root, "right_arm", "ShoulderplateR2");
        this.ShoulderplateR3 = find(root, "right_arm", "ShoulderplateR3");
        this.ShoulderplateTopR = find(root, "right_arm", "ShoulderplateTopR");
        this.RArm1 = find(root, "right_arm", "RArm1");
        this.RArm2 = find(root, "right_arm", "RArm2");
        this.RArm3 = find(root, "right_arm", "RArm3");
        this.ShoulderplateL1 = find(root, "left_arm", "ShoulderplateL1");
        this.ShoulderplateL2 = find(root, "left_arm", "ShoulderplateL2");
        this.ShoulderplateL3 = find(root, "left_arm", "ShoulderplateL3");
        this.ShoulderplateTopL = find(root, "left_arm", "ShoulderplateTopL");
        this.ShoulderR = find(root, "right_arm", "ShoulderR");
        this.LArm1 = find(root, "left_arm", "LArm1");
        this.LArm2 = find(root, "left_arm", "LArm2");
        this.LArm3 = find(root, "left_arm", "LArm3");
        this.ShoulderL = find(root, "left_arm", "ShoulderL");
        this.FrontclothR1 = find(root, "body", "FrontclothR1");
        this.FrontclothR2 = find(root, "body", "FrontclothR2");
        this.FrontclothL1 = find(root, "body", "FrontclothL1");
        this.FrontclothL2 = find(root, "body", "FrontclothL2");
        this.ClothBackR1 = find(root, "body", "ClothBackR1");
        this.ClothBackR2 = find(root, "body", "ClothBackR2");
        this.ClothBackR3 = find(root, "body", "ClothBackR3");
        this.ClothBackL1 = find(root, "body", "ClothBackL1");
        this.ClothBackL2 = find(root, "body", "ClothBackL2");
        this.ClothBackL3 = find(root, "body", "ClothBackL3");
        this.SideclothL2 = find(root, "left_leg", "SideclothL2");
        this.SideclothR1 = find(root, "right_leg", "SideclothR1");
        this.SideclothR2 = find(root, "right_leg", "SideclothR2");
        this.SideclothR3 = find(root, "right_leg", "SideclothR3");
        this.SidepanelR1 = find(root, "right_leg", "SidepanelR1");
        this.LegpanelR6 = find(root, "right_leg", "LegpanelR6");
        this.LegpanelR5 = find(root, "right_leg", "LegpanelR5");
        this.LegpanelR4 = find(root, "right_leg", "LegpanelR4");
        this.SideclothL3 = find(root, "left_leg", "SideclothL3");
        this.Focipouch = find(root, "left_leg", "Focipouch");
        this.SideclothL1 = find(root, "left_leg", "SideclothL1");
        this.LegpanelL4 = find(root, "left_leg", "LegpanelL4");
        this.LegpanelL5 = find(root, "left_leg", "LegpanelL5");
        this.LegpanelL6 = find(root, "left_leg", "LegpanelL6");
        this.SidepanelL1 = find(root, "left_leg", "SidepanelL1");
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
        head.addOrReplaceChild("Hood1", CubeListBuilder.create().texOffs(16, 7).mirror(false).addBox(-4.5F, -9.0F, -4.6F, 9.0F, 9.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("Hood2", CubeListBuilder.create().texOffs(52, 13).mirror(false).addBox(-4.0F, -9.7F, 2.0F, 8.0F, 9.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.2268928F, 0.0F, 0.0F));
        head.addOrReplaceChild("Hood3", CubeListBuilder.create().texOffs(52, 14).mirror(false).addBox(-3.5F, -10.0F, 3.5F, 7.0F, 8.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.3490659F, 0.0F, 0.0F));
        head.addOrReplaceChild("Hood4", CubeListBuilder.create().texOffs(53, 15).mirror(false).addBox(-3.0F, -10.7F, 3.5F, 6.0F, 7.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5759587F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("ShoulderR", CubeListBuilder.create().texOffs(16, 45).mirror().addBox(-3.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("RArm1", CubeListBuilder.create().texOffs(88, 39).mirror(false).addBox(-3.5F, 2.5F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("RArm2", CubeListBuilder.create().texOffs(76, 32).mirror(false).addBox(-3.0F, 5.5F, 2.5F, 4.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("RArm3", CubeListBuilder.create().texOffs(88, 32).mirror(false).addBox(-2.5F, 3.5F, 2.5F, 3.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("ShoulderplateTopR", CubeListBuilder.create().texOffs(56, 25).mirror(false).addBox(-5.5F, -2.5F, -3.5F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_arm.addOrReplaceChild("ShoulderplateR1", CubeListBuilder.create().texOffs(56, 33).mirror(false).addBox(-4.5F, -1.5F, -3.5F, 1.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_arm.addOrReplaceChild("ShoulderplateR2", CubeListBuilder.create().texOffs(40, 33).mirror(false).addBox(-3.5F, 1.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_arm.addOrReplaceChild("ShoulderplateR3", CubeListBuilder.create().texOffs(40, 33).mirror(false).addBox(-2.5F, 3.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        left_arm.addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(16, 45).mirror(false).addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("LArm1", CubeListBuilder.create().texOffs(88, 39).mirror().addBox(-1.5F, 2.5F, -2.5F, 5.0F, 7.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("LArm2", CubeListBuilder.create().texOffs(76, 32).mirror(false).addBox(-1.0F, 5.5F, 2.5F, 4.0F, 4.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("LArm3", CubeListBuilder.create().texOffs(88, 32).mirror(false).addBox(-0.5F, 3.5F, 2.5F, 3.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("ShoulderplateTopL", CubeListBuilder.create().texOffs(56, 25).mirror(false).addBox(3.5F, -2.5F, -3.5F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_arm.addOrReplaceChild("ShoulderplateL1", CubeListBuilder.create().texOffs(56, 33).mirror(false).addBox(3.5F, -1.5F, -3.5F, 1.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_arm.addOrReplaceChild("ShoulderplateL2", CubeListBuilder.create().texOffs(40, 33).mirror(false).addBox(2.5F, 1.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_arm.addOrReplaceChild("ShoulderplateL3", CubeListBuilder.create().texOffs(40, 33).mirror(false).addBox(1.5F, 3.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        right_leg.addOrReplaceChild("LegpanelR4", CubeListBuilder.create().texOffs(76, 38).mirror(false).addBox(-3.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("LegpanelR5", CubeListBuilder.create().texOffs(76, 42).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("LegpanelR6", CubeListBuilder.create().texOffs(82, 38).mirror(false).addBox(-3.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("SidepanelR1", CubeListBuilder.create().texOffs(116, 25).mirror(false).addBox(-2.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_leg.addOrReplaceChild("SideclothR1", CubeListBuilder.create().texOffs(116, 42).mirror(false).addBox(-2.5F, 0.5F, -2.5F, 1.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.122173F));
        right_leg.addOrReplaceChild("SideclothR2", CubeListBuilder.create().texOffs(116, 34).mirror(false).addBox(-1.5F, 5.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.296706F));
        right_leg.addOrReplaceChild("SideclothR3", CubeListBuilder.create().texOffs(116, 1).mirror(false).addBox(0.4F, 8.4F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        if (legs) {
            body.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(16, 55).mirror(false).addBox(-4.0F, 7.0F, -3.0F, 8.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltB", CubeListBuilder.create().texOffs(16, 55).mirror(false).addBox(-4.0F, 7.0F, -4.0F, 8.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.141593F, 0.0F));
            body.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(16, 36).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(16, 36).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("FrontclothR1", CubeListBuilder.create().texOffs(108, 38).mirror(false).addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 11.0F, -2.9F, -0.1047198F, 0.0F, 0.0F));
            body.addOrReplaceChild("FrontclothR2", CubeListBuilder.create().texOffs(108, 47).mirror(false).addBox(0.0F, 7.5F, 1.7F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-3.0F, 11.0F, -2.9F, -0.3316126F, 0.0F, 0.0F));
            body.addOrReplaceChild("FrontclothL1", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(0.0F, 0.0F, 0.0F, 3.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.0F, -2.9F, -0.1047198F, 0.0F, 0.0F));
            body.addOrReplaceChild("FrontclothL2", CubeListBuilder.create().texOffs(108, 47).mirror().addBox(0.0F, 7.5F, 1.7F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.0F, -2.9F, -0.3316126F, 0.0F, 0.0F));
            body.addOrReplaceChild("ClothBackR1", CubeListBuilder.create().texOffs(118, 16).mirror().addBox(0.0F, 0.0F, 0.0F, 4.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.1047198F, 0.0F, 0.0F));
            body.addOrReplaceChild("ClothBackR2", CubeListBuilder.create().texOffs(123, 9).mirror(false).addBox(0.0F, 7.8F, -0.9F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.2268928F, 0.0F, 0.0F));
            body.addOrReplaceChild("ClothBackR3", CubeListBuilder.create().texOffs(120, 12).mirror().addBox(1.0F, 7.8F, -0.9F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(-4.0F, 11.5F, 2.9F, 0.2268928F, 0.0F, 0.0F));
            body.addOrReplaceChild("ClothBackL1", CubeListBuilder.create().texOffs(118, 16).mirror(false).addBox(0.0F, 0.0F, 0.0F, 4.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.1047198F, 0.0F, 0.0F));
            body.addOrReplaceChild("ClothBackL2", CubeListBuilder.create().texOffs(123, 9).mirror().addBox(3.0F, 7.8F, -0.9F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.2268928F, 0.0F, 0.0F));
            body.addOrReplaceChild("ClothBackL3", CubeListBuilder.create().texOffs(120, 12).mirror(false).addBox(0.0F, 7.8F, -0.9F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 11.5F, 2.9F, 0.2268928F, 0.0F, 0.0F));
            left_leg.addOrReplaceChild("Focipouch", CubeListBuilder.create().texOffs(100, 20).mirror(false).addBox(3.5F, 0.5F, -2.5F, 3.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.122173F));
            left_leg.addOrReplaceChild("LegpanelL4", CubeListBuilder.create().texOffs(76, 38).mirror().addBox(1.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
            left_leg.addOrReplaceChild("LegpanelL5", CubeListBuilder.create().texOffs(76, 42).mirror().addBox(1.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
            left_leg.addOrReplaceChild("LegpanelL6", CubeListBuilder.create().texOffs(82, 38).mirror().addBox(1.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
            left_leg.addOrReplaceChild("SidepanelL1", CubeListBuilder.create().texOffs(116, 25).mirror(false).addBox(1.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
            left_leg.addOrReplaceChild("SideclothL1", CubeListBuilder.create().texOffs(116, 42).mirror(false).addBox(1.5F, 0.5F, -2.5F, 1.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.122173F));
            left_leg.addOrReplaceChild("SideclothL2", CubeListBuilder.create().texOffs(116, 34).mirror(false).addBox(0.5F, 5.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.296706F));
            left_leg.addOrReplaceChild("SideclothL3", CubeListBuilder.create().texOffs(116, 1).mirror(false).addBox(-1.4F, 8.4F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        } else {
            body.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(16, 55).mirror(false).addBox(-4.0F, 7.0F, -3.0F, 8.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltB", CubeListBuilder.create().texOffs(16, 55).mirror(false).addBox(-4.0F, 7.0F, -4.0F, 8.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.141593F, 0.0F));
            body.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(16, 36).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(16, 36).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Chestplate", CubeListBuilder.create().texOffs(16, 25).mirror(false).addBox(-4.0F, 1.0F, -3.0F, 8.0F, 6.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Chestthing", CubeListBuilder.create().texOffs(56, 50).mirror(false).addBox(-2.5F, 1.0F, -4.0F, 5.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Scroll", CubeListBuilder.create().texOffs(78, 25).mirror(false).addBox(-2.0F, 9.5F, 4.0F, 8.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1919862F));
            body.addOrReplaceChild("Backplate", CubeListBuilder.create().texOffs(36, 45).mirror(false).addBox(-4.0F, 1.0F, 1.9F, 8.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Book", CubeListBuilder.create().texOffs(81, 16).mirror(false).addBox(1.0F, 0.0F, 4.0F, 5.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7679449F));
            body.addOrReplaceChild("ClothchestL", CubeListBuilder.create().texOffs(108, 38).mirror().addBox(2.1F, 0.5F, -3.5F, 2.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("ClothchestR", CubeListBuilder.create().texOffs(108, 38).mirror(false).addBox(-4.1F, 0.5F, -3.5F, 2.0F, 8.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            left_leg.addOrReplaceChild("LegpanelL4", CubeListBuilder.create().texOffs(76, 38).mirror().addBox(1.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
            left_leg.addOrReplaceChild("LegpanelL5", CubeListBuilder.create().texOffs(76, 42).mirror().addBox(1.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
            left_leg.addOrReplaceChild("LegpanelL6", CubeListBuilder.create().texOffs(82, 38).mirror().addBox(1.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
            left_leg.addOrReplaceChild("SidepanelL1", CubeListBuilder.create().texOffs(116, 25).mirror(false).addBox(1.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
            left_leg.addOrReplaceChild("SideclothL1", CubeListBuilder.create().texOffs(116, 42).mirror(false).addBox(1.5F, 0.5F, -2.5F, 1.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.122173F));
            left_leg.addOrReplaceChild("SideclothL2", CubeListBuilder.create().texOffs(116, 34).mirror(false).addBox(0.5F, 5.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.296706F));
            left_leg.addOrReplaceChild("SideclothL3", CubeListBuilder.create().texOffs(116, 1).mirror(false).addBox(-1.4F, 8.4F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }
}
