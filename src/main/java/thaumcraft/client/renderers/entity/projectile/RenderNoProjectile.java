package thaumcraft.client.renderers.entity.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.projectile.EntityFocusProjectile;

/** Снаряд фокуса без модели (порт RenderNoProjectile): при отрисовке кадра рождает частицы, не чаще чем через 0,2 тика. */
public class RenderNoProjectile extends EntityRenderer<EntityFocusProjectile> {
    public RenderNoProjectile(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.1F;
    }

    @Override
    public void render(EntityFocusProjectile gp, float entityYaw, float fq, PoseStack pose, MultiBufferSource buffer, int light) {
        float qq = fq - gp.lastRenderTick;
        if (qq < 0.0F) {
            qq++;
        }
        if (qq > 0.2) {
            gp.renderParticle(fq);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFocusProjectile entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
