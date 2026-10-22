package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;
import thaumcraft.common.tiles.crafting.TileCrucible;

/**
 * Поверхность жидкости в тигле (порт TileCrucibleRenderer): текстура воды на высоте getFluidHeight, чем больше аспектов —
 * тем сильнее уходит в фиолетовый. В 1.12 текстура воды была синей, в 1.21 она серая и окрашивается цветом воды биома —
 * поэтому оттенок оригинала умножается на него (DECISIONS.md).
 */
public class TileCrucibleRenderer implements BlockEntityRenderer<TileCrucible> {

    public TileCrucibleRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TileCrucible cr, float partialTicks, PoseStack pose, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (cr.tank.getFluidAmount() > 0) {
            this.renderFluid(cr, pose, buffers, packedLight);
        }
    }

    public void renderFluid(TileCrucible cr, PoseStack pose, MultiBufferSource buffers, int light) {
        TextureAtlasSprite icon = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.WATER.defaultBlockState());
        float recolor = cr.aspects.visSize() / 500.0F;
        if (recolor > 0.0F) {
            recolor = 0.5F + recolor / 2.0F;
        }
        if (recolor > 1.0F) {
            recolor = 1.0F;
        }
        int water = cr.getLevel() != null ? BiomeColors.getAverageWaterColor(cr.getLevel(), cr.getBlockPos()) : 0x3F76E4;
        // renderQuadFromIcon(icon, масштаб 1, красный 1 - r/3, зелёный 1 - r, синий 1 - r/2, свет, смешивание 771, непрозрачность 1)
        float r = (1.0F - recolor / 3.0F) * ((water >> 16 & 255) / 255.0F);
        float g = (1.0F - recolor) * ((water >> 8 & 255) / 255.0F);
        float b = (1.0F - recolor / 2.0F) * ((water & 255) / 255.0F);
        float h = cr.getFluidHeight();
        float f1 = icon.getU1();
        float f2 = icon.getV0();
        float f3 = icon.getU0();
        float f4 = icon.getV1();
        VertexConsumer vc = buffers.getBuffer(RenderType.translucent());
        Matrix4f m = pose.last().pose();
        // квадрат (0,0)-(1,1) после поворота на -90° вокруг X и сдвига z+1 оригинала
        vc.addVertex(m, 0.0F, h, 1.0F).setColor(r, g, b, 1.0F).setUv(f1, f4).setLight(light).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
        vc.addVertex(m, 1.0F, h, 1.0F).setColor(r, g, b, 1.0F).setUv(f3, f4).setLight(light).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
        vc.addVertex(m, 1.0F, h, 0.0F).setColor(r, g, b, 1.0F).setUv(f3, f2).setLight(light).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
        vc.addVertex(m, 0.0F, h, 0.0F).setColor(r, g, b, 1.0F).setUv(f1, f2).setLight(light).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
    }
}
