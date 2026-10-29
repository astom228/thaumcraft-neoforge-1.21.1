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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.devices.TileBellows;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;

/**
 * Мехи (порт TileBellowsRenderer + ModelBellows): доски сходятся и расходятся вместе с мешком (inflation), сопло спереди;
 * если впереди буфер эссенции — насадка от бура (ModelBoreBase.renderNozzle). У мешка своя развёртка 64×32 (как в оригинале).
 */
public class TileBellowsRenderer implements BlockEntityRenderer<TileBellows> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/bellows.png");
    private static final ResourceLocation TEX_BORE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/bore.png");
    private final ModelPart bottomPlank;
    private final ModelPart middlePlank;
    private final ModelPart topPlank;
    private final ModelPart nozzle;
    private final ModelPart bag;
    private final ModelPart nozzleExt;

    public TileBellowsRenderer(BlockEntityRendererProvider.Context ctx) {
        MeshDefinition md = new MeshDefinition();
        PartDefinition root = md.getRoot();
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6.0F, 0.0F, -6.0F, 12, 2, 12), PartPose.offset(0.0F, 22.0F, 0.0F));
        root.addOrReplaceChild("middle", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6.0F, -1.0F, -6.0F, 12, 2, 12), PartPose.offset(0.0F, 16.0F, 0.0F));
        root.addOrReplaceChild("top", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-6.0F, 0.0F, -6.0F, 12, 2, 12), PartPose.offset(0.0F, 8.0F, 0.0F));
        root.addOrReplaceChild("nozzle", CubeListBuilder.create().texOffs(0, 36).mirror().addBox(-2.0F, -2.0F, 0.0F, 4, 4, 2), PartPose.offset(0.0F, 16.0F, 6.0F));
        ModelPart m = LayerDefinition.create(md, 128, 128).bakeRoot();
        this.bottomPlank = m.getChild("bottom");
        this.middlePlank = m.getChild("middle");
        this.topPlank = m.getChild("top");
        this.nozzle = m.getChild("nozzle");
        MeshDefinition bd = new MeshDefinition();
        bd.getRoot().addOrReplaceChild("bag", CubeListBuilder.create().texOffs(48, 0).mirror().addBox(-10.0F, -12.03333F, -10.0F, 20, 24, 20),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        this.bag = LayerDefinition.create(bd, 64, 32).bakeRoot().getChild("bag");
        MeshDefinition nd = new MeshDefinition();
        nd.getRoot().addOrReplaceChild("n1", CubeListBuilder.create().texOffs(106, 42).mirror().addBox(2.5F, -2.0F, -2.0F, 5, 4, 4), PartPose.offset(0.0F, 8.0F, 0.0F));
        nd.getRoot().addOrReplaceChild("n2", CubeListBuilder.create().texOffs(106, 51).mirror().addBox(7.0F, -2.5F, -2.5F, 1, 5, 5), PartPose.offset(0.0F, 8.0F, 0.0F));
        this.nozzleExt = LayerDefinition.create(nd, 128, 64).bakeRoot();
    }

    @Override
    public void render(TileBellows bellows, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float scale = bellows.inflation;
        Direction dir = bellows.facing();
        boolean extension = bellows.getLevel() != null && bellows.getLevel().getBlockEntity(bellows.getBlockPos().relative(dir)) instanceof TileTubeBuffer;
        float tscale = 0.125F + scale * 0.875F;
        if (extension) {
            pose.pushPose();
            pose.translate(0.5F + dir.getStepX(), dir.getStepY(), 0.5F + dir.getStepZ());
            switch (dir.getOpposite()) {
                case DOWN -> {
                    pose.translate(-0.5F, 0.5F, 0.0F);
                    pose.mulPose(Axis.ZN.rotationDegrees(90.0F));
                }
                case UP -> {
                    pose.translate(0.5F, 0.5F, 0.0F);
                    pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
                }
                case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
                case WEST -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                default -> {
                }
            }
            this.nozzleExt.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BORE)), light, overlay);
            pose.popPose();
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        pose.pushPose();
        // translateFromOrientation
        pose.translate(0.5F, -0.5F, 0.5F);
        switch (dir) {
            case DOWN -> {
                pose.translate(0.0F, 1.0F, -1.0F);
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            }
            case UP -> {
                pose.translate(0.0F, 1.0F, 1.0F);
                pose.mulPose(Axis.XP.rotationDegrees(270.0F));
            }
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        pose.translate(0.0F, 1.0F, 0.0F);
        pose.pushPose();
        pose.scale(0.5F, (scale + 0.1F) / 2.0F, 0.5F);
        this.bag.render(pose, vc, light, overlay);
        pose.popPose();
        pose.translate(0.0F, -1.0F, 0.0F);
        pose.pushPose();
        pose.translate(0.0F, -tscale / 2.0F + 0.5F, 0.0F);
        this.topPlank.render(pose, vc, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.0F, tscale / 2.0F - 0.5F, 0.0F);
        this.bottomPlank.render(pose, vc, light, overlay);
        pose.popPose();
        this.middlePlank.render(pose, vc, light, overlay);
        this.nozzle.render(pose, vc, light, overlay);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TileBellows bellows) {
        return new AABB(bellows.getBlockPos()).inflate(0.3);
    }
}
