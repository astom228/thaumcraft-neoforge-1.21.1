package thaumcraft.client.renderers.models.entity;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Геометрия ModelArcaneBore (сгенерировано tools/models/convert-entity-models.js — не править вручную). Имена частей — поля оригинала. */
@SuppressWarnings("unused")
public final class ModelArcaneBoreLayer {
    private ModelArcaneBoreLayer() {
    }

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("leg2", CubeListBuilder.create().texOffs(20, 10).mirror(false).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5235988F, 1.570796F, 0.0F));
        root.addOrReplaceChild("tripod", CubeListBuilder.create().texOffs(13, 0).mirror(false).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("leg3", CubeListBuilder.create().texOffs(20, 10).mirror(false).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5235988F, 3.141593F, 0.0F));
        root.addOrReplaceChild("leg4", CubeListBuilder.create().texOffs(20, 10).mirror(false).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5235988F, 4.712389F, 0.0F));
        root.addOrReplaceChild("leg1", CubeListBuilder.create().texOffs(20, 10).mirror(false).addBox(-1.0F, 1.0F, -1.0F, 2.0F, 13.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 0.5235988F, 0.0F, 0.0F));
        {
            PartDefinition base = root.addOrReplaceChild("base", CubeListBuilder.create().texOffs(32, 0).mirror(false).addBox(-3.0F, -6.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            base.addOrReplaceChild("crystal", CubeListBuilder.create().texOffs(32, 25).mirror(false).addBox(-1.0F, -4.0F, 5.0F, 2.0F, 2.0F, 2.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            base.addOrReplaceChild("dome", CubeListBuilder.create().texOffs(44, 16).mirror(false).addBox(-2.0F, -5.0F, 4.0F, 4.0F, 4.0F, 4.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            base.addOrReplaceChild("domebase", CubeListBuilder.create().texOffs(32, 19).mirror(false).addBox(-2.0F, -5.0F, 3.0F, 4.0F, 4.0F, 1.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            base.addOrReplaceChild("magbase", CubeListBuilder.create().texOffs(0, 18).mirror(false).addBox(-1.0F, -4.0F, -6.0F, 2.0F, 2.0F, 3.0F), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
            base.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 9).mirror(false).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offsetAndRotation(0.0F, -3.0F, -6.0F, -1.570796F, 0.0F, 0.0F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }
}
