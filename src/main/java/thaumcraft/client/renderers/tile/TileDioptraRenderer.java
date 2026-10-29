package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.blocks.devices.BlockDioptra;
import thaumcraft.common.tiles.devices.TileDioptra;

/**
 * Рендер диоптры (порт TileDioptraRenderer): над блоком — рельеф 12×12 из карты ауры (высота = значение / 96) с текстурой
 * gridblock, по краям — «юбка» с прозрачным низом; вокруг — четыре стенки dioptra_side. Всё аддитивно, яркость ячейки
 * 200 ± 15 по волне от центра, стенки — 220. Цвет: Вис — голубоватый, Выброс — сиреневый, слегка мерцает.
 */
public class TileDioptraRenderer implements BlockEntityRenderer<TileDioptra> {
    private static final ResourceLocation GRID = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/gridblock.png");
    private static final ResourceLocation SIDE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/dioptra_side.png");
    private static final float[] ALPHAS = {0.9F, 0.9F, 0.9F, 0.9F};
    private static final float[] ALPHAS_X = {0.0F, 0.9F, 0.9F, 0.0F};
    private static final float[] ALPHAS_Z = {0.0F, 0.0F, 0.9F, 0.9F};

    public TileDioptraRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TileDioptra tco, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Entity view = Minecraft.getInstance().getCameraEntity();
        float t = view != null ? view.tickCount + partialTicks : 0.0F;
        float rc = Mth.sin(t / 12.0F) * 0.05F + 0.85F;
        float gc = tco.getBlockState().getValue(BlockDioptra.ENABLED) ? Mth.sin(t / 11.0F) * 0.05F + 0.9F : Mth.sin(t / 11.0F) * 0.05F + 0.45F;
        float bc = Mth.sin(t / 10.0F) * 0.05F + 0.95F;
        // calcColorMap без долей: у всех вершин (r·0.8, g, b)
        float cr = rc * 0.8F;
        float cg = Mth.clamp(gc, 0.0F, 1.0F);
        byte[] g = tco.grid_amt;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.pushPose();
        pose.translate(-0.495, 0.501, -0.495);
        pose.scale(0.99F, 1.0F, 0.99F);
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.glow(GRID));
        Matrix4f m = pose.last().pose();
        for (int a = 0; a < 12; a++) {
            for (int b = 0; b < 12; b++) {
                double d3 = a - 6;
                double d5 = b - 6;
                double dis = Math.sqrt(d3 * d3 + d5 * d5);
                float s = Mth.sin((float) ((tco.counter - dis * 10.0) / 8.0));
                int br = (int) (200.0F + s * 15.0F);
                quad(vc, m, new float[][] {
                        {a / 12.0F, g[a + b * 13] / 96.0F, b / 12.0F, 0.0F, 1.0F},
                        {(a + 1) / 12.0F, g[a + 1 + b * 13] / 96.0F, b / 12.0F, 1.0F, 1.0F},
                        {(a + 1) / 12.0F, g[a + 1 + (b + 1) * 13] / 96.0F, (b + 1) / 12.0F, 1.0F, 0.0F},
                        {a / 12.0F, g[a + (b + 1) * 13] / 96.0F, (b + 1) / 12.0F, 0.0F, 0.0F}}, true, br, cr, cg, bc, ALPHAS);
                if (a == 0) {
                    quad(vc, m, new float[][] {
                            {0.0F, 0.0F, b / 12.0F, 0.0F, 1.0F},
                            {0.0F, g[b * 13] / 96.0F, b / 12.0F, 1.0F, 1.0F},
                            {0.0F, g[(b + 1) * 13] / 96.0F, (b + 1) / 12.0F, 1.0F, 0.0F},
                            {0.0F, 0.0F, (b + 1) / 12.0F, 0.0F, 0.0F}}, true, br, cr, cg, bc, ALPHAS_X);
                }
                if (a == 11) {
                    quad(vc, m, new float[][] {
                            {1.0F, 0.0F, b / 12.0F, 0.0F, 1.0F},
                            {1.0F, g[a + 1 + b * 13] / 96.0F, b / 12.0F, 1.0F, 1.0F},
                            {1.0F, g[a + 1 + (b + 1) * 13] / 96.0F, (b + 1) / 12.0F, 1.0F, 0.0F},
                            {1.0F, 0.0F, (b + 1) / 12.0F, 0.0F, 0.0F}}, false, br, cr, cg, bc, ALPHAS_X);
                }
                if (b == 0) {
                    quad(vc, m, new float[][] {
                            {a / 12.0F, 0.0F, 0.0F, 0.0F, 1.0F},
                            {(a + 1) / 12.0F, 0.0F, 0.0F, 1.0F, 1.0F},
                            {(a + 1) / 12.0F, g[a + 1] / 96.0F, 0.0F, 1.0F, 0.0F},
                            {a / 12.0F, g[a] / 96.0F, 0.0F, 0.0F, 0.0F}}, true, br, cr, cg, bc, ALPHAS_Z);
                }
                if (b == 11) {
                    quad(vc, m, new float[][] {
                            {a / 12.0F, 0.0F, 1.0F, 0.0F, 1.0F},
                            {(a + 1) / 12.0F, 0.0F, 1.0F, 1.0F, 1.0F},
                            {(a + 1) / 12.0F, g[a + 1 + (b + 1) * 13] / 96.0F, 1.0F, 1.0F, 0.0F},
                            {a / 12.0F, g[a + (b + 1) * 13] / 96.0F, 1.0F, 0.0F, 0.0F}}, false, br, cr, cg, bc, ALPHAS_Z);
                }
            }
        }
        pose.popPose();
        // стенки: renderQuadCentered(dioptra_side, 1, rc, gc, bc, 220, аддитивно, 0.8)
        VertexConsumer sv = buffers.getBuffer(TCRenderTypes.glow(SIDE));
        pose.pushPose();
        pose.translate(0.0, 1.0, 0.0);
        pose.mulPose(Axis.ZP.rotationDegrees(270.0F));
        for (int q = 0; q < 4; q++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90.0F * q));
            pose.translate(0.0, 0.0, -0.5);
            Matrix4f sm = pose.last().pose();
            sv.addVertex(sm, -0.5F, 0.5F, 0.0F).setColor(rc, gc, bc, 0.8F).setUv(1.0F, 1.0F).setLight(220);
            sv.addVertex(sm, 0.5F, 0.5F, 0.0F).setColor(rc, gc, bc, 0.8F).setUv(1.0F, 0.0F).setLight(220);
            sv.addVertex(sm, 0.5F, -0.5F, 0.0F).setColor(rc, gc, bc, 0.8F).setUv(0.0F, 0.0F).setLight(220);
            sv.addVertex(sm, -0.5F, -0.5F, 0.0F).setColor(rc, gc, bc, 0.8F).setUv(0.0F, 1.0F).setLight(220);
            pose.popPose();
        }
        pose.popPose();
        pose.popPose();
    }

    /** TexturedQuadTC.draw с цветами и прозрачностями по вершинам: при flipFace порядок обратный, прозрачность остаётся у своей вершины. */
    private static void quad(VertexConsumer vc, Matrix4f m, float[][] v, boolean flip, int light, float r, float g, float b, float[] alphas) {
        for (int i = 0; i < 4; i++) {
            int idx = flip ? 3 - i : i;
            float[] p = v[idx];
            vc.addVertex(m, p[0], p[1], p[2]).setColor(r, g, b, alphas[idx]).setUv(p[3], p[4]).setLight(light);
        }
    }

    /** getRenderBoundingBox оригинала: блок ± 0.3, вверх до +2.3. */
    @Override
    public AABB getRenderBoundingBox(TileDioptra tile) {
        var p = tile.getBlockPos();
        return new AABB(p.getX() - 0.3, p.getY() - 0.3, p.getZ() - 0.3, p.getX() + 1.3, p.getY() + 2.3, p.getZ() + 1.3);
    }
}
