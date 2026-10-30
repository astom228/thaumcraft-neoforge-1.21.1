package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelEldritchGolem;
import thaumcraft.common.entities.monster.boss.EntityEldritchGolem;

/** Рендер потустороннего голема (порт RenderEldritchGolem): масштаб 1.7, со смешиванием по прозрачности текстуры. */
public class RenderEldritchGolem extends MobRenderer<EntityEldritchGolem, ModelEldritchGolem> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch_golem"), "main");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/eldritch_golem.png");

    public RenderEldritchGolem(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelEldritchGolem(ctx.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    protected void scale(EntityEldritchGolem entity, PoseStack pose, float pt) {
        pose.scale(1.7F, 1.7F, 1.7F);
    }

    @Override
    protected RenderType getRenderType(EntityEldritchGolem entity, boolean visible, boolean translucent, boolean glowing) {
        return visible ? RenderType.entityTranslucent(TEX) : super.getRenderType(entity, visible, translucent, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityEldritchGolem entity) {
        return TEX;
    }
}
