package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.Random;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.blocks.BlocksTC;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;

/**
 * Рендер рунической матрицы (порт TileInfusionMatrixRenderer): восемь кубиков (текстура по виду колонн), у включённой —
 * вращение, наклон, дрожание от нестабильности и аддитивное фиолетовое свечение; во время наполнения — ореол лучей.
 */
public class TileInfusionMatrixRenderer implements BlockEntityRenderer<TileInfusionMatrix> {
    private static final ResourceLocation TEX1 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/infuser_normal.png");
    private static final ResourceLocation TEX2 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/infuser_ancient.png");
    private static final ResourceLocation TEX3 = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/blocks/infuser_eldritch.png");
    private final ModelPart model;
    private final ModelPart modelOver;

    public TileInfusionMatrixRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = cube(0);
        this.modelOver = cube(32);
    }

    /** ModelCube(shift): куб 16×16×16 с центром в начале координат, текстура 64×64. */
    private static ModelPart cube(int shift) {
        MeshDefinition md = new MeshDefinition();
        md.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, shift).mirror().addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16), PartPose.ZERO);
        return LayerDefinition.create(md, 64, 64).bakeRoot().getChild("cube");
    }

    @Override
    public void render(TileInfusionMatrix is, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Entity view = Minecraft.getInstance().getCameraEntity();
        float ticks = (view != null ? view.tickCount : 0) + partialTicks;
        ResourceLocation t = TEX1;
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        float inst = 0.0F;
        int craftcount = 0;
        float startup = 0.0F;
        boolean active = false;
        boolean crafting = false;
        if (is.getLevel() != null) {
            pose.mulPose(Axis.YP.rotationDegrees(ticks % 360.0F * is.startUp));
            pose.mulPose(Axis.XP.rotationDegrees(35.0F * is.startUp));
            pose.mulPose(Axis.ZP.rotationDegrees(45.0F * is.startUp));
            BlockState bs = is.getLevel().getBlockState(is.getBlockPos().offset(-1, -2, -1));
            if (bs.is(BlocksTC.pillarAncient.get())) {
                t = TEX2;
            }
            if (bs.is(BlocksTC.pillarEldritch.get())) {
                t = TEX3;
            }
            inst = is.stability;
            craftcount = is.craftCount;
            startup = is.startUp;
            active = is.active;
            crafting = is.crafting;
        }
        float instability = Math.min(6.0F, 1.0F + (inst < 0.0F ? -inst * 0.66F : 1.0F) * (Math.min(craftcount, 50) / 50.0F));
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(t));
        this.cubes(pose, vc, light, ticks, startup, instability, active, false);
        if (active) {
            VertexConsumer vo = buffers.getBuffer(TCRenderTypes.additiveEntity(t));
            this.cubes(pose, vo, LightTexture.FULL_BRIGHT, ticks, startup, instability, true, true);
        }
        pose.popPose();
        if (crafting) {
            this.drawHalo(pose, buffers, craftcount);
        }
    }

    private void cubes(PoseStack pose, VertexConsumer vc, int light, float ticks, float startup, float instability, boolean active, boolean over) {
        for (int a = 0; a < 2; a++) {
            for (int b = 0; b < 2; b++) {
                for (int c = 0; c < 2; c++) {
                    float b1 = 0.0F;
                    float b2 = 0.0F;
                    float b3 = 0.0F;
                    if (active) {
                        b1 = Mth.sin((ticks + a * 10) / 15.0F) * 0.01F * startup * instability;
                        b2 = Mth.sin((ticks + b * 10) / 14.0F) * 0.01F * startup * instability;
                        b3 = Mth.sin((ticks + c * 10) / 13.0F) * 0.01F * startup * instability;
                    }
                    int aa = a == 0 ? -1 : 1;
                    int bb = b == 0 ? -1 : 1;
                    int cc = c == 0 ? -1 : 1;
                    pose.pushPose();
                    pose.translate(b1 + aa * 0.25F, b2 + bb * 0.25F, b3 + cc * 0.25F);
                    if (a > 0) {
                        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                    }
                    if (b > 0) {
                        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
                    }
                    if (c > 0) {
                        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    }
                    pose.scale(0.45F, 0.45F, 0.45F);
                    if (over) {
                        float alpha = (Mth.sin((ticks + a * 2 + b * 3 + c * 4) / 4.0F) * 0.1F + 0.2F) * startup;
                        int color = ((int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255) << 24) | ((int) (0.8F * 255) << 16) | ((int) (0.1F * 255) << 8) | 255;
                        this.modelOver.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
                    } else {
                        this.model.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
                    }
                    pose.popPose();
                }
            }
        }
    }

    /** drawHalo: лучи-пирамиды из центра со случайными поворотами (семя 245), длина растёт с craftCount. */
    private void drawHalo(PoseStack pose, MultiBufferSource buffers, int count) {
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        int q = Minecraft.getInstance().options.graphicsMode().get() == GraphicsStatus.FAST ? 10 : 20;
        float f1 = count / 500.0F;
        float f2 = 0.0F;
        Random random = new Random(245L);
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.HALO);
        for (int i = 0; i < q; i++) {
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.XP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(random.nextFloat() * 360.0F + f1 * 360.0F));
            float fa = random.nextFloat() * 20.0F + 5.0F + f2 * 10.0F;
            float f4 = random.nextFloat() * 2.0F + 1.0F + f2 * 2.0F;
            fa /= 20.0F / (Math.min(count, 50) / 50.0F);
            f4 /= 20.0F / (Math.min(count, 50) / 50.0F);
            Matrix4f m = pose.last().pose();
            int ca = (int) (255.0F * (1.0F - f1));
            // веер GL_TRIANGLE_FAN (центр + 4 точки) — три треугольника, каждый вырожденным четырёхугольником
            float[][] pts = {{-0.866F * f4, fa, -0.5F * f4}, {0.866F * f4, fa, -0.5F * f4}, {0.0F, fa, 1.0F * f4}, {-0.866F * f4, fa, -0.5F * f4}};
            for (int k = 0; k < 3; k++) {
                vc.addVertex(m, 0.0F, 0.0F, 0.0F).setColor(255, 255, 255, Mth.clamp(ca, 0, 255));
                vc.addVertex(m, pts[k][0], pts[k][1], pts[k][2]).setColor(255, 0, 255, 0);
                vc.addVertex(m, pts[k + 1][0], pts[k + 1][1], pts[k + 1][2]).setColor(255, 0, 255, 0);
                vc.addVertex(m, pts[k + 1][0], pts[k + 1][1], pts[k + 1][2]).setColor(255, 0, 255, 0);
            }
        }
        pose.popPose();
    }

    /** getRenderBoundingBox оригинала: блок с запасом 0.1 (ореол — в нём же, как в оригинале). */
    @Override
    public AABB getRenderBoundingBox(TileInfusionMatrix tile) {
        return new AABB(tile.getBlockPos()).inflate(0.1);
    }
}
