package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelEldritchGuardian;

/**
 * Рендер потустороннего стража (порт RenderEldritchGuardian): полупрозрачный (0.6 ближе 16 блоков, дальше тает к 32,
 * на сложной сложности — к 24). В Чужих землях непрозрачен — проверка измерения появится на этапе 17.
 * Текстура надзирателя — когда появится надзиратель.
 */
public class RenderEldritchGuardian extends MobRenderer<Mob, ModelEldritchGuardian> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch_guardian"), "main");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/eldritch_guardian.png");

    public RenderEldritchGuardian(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelEldritchGuardian(ctx.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public void render(Mob guardian, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        float base = 1.0F;
        var e = Minecraft.getInstance().getCameraEntity();
        if (e != null) {
            float d6 = e.level().getDifficulty() == Difficulty.HARD ? 576.0F : 1024.0F;
            float d7 = 256.0F;
            double d8 = guardian.distanceToSqr(e.getX(), e.getY(), e.getZ());
            if (d8 < 256.0) {
                base = 0.6F;
            } else {
                base = (float) (1.0 - Math.min(d6 - d7, d8 - d7) / (d6 - d7)) * 0.6F;
            }
        }
        this.model.alpha = base;
        super.render(guardian, yaw, pt, pose, buffers, light);
        this.model.alpha = 1.0F;
    }

    @Override
    protected RenderType getRenderType(Mob entity, boolean visible, boolean translucent, boolean glowing) {
        return visible ? RenderType.entityTranslucent(this.getTextureLocation(entity)) : super.getRenderType(entity, visible, translucent, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
        return TEX;
    }
}
