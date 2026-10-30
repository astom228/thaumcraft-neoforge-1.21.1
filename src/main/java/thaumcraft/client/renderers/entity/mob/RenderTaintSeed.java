package thaumcraft.client.renderers.entity.mob;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelTaintSeed;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;

/** Рендер семени порчи (порт RenderTaintSeed): модель с прозрачностью, без отсечения граней, тень 0.4. */
public class RenderTaintSeed extends MobRenderer<EntityTaintSeed, ModelTaintSeed> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "taint_seed"), "main");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/taintseed.png");

    public RenderTaintSeed(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelTaintSeed(ctx.bakeLayer(LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTaintSeed entity) {
        return TEX;
    }

    @Override
    protected RenderType getRenderType(EntityTaintSeed entity, boolean visible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(TEX);
    }
}
