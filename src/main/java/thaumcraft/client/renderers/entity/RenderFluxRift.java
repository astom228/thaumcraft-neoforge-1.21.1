package thaumcraft.client.renderers.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.client.lib.PolyCone;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.entities.EntityFluxRift;
import thaumcraft.common.lib.utils.EntityUtils;

/**
 * Рендер разлома Выброса (порт RenderFluxRift): трубка PolyCone (6 сторон) по точкам разлома, покачивается и «дышит»
 * тем сильнее, чем ниже стабильность. Четыре прохода: три светящихся (радиус ×1.25, ×1.75, ×2.25) и сердцевина.
 */
public class RenderFluxRift extends EntityRenderer<EntityFluxRift> {
    public RenderFluxRift(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(EntityFluxRift rift, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        int n = rift.points.size();
        if (n < 4) {
            return;
        }
        boolean goggles = EntityUtils.hasGoggles(Minecraft.getInstance().player);
        float amp = 1.0F;
        float stab = Mth.clamp(1.0F - rift.getRiftStability() / 50.0F, 0.0F, 1.5F);
        for (int q = 0; q <= 3; q++) {
            RenderType type = q == 3 ? TCRenderTypes.RIFT_CORE : (q == 0 && goggles ? TCRenderTypes.RIFT_GLOW_XRAY : TCRenderTypes.RIFT_GLOW);
            double[][] pp = new double[n][3];
            float[][] colours = new float[n][4];
            double[] radii = new double[n];
            for (int a = 0; a < n; a++) {
                float var = rift.tickCount + pt;
                if (a > n / 2) {
                    var -= a * 10;
                } else if (a < n / 2) {
                    var += a * 10;
                }
                pp[a][0] = rift.points.get(a).x + Math.sin(var / 50.0F * amp) * 0.1F * stab;
                pp[a][1] = rift.points.get(a).y + Math.sin(var / 60.0F * amp) * 0.1F * stab;
                pp[a][2] = rift.points.get(a).z + Math.sin(var / 70.0F * amp) * 0.1F * stab;
                colours[a][0] = 1.0F;
                colours[a][1] = 1.0F;
                colours[a][2] = 1.0F;
                colours[a][3] = 1.0F;
                double w = 1.0 - Math.sin(var / 8.0F * amp) * 0.1F * stab;
                radii[a] = rift.pointsWidth.get(a) * w * (q < 3 ? 1.25F + 0.5F * q : 1.0F);
            }
            PolyCone.draw(buffers.getBuffer(type), pose.last().pose(), 6, pp, colours, radii, 1.0F, 0.0F);
        }
    }

    @Override
    public boolean shouldRender(EntityFluxRift rift, Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(rift.getBoundingBox().inflate(1.0));
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFluxRift entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
