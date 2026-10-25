package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.tiles.essentia.TileAlembic;

/**
 * Рендер алембика (порт TileAlembicRenderer): ярлык с аспектом-фильтром на грани facing и патрубки (Nozzle из ModelBoreBase)
 * к соседним трубам, принимающим эссенцию, по бокам.
 */
public class TileAlembicRenderer implements BlockEntityRenderer<TileAlembic> {
    private static final ResourceLocation TEX_LABEL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/label.png");
    private static final ResourceLocation TEX_BORE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/bore.png");
    private final ModelPart nozzle;

    public TileAlembicRenderer(BlockEntityRendererProvider.Context ctx) {
        // ModelBoreBase.Nozzle1 / Nozzle2 (текстура 128×64, точка вращения (0, 8, 0))
        MeshDefinition md = new MeshDefinition();
        md.getRoot().addOrReplaceChild("nozzle1", CubeListBuilder.create().texOffs(106, 42).mirror().addBox(2.5F, -2.0F, -2.0F, 5, 4, 4),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        md.getRoot().addOrReplaceChild("nozzle2", CubeListBuilder.create().texOffs(106, 51).mirror().addBox(7.0F, -2.5F, -2.5F, 1, 5, 5),
                PartPose.offset(0.0F, 8.0F, 0.0F));
        this.nozzle = LayerDefinition.create(md, 128, 64).bakeRoot();
    }

    @Override
    public void render(TileAlembic tile, float f, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.aspectFilter != null) {
            pose.pushPose();
            pose.translate(0.5F, 0.0F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            switch (tile.facing) {
                case 2 -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
                case 3 -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                case 5 -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                default -> {
                }
            }
            pose.pushPose();
            pose.translate(0.0F, 0.5F, -0.376F);
            UtilsFX.renderQuadCenteredWorld(pose, buffers.getBuffer(RenderType.entityTranslucent(TEX_LABEL)), 0.44F, 1.0F, 1.0F, 1.0F, 1.0F, light);
            pose.popPose();
            pose.pushPose();
            pose.translate(0.0F, 0.5F, -0.377F);
            pose.scale(0.02F, 0.02F, 0.02F);
            pose.mulPose(Axis.ZP.rotationDegrees(180.0F));
            UtilsFX.drawTagWorld(pose, buffers, -8, -8, tile.aspectFilter, light);
            pose.popPose();
            pose.popPose();
        }
        if (tile.getLevel() != null) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BORE));
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (tile.canOutputTo(dir)) {
                    BlockEntity te = ThaumcraftApiHelper.getConnectableTile(tile.getLevel(), tile.getBlockPos(), dir);
                    if (te != null && ((IEssentiaTransport) te).canInputFrom(dir.getOpposite())) {
                        pose.pushPose();
                        pose.translate(0.5F, 0.0F, 0.5F);
                        switch (dir) {
                            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
                            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                            default -> {
                            }
                        }
                        this.nozzle.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
                        pose.popPose();
                    }
                }
            }
        }
    }
}
