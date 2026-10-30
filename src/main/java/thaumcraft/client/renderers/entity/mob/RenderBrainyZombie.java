package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.monster.EntityGiantBrainyZombie;

/**
 * Злобный/яростный зомби (порт RenderBrainyZombie): ванильная модель зомби со своей текстурой; яростный растёт
 * (×(1 + злость)) и краснеет. Тот же рендер (со своей текстурой) — у одержимого зомби (RenderInhabitedZombie).
 */
public class RenderBrainyZombie extends AbstractZombieRenderer<Zombie, RenderBrainyZombie.TintZombieModel> {
    public static final ResourceLocation BRAINY = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/bzombie.png");
    public static final ResourceLocation INHABITED = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/czombie.png");
    private final ResourceLocation texture;

    public RenderBrainyZombie(EntityRendererProvider.Context ctx) {
        this(ctx, BRAINY);
    }

    public RenderBrainyZombie(EntityRendererProvider.Context ctx, ResourceLocation texture) {
        super(ctx, new TintZombieModel(ctx.bakeLayer(ModelLayers.ZOMBIE)), new TintZombieModel(ctx.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new TintZombieModel(ctx.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)));
        this.texture = texture;
    }

    public static RenderBrainyZombie inhabited(EntityRendererProvider.Context ctx) {
        return new RenderBrainyZombie(ctx, INHABITED);
    }

    @Override
    public void render(Zombie entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        int tint = -1;
        if (entity instanceof EntityGiantBrainyZombie z) {
            float q = Math.min(1.0F, z.getAnger()) / 2.0F;
            tint = 0xFF000000 | 255 << 16 | (int) ((1.0F - q) * 255) << 8 | (int) ((1.0F - q) * 255);
        }
        this.model.tint = tint;
        super.render(entity, yaw, pt, pose, buffers, light);
        this.model.tint = -1;
    }

    @Override
    protected void scale(Zombie entity, PoseStack pose, float pt) {
        if (entity instanceof EntityGiantBrainyZombie z) {
            float s = 1.0F + z.getAnger();
            pose.scale(s, s, s);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie entity) {
        return this.texture;
    }

    /** Модель зомби с множителем цвета (glColor3f оригинала). */
    public static class TintZombieModel extends ZombieModel<Zombie> {
        int tint = -1;

        public TintZombieModel(ModelPart root) {
            super(root);
        }

        @Override
        public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
            if (this.tint != -1) {
                int r = (color >> 16 & 255) * (this.tint >> 16 & 255) / 255;
                int g = (color >> 8 & 255) * (this.tint >> 8 & 255) / 255;
                int b = (color & 255) * (this.tint & 255) / 255;
                color = color & 0xFF000000 | r << 16 | g << 8 | b;
            }
            super.renderToBuffer(pose, vc, light, overlay, color);
        }
    }
}
