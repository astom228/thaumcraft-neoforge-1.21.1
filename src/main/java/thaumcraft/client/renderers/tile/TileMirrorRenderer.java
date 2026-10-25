package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.blocks.devices.BlockMirror;
import thaumcraft.common.tiles.devices.TileMirrorLinked;

/**
 * Рендер зеркал (порт TileMirrorRenderer): объёмная рамка (mirrorframe / mirrorframe2), у связанного зеркала ближе 32 блоков —
 * «портал» и полупрозрачное стекло поверх, у несвязанного — матовое стекло.
 */
public class TileMirrorRenderer implements BlockEntityRenderer<TileMirrorLinked> {
    private static final ResourceLocation MP = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/mirrorpane.png");
    private static final ResourceLocation MPT = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/mirrorpanetrans.png");
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "blocks/mirrorframe");
    private static final ResourceLocation FRAME2 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "blocks/mirrorframe2");
    private static final float P = 0.1875F;

    public TileMirrorRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TileMirrorLinked te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction dir = te.getBlockState().getValue(BlockMirror.FACING);
        boolean linked = te.linked;
        pose.pushPose();
        translateFromOrientation(pose, dir.ordinal(), 0.01F);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(te.getBlockState().is(BlocksTC.mirror.get()) ? FRAME : FRAME2);
        UtilsFX.renderItemIn2DWorld(pose, buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)), sprite, 0.0625F, light);
        pose.popPose();
        var player = Minecraft.getInstance().player;
        if (linked && player != null && player.distanceToSqr(te.getBlockPos().getCenter()) < 1024.0) {
            this.drawPlane(pose, buffers.getBuffer(TCRenderTypes.MIRROR_PORTAL), dir);
            this.pane(pose, buffers, dir, MPT, light);
        } else {
            this.pane(pose, buffers, dir, MP, light);
        }
    }

    private void pane(PoseStack pose, MultiBufferSource buffers, Direction dir, ResourceLocation tex, int light) {
        pose.pushPose();
        translateFromOrientation(pose, dir.ordinal(), 0.02F);
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        pose.translate(0.5, -0.5, 0.0);
        UtilsFX.renderQuadCenteredWorld(pose, buffers.getBuffer(RenderType.entityTranslucent(tex)), 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, light);
        pose.popPose();
    }

    /** drawPlaneYPos … drawPlaneZNeg: квадрат со вставкой 3/16 у грани опоры. */
    private void drawPlane(PoseStack pose, VertexConsumer vc, Direction dir) {
        Matrix4f m = pose.last().pose();
        float a = P;
        float b = 1.0F - P;
        switch (dir) {
            case DOWN -> quad(vc, m, a, 0.99F, b, a, 0.99F, a, b, 0.99F, a, b, 0.99F, b);
            case UP -> quad(vc, m, a, 0.01F, a, a, 0.01F, b, b, 0.01F, b, b, 0.01F, a);
            case WEST -> quad(vc, m, 0.99F, a, a, 0.99F, a, b, 0.99F, b, b, 0.99F, b, a);
            case EAST -> quad(vc, m, 0.01F, b, a, 0.01F, b, b, 0.01F, a, b, 0.01F, a, a);
            case NORTH -> quad(vc, m, a, a, 0.99F, a, b, 0.99F, b, b, 0.99F, b, a, 0.99F);
            case SOUTH -> quad(vc, m, a, b, 0.01F, a, a, 0.01F, b, a, 0.01F, b, b, 0.01F);
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float z0, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3) {
        vc.addVertex(m, x0, y0, z0);
        vc.addVertex(m, x1, y1, z1);
        vc.addVertex(m, x2, y2, z2);
        vc.addVertex(m, x3, y3, z3);
    }

    private static void translateFromOrientation(PoseStack pose, int orientation, float off) {
        if (orientation == 0) {
            pose.translate(0.0F, 1.0F, 1.0F);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        } else if (orientation == 1) {
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else if (orientation == 2) {
            pose.translate(0.0F, 0.0F, 1.0F);
        } else if (orientation == 3) {
            pose.translate(1.0F, 0.0F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        } else if (orientation == 4) {
            pose.translate(1.0F, 0.0F, 1.0F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        } else if (orientation == 5) {
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }
        pose.translate(0.0F, 0.0F, -off);
    }
}
