package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.entities.monster.cult.EntityCultistCleric;

import java.util.Random;

/**
 * Рендер культистов и претора (порт RenderCultist / RenderCultistLeader): человеческая модель 1.12 (текстура 64×32),
 * броня и предмет в руке; претор крупнее (×1.15). Клирик-ритуалист покачивается в воздухе, от него к алтарю тянется
 * колышущаяся красная лента (wispy).
 */
public class RenderCultist<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "cultist"), "main");
    private static final ResourceLocation SKIN = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/cultist.png");
    private static final ResourceLocation WISPY = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/wispy.png");
    private final float scale;

    public RenderCultist(EntityRendererProvider.Context ctx, float scale) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(LAYER)), 0.5F);
        this.scale = scale;
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    public static <T extends Mob> RenderCultist<T> cultist(EntityRendererProvider.Context ctx) {
        return new RenderCultist<>(ctx, 1.0F);
    }

    public static <T extends Mob> RenderCultist<T> leader(EntityRendererProvider.Context ctx) {
        return new RenderCultist<>(ctx, 1.15F);
    }

    /** ModelBiped 1.12: сетка 64×32. */
    public static LayerDefinition createLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32);
    }

    @Override
    protected void scale(T entity, PoseStack pose, float pt) {
        if (this.scale != 1.0F) {
            pose.scale(this.scale, this.scale, this.scale);
        }
    }

    @Override
    public void render(T entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        float bob = 0.0F;
        boolean rit = entity instanceof EntityCultistCleric c && c.getIsRitualist() && c.hasRestriction();
        if (rit) {
            int val = new Random(entity.getId()).nextInt(1000);
            float c = entity.tickCount + pt + val;
            bob = Mth.sin(c / 9.0F) * 0.1F + 0.21F;
            pose.translate(0.0, bob, 0.0);
        }
        super.render(entity, yaw, pt, pose, buffers, light);
        if (rit) {
            BlockPos home = entity.getRestrictCenter();
            double ex = Mth.lerp(pt, entity.xo, entity.getX());
            double ey = Mth.lerp(pt, entity.yo, entity.getY());
            double ez = Mth.lerp(pt, entity.zo, entity.getZ());
            this.drawFloatyLine(pose, buffers, ex, ey, ez, entity.getX(), entity.getY() + entity.getEyeHeight() * 1.2F, entity.getZ(), home.getX() + 0.5,
                    home.getY() + 1.5 - bob, home.getZ() + 0.5, 0x110011, -0.03F, Math.min(entity.tickCount, 10) / 10.0F, 0.25F);
        }
        pose.popPose();
    }

    /** drawFloatyLine оригинала: две перекрещенные ленты-полосы от алтаря (x2…) к ритуалисту (x…). */
    private void drawFloatyLine(PoseStack pose, MultiBufferSource buffers, double ox, double oy, double oz, double x, double y, double z, double x2, double y2,
            double z2, int color, float speed, float distance, float width) {
        pose.pushPose();
        pose.translate(x2 - ox, y2 - oy, z2 - oz);
        float time = (float) (System.nanoTime() / 30000000L);
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        double dc1x = (float) (x - x2);
        double dc1y = (float) (y - y2);
        double dc1z = (float) (z - z2);
        float dist = Mth.sqrt((float) (dc1x * dc1x + dc1y * dc1y + dc1z * dc1z));
        float length = Math.round(dist) * 6.0F;
        if (length <= 0.0F) {
            pose.popPose();
            return;
        }
        int n = (int) (length * distance);
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.tube(WISPY));
        Matrix4f m = pose.last().pose();
        for (int pass = 0; pass < 2; pass++) {
            float[] prev = null;
            for (int i = 0; i <= n; i++) {
                float f2 = i / length;
                float f3 = 1.0F - Math.abs(i - length / 2.0F) / (length / 2.0F);
                double dx = dc1x + Mth.sin((float) ((z % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 4.0)) * 0.5F * f3;
                double dy = dc1y + Mth.sin((float) ((x % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 3.0)) * 0.5F * f3;
                double dz = dc1z + Mth.sin((float) ((y % 16.0 + dist * (1.0F - f2) * 6.0F - time % 32767.0F / 5.0F) / 2.0)) * 0.5F * f3;
                float f13 = (1.0F - f2) * dist - time * speed;
                float px = (float) (dx * f2);
                float py = (float) (dy * f2);
                float pz = (float) (dz * f2);
                // полоса оригинала (GL_TRIANGLE_STRIP): пара вершин на шаг; здесь — четырёхугольники между соседними парами
                float[] cur = pass == 0 ? new float[] {px, py - width, pz, px, py + width, pz, f13} : new float[] {px - width, py, pz, px + width, py, pz, f13};
                if (prev != null) {
                    vc.addVertex(m, prev[0], prev[1], prev[2]).setUv(prev[6], 1.0F).setColor(r, g, b, 0.8F);
                    vc.addVertex(m, prev[3], prev[4], prev[5]).setUv(prev[6], 0.0F).setColor(r, g, b, 0.8F);
                    vc.addVertex(m, cur[3], cur[4], cur[5]).setUv(cur[6], 0.0F).setColor(r, g, b, 0.8F);
                    vc.addVertex(m, cur[0], cur[1], cur[2]).setUv(cur[6], 1.0F).setColor(r, g, b, 0.8F);
                }
                prev = cur;
            }
        }
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return SKIN;
    }
}
