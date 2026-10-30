package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Геометрия ModelEldritchCrab (сгенерировано tools/models/convert-entity-models.js — не править вручную). Имена частей — поля оригинала. */
@SuppressWarnings("unused")
public final class ModelEldritchCrabLayer {
    private ModelEldritchCrabLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("TailHelm", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-4.5F, -4.5F, -0.4F, 9.0F, 9.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.1047198F, 0.0F, 0.0F));
        root.addOrReplaceChild("TailBare", CubeListBuilder.create().texOffs(64, 0).mirror(false).addBox(-4.0F, -4.0F, -0.4F, 8.0F, 8.0F, 8.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.1047198F, 0.0F, 0.0F));
        root.addOrReplaceChild("RClaw1", CubeListBuilder.create().texOffs(0, 47).mirror(false).addBox(-2.0F, -1.0F, -5.066667F, 4.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(-6.0F, 15.5F, -10.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Head1", CubeListBuilder.create().texOffs(0, 38).mirror(false).addBox(-2.0F, -1.5F, -9.066667F, 4.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("RClaw0", CubeListBuilder.create().texOffs(0, 55).mirror(false).addBox(-2.0F, -2.5F, -3.066667F, 4.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(-6.0F, 17.0F, -7.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("RClaw2", CubeListBuilder.create().texOffs(14, 54).mirror(false).addBox(-1.5F, -1.0F, -4.066667F, 3.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(-6.0F, 18.5F, -10.0F, 0.3141593F, 0.0F, 0.0F));
        root.addOrReplaceChild("RArm", CubeListBuilder.create().texOffs(44, 4).mirror(false).addBox(-1.0F, -1.0F, -5.066667F, 2.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(-3.0F, 17.0F, -4.0F, 0.0F, 0.7504916F, 0.0F));
        root.addOrReplaceChild("LClaw2", CubeListBuilder.create().texOffs(14, 54).mirror(false).addBox(-1.5F, -1.0F, -4.066667F, 3.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(6.0F, 18.5F, -10.0F, 0.3141593F, 0.0F, 0.0F));
        root.addOrReplaceChild("LClaw1", CubeListBuilder.create().texOffs(0, 47).mirror().addBox(-2.0F, -1.0F, -5.066667F, 4.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(6.0F, 15.5F, -10.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("LClaw0", CubeListBuilder.create().texOffs(0, 55).mirror().addBox(-2.0F, -2.5F, -3.066667F, 4.0F, 5.0F, 3.0F), PartPose.offsetAndRotation(6.0F, 17.0F, -7.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("LArm", CubeListBuilder.create().texOffs(44, 4).mirror(false).addBox(-1.0F, -1.0F, -4.066667F, 2.0F, 2.0F, 6.0F), PartPose.offsetAndRotation(4.0F, 17.0F, -5.0F, 0.0F, -0.7504916F, 0.0F));
        root.addOrReplaceChild("Torso", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-3.5F, -3.5F, -6.066667F, 7.0F, 7.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.0523599F, 0.0F, 0.0F));
        root.addOrReplaceChild("Head0", CubeListBuilder.create().texOffs(0, 31).mirror(false).addBox(-2.5F, -2.0F, -8.066667F, 5.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("RRLeg1", CubeListBuilder.create().texOffs(36, 4).mirror(false).addBox(-4.5F, 1.0F, -0.9F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 20.0F, -1.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("RFLeg1", CubeListBuilder.create().texOffs(36, 4).mirror(false).addBox(-5.0F, 1.0F, -1.066667F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 20.0F, -3.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("LRLeg1", CubeListBuilder.create().texOffs(36, 4).mirror(false).addBox(2.5F, 1.0F, -0.9F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 20.0F, -1.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("LFLeg1", CubeListBuilder.create().texOffs(36, 4).mirror(false).addBox(3.0F, 1.0F, -1.066667F, 2.0F, 5.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 20.0F, -3.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("RRLeg0", CubeListBuilder.create().texOffs(36, 0).mirror(false).addBox(-4.5F, -1.0F, -0.9F, 6.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 20.0F, -1.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("RFLeg0", CubeListBuilder.create().texOffs(36, 0).mirror(false).addBox(-5.0F, -1.0F, -1.066667F, 6.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(-4.0F, 20.0F, -3.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("LFLeg0", CubeListBuilder.create().texOffs(36, 0).mirror(false).addBox(-1.0F, -1.0F, -1.066667F, 6.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 20.0F, -3.5F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("LRLeg0", CubeListBuilder.create().texOffs(36, 0).mirror(false).addBox(-1.5F, -1.0F, -0.9F, 6.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(4.0F, 20.0F, -1.5F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
