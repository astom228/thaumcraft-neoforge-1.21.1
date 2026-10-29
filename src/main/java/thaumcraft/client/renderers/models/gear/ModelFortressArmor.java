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
 * Порт ModelFortressArmor (сгенерировано tools/models/convert-armor-models.js — не править вручную). Части — как в оригинале;
 * legs = true — вариант для поножей (ModelBiped(0.5F)), иначе — для шлема, нагрудника и ботинок (1.0F).
 * Части, которых нет в варианте, — null. Анимацию и скрытие частей задаёт код предмета (render оригинала).
 */
public class ModelFortressArmor extends HumanoidModel<LivingEntity> {
    public final ModelPart Mask0;
    public final ModelPart Mask1;
    public final ModelPart Mask2;
    public final ModelPart Goggles;
    public final ModelPart OrnamentL;
    public final ModelPart OrnamentL2;
    public final ModelPart OrnamentR;
    public final ModelPart OrnamentR2;
    public final ModelPart Helmet;
    public final ModelPart HelmetR;
    public final ModelPart HelmetL;
    public final ModelPart HelmetB;
    public final ModelPart capsthingy;
    public final ModelPart flapR;
    public final ModelPart flapL;
    public final ModelPart Gemornament;
    public final ModelPart Gem;
    public final ModelPart BeltR;
    public final ModelPart Mbelt;
    public final ModelPart MbeltL;
    public final ModelPart MbeltR;
    public final ModelPart BeltL;
    public final ModelPart Chestplate;
    public final ModelPart Scroll;
    public final ModelPart Backplate;
    public final ModelPart Book;
    public final ModelPart ShoulderR;
    public final ModelPart GauntletR;
    public final ModelPart GauntletstrapR1;
    public final ModelPart GauntletstrapR2;
    public final ModelPart ShoulderplateRtop;
    public final ModelPart ShoulderplateR1;
    public final ModelPart ShoulderplateR2;
    public final ModelPart ShoulderplateR3;
    public final ModelPart ShoulderL;
    public final ModelPart GauntletL;
    public final ModelPart Gauntletstrapl1;
    public final ModelPart GauntletstrapL2;
    public final ModelPart ShoulderplateLtop;
    public final ModelPart ShoulderplateL1;
    public final ModelPart ShoulderplateL2;
    public final ModelPart ShoulderplateL3;
    public final ModelPart LegpanelR1;
    public final ModelPart LegpanelR2;
    public final ModelPart LegpanelR3;
    public final ModelPart LegpanelR4;
    public final ModelPart LegpanelR5;
    public final ModelPart LegpanelR6;
    public final ModelPart SidepanelR1;
    public final ModelPart SidepanelR2;
    public final ModelPart SidepanelR3;
    public final ModelPart BackpanelR1;
    public final ModelPart BackpanelR2;
    public final ModelPart BackpanelR3;
    public final ModelPart BackpanelL3;
    public final ModelPart LegpanelL1;
    public final ModelPart LegpanelL2;
    public final ModelPart LegpanelL3;
    public final ModelPart LegpanelL4;
    public final ModelPart LegpanelL5;
    public final ModelPart LegpanelL6;
    public final ModelPart SidepanelL1;
    public final ModelPart SidepanelL2;
    public final ModelPart SidepanelL3;
    public final ModelPart BackpanelL1;
    public final ModelPart BackpanelL2;

    public ModelFortressArmor(ModelPart root) {
        super(root);
        this.Mask0 = find(root, "head", "Mask0");
        this.Mask1 = find(root, "head", "Mask1");
        this.Mask2 = find(root, "head", "Mask2");
        this.Goggles = find(root, "head", "Goggles");
        this.OrnamentL = find(root, "head", "OrnamentL");
        this.OrnamentL2 = find(root, "head", "OrnamentL2");
        this.OrnamentR = find(root, "head", "OrnamentR");
        this.OrnamentR2 = find(root, "head", "OrnamentR2");
        this.Helmet = find(root, "head", "Helmet");
        this.HelmetR = find(root, "head", "HelmetR");
        this.HelmetL = find(root, "head", "HelmetL");
        this.HelmetB = find(root, "head", "HelmetB");
        this.capsthingy = find(root, "head", "capsthingy");
        this.flapR = find(root, "head", "flapR");
        this.flapL = find(root, "head", "flapL");
        this.Gemornament = find(root, "head", "Gemornament");
        this.Gem = find(root, "head", "Gem");
        this.BeltR = find(root, "body", "BeltR");
        this.Mbelt = find(root, "body", "Mbelt");
        this.MbeltL = find(root, "body", "MbeltL");
        this.MbeltR = find(root, "body", "MbeltR");
        this.BeltL = find(root, "body", "BeltL");
        this.Chestplate = find(root, "body", "Chestplate");
        this.Scroll = find(root, "body", "Scroll");
        this.Backplate = find(root, "body", "Backplate");
        this.Book = find(root, "body", "Book");
        this.ShoulderR = find(root, "right_arm", "ShoulderR");
        this.GauntletR = find(root, "right_arm", "GauntletR");
        this.GauntletstrapR1 = find(root, "right_arm", "GauntletstrapR1");
        this.GauntletstrapR2 = find(root, "right_arm", "GauntletstrapR2");
        this.ShoulderplateRtop = find(root, "right_arm", "ShoulderplateRtop");
        this.ShoulderplateR1 = find(root, "right_arm", "ShoulderplateR1");
        this.ShoulderplateR2 = find(root, "right_arm", "ShoulderplateR2");
        this.ShoulderplateR3 = find(root, "right_arm", "ShoulderplateR3");
        this.ShoulderL = find(root, "left_arm", "ShoulderL");
        this.GauntletL = find(root, "left_arm", "GauntletL");
        this.Gauntletstrapl1 = find(root, "left_arm", "Gauntletstrapl1");
        this.GauntletstrapL2 = find(root, "left_arm", "GauntletstrapL2");
        this.ShoulderplateLtop = find(root, "left_arm", "ShoulderplateLtop");
        this.ShoulderplateL1 = find(root, "left_arm", "ShoulderplateL1");
        this.ShoulderplateL2 = find(root, "left_arm", "ShoulderplateL2");
        this.ShoulderplateL3 = find(root, "left_arm", "ShoulderplateL3");
        this.LegpanelR1 = find(root, "right_leg", "LegpanelR1");
        this.LegpanelR2 = find(root, "right_leg", "LegpanelR2");
        this.LegpanelR3 = find(root, "right_leg", "LegpanelR3");
        this.LegpanelR4 = find(root, "right_leg", "LegpanelR4");
        this.LegpanelR5 = find(root, "right_leg", "LegpanelR5");
        this.LegpanelR6 = find(root, "right_leg", "LegpanelR6");
        this.SidepanelR1 = find(root, "right_leg", "SidepanelR1");
        this.SidepanelR2 = find(root, "right_leg", "SidepanelR2");
        this.SidepanelR3 = find(root, "right_leg", "SidepanelR3");
        this.BackpanelR1 = find(root, "right_leg", "BackpanelR1");
        this.BackpanelR2 = find(root, "right_leg", "BackpanelR2");
        this.BackpanelR3 = find(root, "right_leg", "BackpanelR3");
        this.BackpanelL3 = find(root, "left_leg", "BackpanelL3");
        this.LegpanelL1 = find(root, "left_leg", "LegpanelL1");
        this.LegpanelL2 = find(root, "left_leg", "LegpanelL2");
        this.LegpanelL3 = find(root, "left_leg", "LegpanelL3");
        this.LegpanelL4 = find(root, "left_leg", "LegpanelL4");
        this.LegpanelL5 = find(root, "left_leg", "LegpanelL5");
        this.LegpanelL6 = find(root, "left_leg", "LegpanelL6");
        this.SidepanelL1 = find(root, "left_leg", "SidepanelL1");
        this.SidepanelL2 = find(root, "left_leg", "SidepanelL2");
        this.SidepanelL3 = find(root, "left_leg", "SidepanelL3");
        this.BackpanelL1 = find(root, "left_leg", "BackpanelL1");
        this.BackpanelL2 = find(root, "left_leg", "BackpanelL2");
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
        head.addOrReplaceChild("OrnamentL", CubeListBuilder.create().texOffs(78, 8).mirror().addBox(1.5F, -9.0F, -6.5F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("OrnamentL2", CubeListBuilder.create().texOffs(78, 8).mirror().addBox(3.5F, -10.0F, -6.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("OrnamentR", CubeListBuilder.create().texOffs(78, 8).mirror(false).addBox(-3.5F, -9.0F, -6.5F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("OrnamentR2", CubeListBuilder.create().texOffs(78, 8).mirror(false).addBox(-4.5F, -10.0F, -6.5F, 1.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("Helmet", CubeListBuilder.create().texOffs(41, 8).mirror(false).addBox(-4.5F, -9.0F, -4.5F, 9.0F, 4.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("HelmetR", CubeListBuilder.create().texOffs(21, 13).mirror(false).addBox(-6.5F, -3.0F, -4.5F, 1.0F, 5.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F));
        head.addOrReplaceChild("HelmetL", CubeListBuilder.create().texOffs(21, 13).mirror().addBox(5.5F, -3.0F, -4.5F, 1.0F, 5.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F));
        head.addOrReplaceChild("HelmetB", CubeListBuilder.create().texOffs(41, 21).mirror(false).addBox(-4.5F, -3.0F, 5.5F, 9.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.5235988F, 0.0F, 0.0F));
        head.addOrReplaceChild("capsthingy", CubeListBuilder.create().texOffs(21, 0).mirror(false).addBox(-4.5F, -6.0F, -6.5F, 9.0F, 1.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("flapR", CubeListBuilder.create().texOffs(59, 10).mirror(false).addBox(-10.0F, -2.0F, -1.0F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.5235988F, 0.5235988F));
        head.addOrReplaceChild("flapL", CubeListBuilder.create().texOffs(59, 10).mirror().addBox(7.0F, -2.0F, -1.0F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.5235988F, -0.5235988F));
        head.addOrReplaceChild("Gemornament", CubeListBuilder.create().texOffs(68, 11).mirror(false).addBox(-1.5F, -9.0F, -7.0F, 3.0F, 3.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("Gem", CubeListBuilder.create().texOffs(72, 8).mirror(false).addBox(-1.0F, -8.5F, -7.5F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1396263F, 0.0F, 0.0F));
        head.addOrReplaceChild("Goggles", CubeListBuilder.create().texOffs(100, 18).mirror(false).addBox(-4.5F, -5.0F, -4.25F, 9.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("Mask0", CubeListBuilder.create().texOffs(52, 2).mirror(false).addBox(-4.5F, -5.0F, -4.6F, 9.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("Mask1", CubeListBuilder.create().texOffs(76, 2).mirror(false).addBox(-4.5F, -5.0F, -4.6F, 9.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        head.addOrReplaceChild("Mask2", CubeListBuilder.create().texOffs(100, 2).mirror(false).addBox(-4.5F, -5.0F, -4.6F, 9.0F, 5.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("ShoulderR", CubeListBuilder.create().texOffs(56, 35).mirror(false).addBox(-3.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("GauntletR", CubeListBuilder.create().texOffs(100, 26).mirror(false).addBox(-3.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("GauntletstrapR1", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("GauntletstrapR2", CubeListBuilder.create().texOffs(84, 31).mirror(false).addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        right_arm.addOrReplaceChild("ShoulderplateRtop", CubeListBuilder.create().texOffs(110, 37).mirror(false).addBox(-5.5F, -2.5F, -3.5F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_arm.addOrReplaceChild("ShoulderplateR1", CubeListBuilder.create().texOffs(110, 45).mirror(false).addBox(-4.5F, -1.5F, -3.5F, 1.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_arm.addOrReplaceChild("ShoulderplateR2", CubeListBuilder.create().texOffs(94, 45).mirror(false).addBox(-3.5F, 1.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_arm.addOrReplaceChild("ShoulderplateR3", CubeListBuilder.create().texOffs(94, 45).mirror(false).addBox(-2.5F, 3.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        left_arm.addOrReplaceChild("ShoulderL", CubeListBuilder.create().texOffs(56, 35).mirror().addBox(-1.5F, -2.5F, -2.5F, 5.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("GauntletL", CubeListBuilder.create().texOffs(114, 26).mirror(false).addBox(1.5F, 3.5F, -2.5F, 2.0F, 6.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("Gauntletstrapl1", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 3.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("GauntletstrapL2", CubeListBuilder.create().texOffs(84, 31).mirror().addBox(-1.5F, 6.5F, -2.5F, 3.0F, 1.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        left_arm.addOrReplaceChild("ShoulderplateLtop", CubeListBuilder.create().texOffs(110, 37).mirror().addBox(3.5F, -2.5F, -3.5F, 2.0F, 1.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_arm.addOrReplaceChild("ShoulderplateL1", CubeListBuilder.create().texOffs(110, 45).mirror().addBox(3.5F, -1.5F, -3.5F, 1.0F, 4.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_arm.addOrReplaceChild("ShoulderplateL2", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(2.5F, 1.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_arm.addOrReplaceChild("ShoulderplateL3", CubeListBuilder.create().texOffs(94, 45).mirror().addBox(1.5F, 3.5F, -3.5F, 1.0F, 3.0F, 7.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        right_leg.addOrReplaceChild("LegpanelR1", CubeListBuilder.create().texOffs(0, 51).mirror(false).addBox(-1.0F, 0.5F, -3.5F, 3.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("LegpanelR2", CubeListBuilder.create().texOffs(8, 51).mirror(false).addBox(-1.0F, 3.5F, -2.5F, 3.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("LegpanelR3", CubeListBuilder.create().texOffs(0, 56).mirror(false).addBox(-1.0F, 6.5F, -1.5F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("LegpanelR4", CubeListBuilder.create().texOffs(0, 43).mirror(false).addBox(-3.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("LegpanelR5", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(-3.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("LegpanelR6", CubeListBuilder.create().texOffs(6, 43).mirror(false).addBox(-3.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("SidepanelR1", CubeListBuilder.create().texOffs(0, 22).mirror(false).addBox(-2.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_leg.addOrReplaceChild("SidepanelR2", CubeListBuilder.create().texOffs(0, 31).mirror(false).addBox(-1.5F, 3.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_leg.addOrReplaceChild("SidepanelR3", CubeListBuilder.create().texOffs(12, 31).mirror(false).addBox(-0.5F, 5.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363323F));
        right_leg.addOrReplaceChild("BackpanelR1", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-3.0F, 0.5F, 2.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("BackpanelR2", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-3.0F, 2.5F, 1.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        right_leg.addOrReplaceChild("BackpanelR3", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-3.0F, 4.5F, 0.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("BackpanelL3", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2.0F, 4.5F, 0.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("LegpanelL1", CubeListBuilder.create().texOffs(0, 51).mirror().addBox(-2.0F, 0.5F, -3.5F, 3.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("LegpanelL2", CubeListBuilder.create().texOffs(8, 51).mirror().addBox(-2.0F, 3.5F, -2.5F, 3.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("LegpanelL3", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-2.0F, 6.5F, -1.5F, 3.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("LegpanelL4", CubeListBuilder.create().texOffs(0, 43).mirror().addBox(1.0F, 0.5F, -3.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("LegpanelL5", CubeListBuilder.create().texOffs(0, 47).mirror().addBox(1.0F, 2.5F, -2.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("LegpanelL6", CubeListBuilder.create().texOffs(6, 43).mirror().addBox(1.0F, 4.5F, -1.5F, 2.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("SidepanelL1", CubeListBuilder.create().texOffs(0, 22).mirror().addBox(1.5F, 0.5F, -2.5F, 1.0F, 4.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_leg.addOrReplaceChild("SidepanelL2", CubeListBuilder.create().texOffs(0, 31).mirror().addBox(0.5F, 3.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_leg.addOrReplaceChild("SidepanelL3", CubeListBuilder.create().texOffs(12, 31).mirror().addBox(-0.5F, 5.5F, -2.5F, 1.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363323F));
        left_leg.addOrReplaceChild("BackpanelL1", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2.0F, 0.5F, 2.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        left_leg.addOrReplaceChild("BackpanelL2", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(-2.0F, 2.5F, 1.5F, 5.0F, 3.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.4363323F, 0.0F, 0.0F));
        if (legs) {
            body.addOrReplaceChild("Mbelt", CubeListBuilder.create().texOffs(56, 55).mirror(false).addBox(-4.0F, 8.0F, -3.0F, 8.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("MbeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 8.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        } else {
            body.addOrReplaceChild("BeltR", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(-5.0F, 4.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("BeltL", CubeListBuilder.create().texOffs(76, 44).mirror(false).addBox(4.0F, 4.0F, -3.0F, 1.0F, 3.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Chestplate", CubeListBuilder.create().texOffs(56, 45).mirror(false).addBox(-4.0F, 1.0F, -4.0F, 8.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Scroll", CubeListBuilder.create().texOffs(34, 27).mirror(false).addBox(-2.0F, 9.5F, 4.0F, 8.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.1919862F));
            body.addOrReplaceChild("Backplate", CubeListBuilder.create().texOffs(36, 45).mirror(false).addBox(-4.0F, 1.0F, 2.0F, 8.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            body.addOrReplaceChild("Book", CubeListBuilder.create().texOffs(100, 8).mirror(false).addBox(1.0F, -0.3F, 4.0F, 5.0F, 7.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.7679449F));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }
}
