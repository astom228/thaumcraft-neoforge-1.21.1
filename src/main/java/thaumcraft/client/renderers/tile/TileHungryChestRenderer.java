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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.ChestBlock;
import thaumcraft.Thaumcraft;
import thaumcraft.common.tiles.devices.TileHungryChest;

/**
 * Голодный сундук (порт TileHungryChestRenderer): модель сундука 1.12 (ModelChest — развёртка до 1.15, поэтому не ванильный
 * ChestRenderer) с текстурой chesthungry; крышка открывается по кривой оригинала.
 */
public class TileHungryChestRenderer implements BlockEntityRenderer<TileHungryChest> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/chesthungry.png");
    private final ModelPart lid;
    private final ModelPart knob;
    private final ModelPart bottom;

    public TileHungryChestRenderer(BlockEntityRendererProvider.Context ctx) {
        MeshDefinition md = new MeshDefinition();
        PartDefinition root = md.getRoot();
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -5.0F, -14.0F, 14, 5, 14), PartPose.offset(1.0F, 7.0F, 15.0F));
        root.addOrReplaceChild("knob", CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -2.0F, -15.0F, 2, 4, 1), PartPose.offset(8.0F, 7.0F, 15.0F));
        root.addOrReplaceChild("bottom", CubeListBuilder.create().texOffs(0, 19).addBox(0.0F, -10.0F, -14.0F, 14, 10, 14), PartPose.offset(1.0F, 6.0F, 1.0F));
        ModelPart m = LayerDefinition.create(md, 64, 64).bakeRoot();
        this.lid = m.getChild("lid");
        this.knob = m.getChild("knob");
        this.bottom = m.getChild("bottom");
    }

    @Override
    public void render(TileHungryChest chest, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.0F, 1.0F, 1.0F);
        pose.scale(1.0F, -1.0F, -1.0F);
        pose.translate(0.5F, 0.5F, 0.5F);
        float rot = switch (chest.getBlockState().getValue(ChestBlock.FACING)) {
            case NORTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        pose.mulPose(Axis.YP.rotationDegrees(rot));
        pose.translate(-0.5F, -0.5F, -0.5F);
        float a = chest.getOpenNess(pt);
        a = 1.0F - a;
        a = 1.0F - a * a * a;
        this.lid.xRot = -(a * (float) Math.PI / 2.0F);
        this.knob.xRot = this.lid.xRot;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TEX));
        this.lid.render(pose, vc, light, overlay);
        this.knob.render(pose, vc, light, overlay);
        this.bottom.render(pose, vc, light, overlay);
        pose.popPose();
    }
}
