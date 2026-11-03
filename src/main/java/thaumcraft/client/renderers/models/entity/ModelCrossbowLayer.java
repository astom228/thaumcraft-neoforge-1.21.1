package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Геометрия ModelCrossbow (сгенерировано tools/models/convert-entity-models.js — не править вручную). Имена частей — поля оригинала. */
@SuppressWarnings("unused")
public final class ModelCrossbowLayer {
    private ModelCrossbowLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        {
            PartDefinition crossbow = root.addOrReplaceChild("crossbow", CubeListBuilder.create().texOffs(28, 14).mirror(false).addBox(-2.0F, 0.0F, -7.0F, 4.0F, 2.0F, 14.0F), PartPose.offsetAndRotation(0.0F, 10.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            crossbow.addOrReplaceChild("ammobox", CubeListBuilder.create().texOffs(38, 0).mirror(false).addBox(-2.0F, -5.0F, -6.0F, 4.0F, 5.0F, 9.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            crossbow.addOrReplaceChild("barrel", CubeListBuilder.create().texOffs(20, 28).mirror(false).addBox(-1.0F, -1.0F, -8.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            crossbow.addOrReplaceChild("basebarcross", CubeListBuilder.create().texOffs(0, 13).mirror(false).addBox(-2.0F, 0.5F, 10.0F, 4.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            crossbow.addOrReplaceChild("basebarr", CubeListBuilder.create().texOffs(40, 23).mirror(false).addBox(-1.0F, 0.0F, 7.0F, 1.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.1396263F, 0.0F));
            crossbow.addOrReplaceChild("basebarl", CubeListBuilder.create().texOffs(40, 23).mirror(false).addBox(0.0F, 0.0F, 7.0F, 1.0F, 2.0F, 5.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.1396263F, 0.0F));
            crossbow.addOrReplaceChild("loadbarcross", CubeListBuilder.create().texOffs(0, 13).mirror(false).addBox(-2.0F, -8.5F, -0.5F, 4.0F, 1.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585054F, 0.0F, 0.0F));
            crossbow.addOrReplaceChild("loadbarl", CubeListBuilder.create().texOffs(0, 15).mirror(false).addBox(2.0F, -9.0F, -1.0F, 1.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585054F, 0.0F, 0.0F));
            crossbow.addOrReplaceChild("loadbarr", CubeListBuilder.create().texOffs(0, 15).mirror(false).addBox(-3.0F, -9.0F, -1.0F, 1.0F, 11.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.5585054F, 0.0F, 0.0F));
            crossbow.addOrReplaceChild("crossl1", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(0.0F, 0.0F, -6.0F, 5.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443461F, 0.0F));
            crossbow.addOrReplaceChild("crossl2", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(4.0F, 0.0F, -5.0F, 3.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443461F, 0.0F));
            crossbow.addOrReplaceChild("crossl3", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(6.0F, 0.0F, -4.0F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -0.2443461F, 0.0F));
            crossbow.addOrReplaceChild("crossr1", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-5.0F, 0.0F, -6.0F, 5.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443461F, 0.0F));
            crossbow.addOrReplaceChild("crossr2", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-7.0F, 0.0F, -5.0F, 3.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443461F, 0.0F));
            crossbow.addOrReplaceChild("crossr3", CubeListBuilder.create().texOffs(0, 0).mirror(false).addBox(-8.0F, 0.0F, -4.0F, 2.0F, 2.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.2443461F, 0.0F));
        }
        root.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(20, 10).mirror(false).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5235988F, 1.570796F, 0.0F));
        root.addOrReplaceChild("tripod", CubeListBuilder.create().texOffs(13, 0).mirror(false).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(20, 10).mirror(false).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5235988F, 3.141593F, 0.0F));
        root.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(20, 10).mirror(false).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5235988F, 4.712389F, 0.0F));
        root.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(20, 10).mirror(false).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5235988F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }
}
