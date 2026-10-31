package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.obj.WavefrontObject;
import thaumcraft.common.tiles.crafting.TileGolemBuilder;

/**
 * Пресс големов (порт TileGolemBuilderRenderer): OBJ-модель (стол, наковальня, короб) по стороне блока, пресс опускается
 * на sin(press)·0.625; в котле — квадрат лавы. В оригинале матрица текстуры переворачивала v — модель загружается без
 * переворота v.
 */
public class TileGolemBuilderRenderer implements BlockEntityRenderer<TileGolemBuilder> {
    private static final ResourceLocation TM = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "models/obj/golembuilder.obj");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/golembuilder.png");
    private static final ResourceLocation LAVA = ResourceLocation.withDefaultNamespace("block/lava_still");
    private WavefrontObject model;

    public TileGolemBuilderRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TileGolemBuilder tile, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (this.model == null) {
            this.model = WavefrontObject.load(TM, false);
        }
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        Direction facing = tile.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? tile.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
        switch (facing) {
            case SOUTH -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> {
            }
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        for (String part : this.model.getPartNames()) {
            if (!part.equalsIgnoreCase("press")) {
                this.model.renderPartEntity(part, vc, pose.last(), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
        pose.pushPose();
        double s = Math.sin(Math.toRadians(tile.press)) * 0.625;
        pose.translate(0.0, -s, 0.0);
        this.model.renderPartEntity("press", vc, pose.last(), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();

        pose.translate(-0.3125F, 0.625F, 1.3125F);
        pose.mulPose(Axis.XN.rotationDegrees(90.0F));
        pose.scale(0.625F, 0.625F, 0.625F);
        TextureAtlasSprite icon = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LAVA);
        VertexConsumer lv = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        PoseStack.Pose last = pose.last();
        Matrix4f m = last.pose();
        float f1 = icon.getU1();
        float f2 = icon.getV0();
        float f3 = icon.getU0();
        float f4 = icon.getV1();
        lv.addVertex(m, 0, 0, 0).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f1, f4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        lv.addVertex(m, 1, 0, 0).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f3, f4).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        lv.addVertex(m, 1, 1, 0).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f3, f2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        lv.addVertex(m, 0, 1, 0).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f1, f2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
        pose.popPose();
    }

    /** getRenderBoundingBox оригинала: x−1..x+2, y..y+2, z−1..z+2. */
    @Override
    public AABB getRenderBoundingBox(TileGolemBuilder tile) {
        var p = tile.getBlockPos();
        return new AABB(p.getX() - 1, p.getY(), p.getZ() - 1, p.getX() + 2, p.getY() + 2, p.getZ() + 2);
    }
}
