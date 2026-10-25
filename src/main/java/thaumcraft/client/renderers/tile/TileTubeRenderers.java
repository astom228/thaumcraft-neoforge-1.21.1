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
import org.joml.Quaternionf;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.common.tiles.essentia.TileTubeBuffer;
import thaumcraft.common.tiles.essentia.TileTubeOneway;
import thaumcraft.common.tiles.essentia.TileTubeValve;

/**
 * Рендеры трубок (порт TileTubeValveRenderer / TileTubeOnewayRenderer / TileTubeBufferRenderer): маховик клапана, стрелка
 * направленной трубки, заглушки буфера. Модель ModelTubeValve (стержень и кольцо) собрана из тех же кубов.
 */
public final class TileTubeRenderers {
    static final ResourceLocation TEX_VALVE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/valve.png");

    private TileTubeRenderers() {
    }

    static final class ValveModel {
        final ModelPart rod;
        final ModelPart ring;

        ValveModel() {
            MeshDefinition md = new MeshDefinition();
            md.getRoot().addOrReplaceChild("rod", CubeListBuilder.create().texOffs(0, 10).addBox(-1.0F, 2.0F, -1.0F, 2, 2, 2), PartPose.ZERO);
            md.getRoot().addOrReplaceChild("ring", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, 4.0F, -2.0F, 4, 1, 4), PartPose.ZERO);
            ModelPart root = LayerDefinition.create(md, 64, 32).bakeRoot();
            this.rod = root.getChild("rod");
            this.ring = root.getChild("ring");
        }
    }

    /** glRotatef(угол, x, y, z) оригинала. */
    static void rotate(PoseStack pose, float deg, float x, float y, float z) {
        if (x == 0.0F && y == 0.0F && z == 0.0F) {
            return;
        }
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(deg), x / len, y / len, z / len));
    }

    /** Общий поворот модели «по стороне» fd, как в оригинале. */
    static void orient(PoseStack pose, Direction fd) {
        if (fd.getStepY() == 0) {
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        } else {
            rotate(pose, 90.0F, -1.0F, 0.0F, 0.0F);
            rotate(pose, 90.0F, fd.getStepY(), 0.0F, 0.0F);
        }
        rotate(pose, 90.0F, fd.getStepX(), fd.getStepY(), fd.getStepZ());
    }

    static int rgb(float r, float g, float b) {
        return 0xFF000000 | (int) (r * 255) << 16 | (int) (g * 255) << 8 | (int) (b * 255);
    }

    public static class Valve implements BlockEntityRenderer<TileTubeValve> {
        private final ValveModel model = new ValveModel();

        public Valve(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(TileTubeValve valve, float f, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_VALVE));
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            orient(pose, valve.facing);
            pose.mulPose(Axis.YP.rotationDegrees(-valve.rotation * 1.5F));
            pose.translate(0.0, -0.03F - valve.rotation / 360.0F * 0.09F, 0.0);
            pose.pushPose();
            this.model.ring.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
            pose.scale(0.75F, 1.0F, 0.75F);
            this.model.rod.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
            pose.popPose();
            pose.popPose();
        }
    }

    public static class Oneway implements BlockEntityRenderer<TileTubeOneway> {
        private final ValveModel model = new ValveModel();

        public Oneway(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(TileTubeOneway valve, float f, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (valve.getLevel() != null && ThaumcraftApiHelper.getConnectableTile(valve.getLevel(), valve.getBlockPos(), valve.facing.getOpposite()) == null) {
                return;
            }
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_VALVE));
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            orient(pose, valve.facing);
            pose.pushPose();
            pose.scale(2.0F, 2.0F, 2.0F);
            pose.translate(0.0, -0.32F, 0.0);
            this.model.rod.render(pose, vc, light, OverlayTexture.NO_OVERLAY, rgb(0.45F, 0.5F, 1.0F));
            pose.popPose();
            pose.popPose();
        }
    }

    public static class Buffer implements BlockEntityRenderer<TileTubeBuffer> {
        private final ValveModel model = new ValveModel();

        public Buffer(BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(TileTubeBuffer buffer, float f, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (buffer.getLevel() == null) {
                return;
            }
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_VALVE));
            for (Direction dir : Direction.values()) {
                if (buffer.chokedSides[dir.ordinal()] != 0 && buffer.openSides[dir.ordinal()]
                        && ThaumcraftApiHelper.getConnectableTile(buffer.getLevel(), buffer.getBlockPos(), dir) != null) {
                    pose.pushPose();
                    pose.translate(0.5, 0.5, 0.5);
                    orient(pose, dir.getOpposite());
                    pose.pushPose();
                    int color = buffer.chokedSides[dir.ordinal()] == 2 ? rgb(1.0F, 0.3F, 0.3F) : rgb(0.3F, 0.3F, 1.0F);
                    pose.scale(2.0F, 1.0F, 2.0F);
                    pose.translate(0.0, -0.5, 0.0);
                    this.model.rod.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
                    pose.popPose();
                    pose.popPose();
                }
            }
        }
    }
}
