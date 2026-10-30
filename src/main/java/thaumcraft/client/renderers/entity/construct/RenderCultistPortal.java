package thaumcraft.client.renderers.entity.construct;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.common.entities.monster.boss.EntityCultistPortalGreater;
import thaumcraft.common.entities.monster.cult.EntityCultistPortalLesser;

/**
 * Рендер порталов культистов (порт RenderCultistPortalLesser / Greater): повёрнутая к камере анимированная плоскость
 * (16 кадров cultist_portal), растягивается при открытии, дрожит от ударов и «пульсирует» при выпуске культистов,
 * с потерей здоровья мерцает. Малый виден, только когда открыт.
 */
public class RenderCultistPortal<T extends Mob> extends EntityRenderer<T> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/misc/cultist_portal.png");

    public RenderCultistPortal(EntityRendererProvider.Context ctx, float shadow) {
        super(ctx);
        this.shadowRadius = shadow;
        this.shadowStrength = shadow > 0.0F ? 0.5F : 0.0F;
    }

    public static <T extends Mob> RenderCultistPortal<T> lesser(EntityRendererProvider.Context ctx) {
        return new RenderCultistPortal<>(ctx, 0.0F);
    }

    public static <T extends Mob> RenderCultistPortal<T> greater(EntityRendererProvider.Context ctx) {
        return new RenderCultistPortal<>(ctx, 0.1F);
    }

    @Override
    public void render(T portal, float yaw, float f, PoseStack pose, MultiBufferSource buffers, int light) {
        int counter;
        int pulse;
        float scaley;
        float scaleMul;
        if (portal instanceof EntityCultistPortalLesser l) {
            if (!l.isActive()) {
                return;
            }
            counter = l.activeCounter;
            pulse = l.pulse;
            scaley = 1.4F;
            scaleMul = 1.25F;
        } else if (portal instanceof EntityCultistPortalGreater g) {
            counter = g.tickCount;
            pulse = g.pulse;
            scaley = 1.5F;
            scaleMul = 1.3F;
        } else {
            return;
        }
        long time = System.nanoTime() / 50000000L;
        int e = (int) Math.min(50.0F, counter + f);
        if (portal.hurtTime > 0) {
            double d = Math.sin(portal.hurtTime * 72 * Math.PI / 180.0);
            scaley = (float) (scaley - d / 4.0);
            e = (int) (e + 6.0 * d);
        }
        if (pulse > 0) {
            double d = Math.sin(pulse * 36 * Math.PI / 180.0);
            scaley = (float) (scaley + d / 4.0);
            e = (int) (e + 12.0 * d);
        }
        float scale = e / 50.0F * scaleMul;
        float m = (1.0F - portal.getHealth() / portal.getMaxHealth()) / 3.0F;
        float bob = Mth.sin(counter / (5.0F - 12.0F * m)) * m + m;
        float bob2 = Mth.sin(counter / (6.0F - 15.0F * m)) * m + m;
        float alpha = 1.0F - bob;
        scaley -= bob / 4.0F;
        scale -= bob2 / 3.0F;
        Camera cam = this.entityRenderDispatcher.camera;
        Vector3f rv = new Vector3f(cam.getLeftVector());
        Vector3f uv = new Vector3f(cam.getUpVector());
        int frame = 15 - (int) time % 16;
        float f2 = frame / 16.0F;
        float f3 = f2 + 0.0625F;
        pose.pushPose();
        pose.translate(0.0, portal.getBbHeight() / 2.0F, 0.0);
        Matrix4f mat = pose.last().pose();
        VertexConsumer vc = buffers.getBuffer(TCRenderTypes.tube(TEX));
        v(vc, mat, -rv.x - uv.x, -rv.y - uv.y, -rv.z - uv.z, scale, scaley, f3, 0.0F, alpha);
        v(vc, mat, -rv.x + uv.x, -rv.y + uv.y, -rv.z + uv.z, scale, scaley, f3, 1.0F, alpha);
        v(vc, mat, rv.x + uv.x, rv.y + uv.y, rv.z + uv.z, scale, scaley, f2, 1.0F, alpha);
        v(vc, mat, rv.x - uv.x, rv.y - uv.y, rv.z - uv.z, scale, scaley, f2, 0.0F, alpha);
        pose.popPose();
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z, float scale, float scaley, float u, float v, float alpha) {
        vc.addVertex(m, x * scale, y * scaley, z * scale).setUv(u, v).setColor(1.0F, 1.0F, 1.0F, alpha);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return TEX;
    }
}
