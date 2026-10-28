package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.Color;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.fx.ParticleEngine;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.tiles.crafting.TileFocalManipulator;

/**
 * Фокусный манипулятор (порт TileFocalManipulatorRenderer): фокус парит и вращается над столом; во время работы вокруг
 * кружат вис-кристаллы платы — у каждого свечение (кадры 320–335 particles.png, аддитивно) и по два луча цвета аспекта.
 */
public class TileFocalManipulatorRenderer implements BlockEntityRenderer<TileFocalManipulator> {
    public TileFocalManipulatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TileFocalManipulator table, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Entity view = Minecraft.getInstance().getCameraEntity();
        if (table.getLevel() == null || view == null) {
            return;
        }
        float ticks = view.tickCount + pt;
        ItemStack focus = table.getStackInSlot(0);
        if (!focus.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5F, 0.8F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(ticks % 360.0F));
            float hover = Mth.sin(ticks / 14.0F) * 0.2F + 0.2F;
            this.renderFloating(focus, hover, pose, buffers, light, table);
            pose.popPose();
        }
        Aspect[] aspects = table.crystalsSync.getAspects();
        int q = aspects.length;
        if (q <= 0) {
            return;
        }
        float ang = 360 / q;
        for (int a = 0; a < q; a++) {
            float angle = ticks % 720.0F / 2.0F + ang * a;
            float bob = Mth.sin((ticks + a * 10) / 12.0F) * 0.02F + 0.02F;
            Color c = new Color(aspects[a].getColor());
            float r = c.getRed() / 255.0F;
            float g = c.getGreen() / 255.0F;
            float b = c.getBlue() / 255.0F;
            // свечение
            pose.pushPose();
            pose.translate(0.5F, 1.3F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(0.0F, bob, 0.4F);
            pose.mulPose(Axis.YP.rotationDegrees(-angle));
            pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            this.billboard(pose, buffers.getBuffer(TCRenderTypes.additiveEntity(ParticleEngine.particleTexture)), 0.175F,
                    320 + view.tickCount % 16, r, g, b, 0.66F);
            pose.popPose();
            // кристалл и лучи
            pose.pushPose();
            pose.translate(0.5F, 1.05F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(angle));
            pose.translate(0.0F, bob, 0.4F);
            pose.scale(0.5F, 0.5F, 0.5F);
            this.renderRay(pose, buffers, angle, a, bob, r, g, b, ticks);
            this.renderRay(pose, buffers, angle, (a + 1) * 5, bob, r, g, b, ticks);
            this.renderFloating(ThaumcraftApiHelper.makeCrystal(aspects[a]), 0.0F, pose, buffers, light, table);
            pose.popPose();
        }
    }

    /** Как EntityItem с возрастом 0 и данным hoverStart: подъём sin(h)·0.1 + 0.1, поворот на h радиан. */
    private void renderFloating(ItemStack is, float hover, PoseStack pose, MultiBufferSource buffers, int light, TileFocalManipulator table) {
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(is, table.getLevel(), null, 0);
        float groundY = model.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
        pose.pushPose();
        pose.translate(0.0F, Mth.sin(hover) * 0.1F + 0.1F + 0.25F * groundY, 0.0F);
        pose.mulPose(Axis.YP.rotation(hover));
        itemRenderer.render(is, ItemDisplayContext.GROUND, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }

    /** UtilsFX.renderBillboardQuad: квадрат кадра frame сетки 64×64. */
    private void billboard(PoseStack pose, VertexConsumer vc, float size, int frame, float r, float g, float b, float a) {
        float u0 = frame % 64 / 64.0F;
        float u1 = u0 + 1.0F / 64.0F;
        float v0 = frame / 64 / 64.0F;
        float v1 = v0 + 1.0F / 64.0F;
        Matrix4f m = pose.last().pose();
        var n = pose.last();
        vc.addVertex(m, -size, size, 0).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(n, 0, 0, 1);
        vc.addVertex(m, size, size, 0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(n, 0, 0, 1);
        vc.addVertex(m, size, -size, 0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(n, 0, 0, 1);
        vc.addVertex(m, -size, -size, 0).setColor(r, g, b, a).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0).setNormal(n, 0, 0, 1);
    }

    /** Луч-веер (GL_TRIANGLE_FAN из 5 вершин → три треугольника вырожденными четырёхугольниками), аддитивно. */
    private void renderRay(PoseStack pose, MultiBufferSource buffers, float angle, int num, float lift, float r, float g, float b, float ticks) {
        Random random = new Random(187L + num * num);
        float pan = Mth.sin((ticks + num * 10) / 15.0F) * 15.0F;
        float aparture = Mth.sin((ticks + num * 10) / 14.0F) * 2.0F;
        pose.pushPose();
        pose.translate(0.0F, 0.475F + lift, 0.0F);
        pose.mulPose(Axis.XN.rotationDegrees(90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360.0F));
        pose.mulPose(Axis.XP.rotationDegrees(pan));
        float fa = random.nextFloat() * 20.0F + 10.0F;
        float f4 = random.nextFloat() * 4.0F + 6.0F + aparture;
        fa /= 30.0F / (Math.min(ticks, 10.0F) / 10.0F);
        f4 /= 30.0F / (Math.min(ticks, 10.0F) / 10.0F);
        Matrix4f m = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.HALO);
        float[][] rim = {{-0.8F * f4, fa, -0.5F * f4}, {0.8F * f4, fa, -0.5F * f4}, {0.0F, fa, 1.0F * f4}, {-0.8F * f4, fa, -0.5F * f4}};
        for (int i = 0; i < 3; i++) {
            vc.addVertex(m, 0, 0, 0).setColor(r, g, b, 0.66F);
            vc.addVertex(m, rim[i][0], rim[i][1], rim[i][2]).setColor(r, g, b, 0.0F);
            vc.addVertex(m, rim[i + 1][0], rim[i + 1][1], rim[i + 1][2]).setColor(r, g, b, 0.0F);
            vc.addVertex(m, rim[i + 1][0], rim[i + 1][1], rim[i + 1][2]).setColor(r, g, b, 0.0F);
        }
        pose.popPose();
    }
}
