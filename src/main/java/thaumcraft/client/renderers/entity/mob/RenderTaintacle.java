package thaumcraft.client.renderers.entity.mob;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelTaintacle;
import thaumcraft.common.entities.monster.tainted.EntityTaintacle;

/** Заражённое щупальце (порт RenderTaintacle): модель длиной 10 (обычное), 6 (усик) или 14 (гигантское), прозрачность. */
public class RenderTaintacle extends MobRenderer<EntityTaintacle, ModelTaintacle> {
    public static final ModelLayerLocation LAYER_10 = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "taintacle"), "10");
    public static final ModelLayerLocation LAYER_6 = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "taintacle"), "6");
    public static final ModelLayerLocation LAYER_14 = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "taintacle"), "14");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/taintacle.png");

    public RenderTaintacle(EntityRendererProvider.Context ctx, float shadow, ModelLayerLocation layer, int length) {
        super(ctx, new ModelTaintacle(ctx.bakeLayer(layer), length), shadow);
    }

    public static RenderTaintacle normal(EntityRendererProvider.Context ctx) {
        return new RenderTaintacle(ctx, 0.6F, LAYER_10, 10);
    }

    public static RenderTaintacle small(EntityRendererProvider.Context ctx) {
        return new RenderTaintacle(ctx, 0.2F, LAYER_6, 6);
    }

    public static RenderTaintacle giant(EntityRendererProvider.Context ctx) {
        return new RenderTaintacle(ctx, 1.0F, LAYER_14, 14);
    }

    @Override
    protected RenderType getRenderType(EntityTaintacle entity, boolean visible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEX);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTaintacle entity) {
        return TEX;
    }
}
