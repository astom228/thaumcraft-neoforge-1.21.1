package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.essentia.TileCentrifuge;

/** Рендер центрифуги (порт TileCentrifugeRenderer + ModelCentrifuge): неподвижные верх и низ, вращающаяся крестовина. */
public class TileCentrifugeRenderer implements BlockEntityRenderer<TileCentrifuge> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/centrifuge.png");
    private final ModelPart boxes;
    private final ModelPart spinny;

    public TileCentrifugeRenderer(BlockEntityRendererProvider.Context ctx) {
        MeshDefinition mb = new MeshDefinition();
        PartDefinition r = mb.getRoot();
        r.addOrReplaceChild("top", CubeListBuilder.create().texOffs(20, 16).mirror().addBox(-4.0F, -8.0F, -4.0F, 8, 4, 8), PartPose.ZERO);
        r.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(20, 16).mirror().addBox(-4.0F, 4.0F, -4.0F, 8, 4, 8), PartPose.ZERO);
        this.boxes = LayerDefinition.create(mb, 64, 32).bakeRoot();
        MeshDefinition ms = new MeshDefinition();
        PartDefinition s = ms.getRoot();
        s.addOrReplaceChild("crossbar", CubeListBuilder.create().texOffs(16, 0).mirror().addBox(-4.0F, -1.0F, -1.0F, 8, 2, 2), PartPose.ZERO);
        s.addOrReplaceChild("dingus1", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(4.0F, -3.0F, -2.0F, 4, 6, 4), PartPose.ZERO);
        s.addOrReplaceChild("dingus2", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-8.0F, -3.0F, -2.0F, 4, 6, 4), PartPose.ZERO);
        s.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.5F, -4.0F, -1.5F, 3, 8, 3), PartPose.ZERO);
        this.spinny = LayerDefinition.create(ms, 64, 32).bakeRoot();
    }

    @Override
    public void render(TileCentrifuge cf, float f, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        this.boxes.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.mulPose(Axis.YP.rotationDegrees(cf.rotation));
        this.spinny.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
