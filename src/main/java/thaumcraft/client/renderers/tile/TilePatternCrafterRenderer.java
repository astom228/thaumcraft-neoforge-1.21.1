package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.blocks.crafting.BlockPatternCrafter;
import thaumcraft.common.tiles.crafting.TilePatternCrafter;

/**
 * Рендер шаблонного крафтера (порт TilePatternCrafterRenderer): на передней стороне — значок текущего шаблона (кадр из 10
 * в pattern_crafter_modes), под ним две латунные шестерёнки, вращающиеся навстречу друг другу.
 */
public class TilePatternCrafterRenderer implements BlockEntityRenderer<TilePatternCrafter> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/pattern_crafter_modes.png");
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/gear_brass.png");

    public TilePatternCrafterRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TilePatternCrafter pc, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 0.75F, 0.5F);
        switch (pc.getBlockState().getValue(BlockPatternCrafter.FACING)) {
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        // значок шаблона: renderQuadCentered(TEX, 10, 1, type, 0.5, …) — кадр type из 10 по горизонтали
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        pose.translate(0.0F, 0.0F, -0.5F);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));
        float f1 = pc.type / 10.0F;
        float f2 = f1 + 0.1F;
        float h = 0.25F;
        Matrix4f m = pose.last().pose();
        vc.addVertex(m, -h, h, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f2, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, 1);
        vc.addVertex(m, h, h, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f2, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, 1);
        vc.addVertex(m, h, -h, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f1, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, 1);
        vc.addVertex(m, -h, -h, 0.0F).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(f1, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, 1);
        pose.popPose();
        VertexConsumer gear = buffers.getBuffer(RenderType.entityCutoutNoCull(ICON));
        for (int side = -1; side <= 1; side += 2) {
            pose.pushPose();
            pose.translate(0.2F * side, -0.40625F, 0.05F);
            pose.mulPose(Axis.ZP.rotationDegrees(side * pc.rot % 360.0F));
            pose.scale(0.5F, 0.5F, 1.0F);
            pose.translate(-0.5F, -0.5F, 0.0F);
            UtilsFX.renderTextureIn3D(pose, gear, 1.0F, 0.0F, 0.0F, 1.0F, 16, 16, 0.1F, light);
            pose.popPose();
        }
        pose.popPose();
    }
}
