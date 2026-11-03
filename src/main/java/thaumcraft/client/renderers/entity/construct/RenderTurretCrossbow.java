package thaumcraft.client.renderers.entity.construct;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelCrossbow;
import thaumcraft.common.entities.construct.EntityTurretCrossbow;

/** Арбалет-турель (порт RenderTurretCrossbow): тело не поворачивается — ложе вращает модель по взгляду. */
public class RenderTurretCrossbow extends MobRenderer<EntityTurretCrossbow, ModelCrossbow> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "crossbow"), "main");
    private static final ResourceLocation RL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/crossbow.png");

    public RenderTurretCrossbow(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelCrossbow(ctx.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public void render(EntityTurretCrossbow e, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        e.loadProgressForRender = e.getLoadProgress(pt);
        e.yBodyRot = 0.0F;
        e.yBodyRotO = 0.0F;
        super.render(e, yaw, pt, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityTurretCrossbow entity) {
        return RL;
    }
}
