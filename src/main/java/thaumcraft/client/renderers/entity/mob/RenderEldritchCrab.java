package thaumcraft.client.renderers.entity.mob;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelEldritchCrab;
import thaumcraft.common.entities.monster.EntityEldritchCrab;

/** Потусторонний краб (порт RenderEldritchCrab): своя модель и текстура, тень 1.0. */
public class RenderEldritchCrab extends MobRenderer<EntityEldritchCrab, ModelEldritchCrab> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch_crab"), "main");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crab.png");

    public RenderEldritchCrab(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelEldritchCrab(ctx.bakeLayer(LAYER)), 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEldritchCrab entity) {
        return TEX;
    }
}
