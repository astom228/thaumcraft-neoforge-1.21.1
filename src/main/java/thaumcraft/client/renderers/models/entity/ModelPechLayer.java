package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Геометрия ModelPech (сгенерировано tools/models/convert-entity-models.js — не править вручную). Имена частей — поля оригинала. */
@SuppressWarnings("unused")
public final class ModelPechLayer {
    private ModelPechLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("bipedBody", CubeListBuilder.create().texOffs(34, 12).mirror(false).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 10.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 9.0F, -3.0F, 0.3129957F, 0.0F, 0.0F));
        root.addOrReplaceChild("bipedRightLeg", CubeListBuilder.create().texOffs(35, 1).mirror().addBox(-2.9F, 0.0F, 0.0F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bipedLeftLeg", CubeListBuilder.create().texOffs(35, 1).mirror(false).addBox(-0.1F, 0.0F, 0.0F, 3.0F, 6.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 18.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bipedHead", CubeListBuilder.create().texOffs(2, 11).mirror(false).addBox(-3.5F, -5.0F, -5.0F, 7.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("Jowls", CubeListBuilder.create().texOffs(1, 21).mirror(false).addBox(-4.0F, -1.0F, -6.0F, 8.0F, 3.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 8.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("LowerPack", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-5.0F, 0.0F, 0.0F, 10.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 3.5F, 0.3013602F, 0.0F, 0.0F));
        root.addOrReplaceChild("UpperPack", CubeListBuilder.create().texOffs(64, 1).mirror(false).addBox(-7.5F, -14.0F, 0.0F, 15.0F, 14.0F, 11.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 3.0F, 0.4537856F, 0.0F, 0.0F));
        root.addOrReplaceChild("bipedRightArm", CubeListBuilder.create().texOffs(52, 2).mirror().addBox(-2.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(-3.0F, 10.0F, -1.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bipedLeftArm", CubeListBuilder.create().texOffs(52, 2).mirror(false).addBox(0.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.offsetAndRotation(3.0F, 10.0F, -1.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }
}
