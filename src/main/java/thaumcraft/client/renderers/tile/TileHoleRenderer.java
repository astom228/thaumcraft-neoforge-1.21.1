package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.common.tiles.misc.TileHole;

/**
 * Дыра «Разлома» (порт TileHoleRenderer): на гранях, где соседний блок непрозрачен, — звёздная плоскость. В 1.12 —
 * свой шейдер «конца» с текстурой end_portal; здесь — ванильный шейдер портала Края (как у зеркал, DECISIONS 90).
 */
public class TileHoleRenderer implements BlockEntityRenderer<TileHole> {
    public TileHoleRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileHole te, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (te.getLevel() == null) {
            return;
        }
        VertexConsumer vc = buffers.getBuffer(RenderType.endPortal());
        Matrix4f m = pose.last().pose();
        BlockPos pos = te.getBlockPos();
        float o = 0.001F;
        for (Direction face : Direction.values()) {
            BlockPos np = pos.relative(face);
            BlockState bs = te.getLevel().getBlockState(np);
            if (!bs.isSolidRender(te.getLevel(), np) || bs.getBlock() == BlocksTC.hole.get()) {
                continue;
            }
            // плоскость на 0.499 от центра в сторону соседа (UtilsFX.renderQuadCentered), видна изнутри дыры
            switch (face) {
                case DOWN -> quad(vc, m, 0, o, 0, 1, o, 0, 1, o, 1, 0, o, 1);
                case UP -> quad(vc, m, 0, 1 - o, 0, 0, 1 - o, 1, 1, 1 - o, 1, 1, 1 - o, 0);
                case NORTH -> quad(vc, m, 0, 0, o, 0, 1, o, 1, 1, o, 1, 0, o);
                case SOUTH -> quad(vc, m, 0, 0, 1 - o, 1, 0, 1 - o, 1, 1, 1 - o, 0, 1, 1 - o);
                case WEST -> quad(vc, m, o, 0, 0, o, 0, 1, o, 1, 1, o, 1, 0);
                case EAST -> quad(vc, m, 1 - o, 0, 0, 1 - o, 1, 0, 1 - o, 1, 1, 1 - o, 0, 1);
            }
        }
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
                             float x4, float y4, float z4) {
        vc.addVertex(m, x1, y1, z1);
        vc.addVertex(m, x2, y2, z2);
        vc.addVertex(m, x3, y3, z3);
        vc.addVertex(m, x4, y4, z4);
    }
}
