package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.SpiderEyesLayer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityMindSpider;

/**
 * Паук разума (порт RenderMindSpider): ванильный паук ×0.6 с горящими глазами, тело почти прозрачное (не больше 0.1),
 * «ненастоящего» видит только его зритель.
 */
public class RenderMindSpider extends MobRenderer<EntityMindSpider, RenderMindSpider.FadedSpiderModel> {
    private static final ResourceLocation TEX = ResourceLocation.withDefaultNamespace("textures/entity/spider/spider.png");

    public RenderMindSpider(EntityRendererProvider.Context ctx) {
        super(ctx, new FadedSpiderModel(ctx.bakeLayer(ModelLayers.SPIDER)), 0.5F);
        // глаза — без прозрачности тела (в 1.12 цвет сбрасывался перед слоями)
        this.addLayer(new SpiderEyesLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityMindSpider e, float ls, float la, float pt, float age, float yaw, float pitch) {
                float a = RenderMindSpider.this.model.alpha;
                RenderMindSpider.this.model.alpha = 1.0F;
                super.render(pose, buffers, light, e, ls, la, pt, age, yaw, pitch);
                RenderMindSpider.this.model.alpha = a;
            }
        });
    }

    @Override
    public void render(EntityMindSpider entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        String viewer = entity.getViewer();
        var cam = Minecraft.getInstance().getCameraEntity();
        if (viewer.isEmpty() || cam != null && viewer.equals(cam.getName().getString())) {
            this.model.alpha = Math.min(0.1F, entity.tickCount / 100.0F);
            super.render(entity, yaw, pt, pose, buffers, light);
        }
    }

    @Override
    protected void scale(EntityMindSpider entity, PoseStack pose, float pt) {
        pose.scale(0.6F, 0.6F, 0.6F);
    }

    @Override
    protected RenderType getRenderType(EntityMindSpider entity, boolean visible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEX);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityMindSpider entity) {
        return TEX;
    }

    /** Модель паука с общей прозрачностью тела (слой глаз рисуется отдельно, без неё). */
    public static class FadedSpiderModel extends SpiderModel<EntityMindSpider> {
        float alpha = 1.0F;

        public FadedSpiderModel(ModelPart root) {
            super(root);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            int a = (int) ((color >>> 24) * this.alpha);
            super.renderToBuffer(pose, vc, light, overlay, (a << 24) | (color & 0xFFFFFF));
        }
    }
}
