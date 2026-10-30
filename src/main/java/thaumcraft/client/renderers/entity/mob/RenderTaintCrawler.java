package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.tainted.EntityTaintCrawler;

/** Заражённый ползун (порт RenderTaintCrawler): модель чешуйницы ×0.7 со своей текстурой, при смерти переворачивается на 180°. */
public class RenderTaintCrawler extends MobRenderer<EntityTaintCrawler, SilverfishModel<EntityTaintCrawler>> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crawler.png");

    public RenderTaintCrawler(EntityRendererProvider.Context ctx) {
        super(ctx, new SilverfishModel<>(ctx.bakeLayer(ModelLayers.SILVERFISH)), 0.2F);
    }

    @Override
    protected float getFlipDegrees(EntityTaintCrawler entity) {
        return 180.0F;
    }

    @Override
    protected void scale(EntityTaintCrawler entity, PoseStack pose, float pt) {
        pose.scale(0.7F, 0.7F, 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTaintCrawler entity) {
        return TEX;
    }
}
