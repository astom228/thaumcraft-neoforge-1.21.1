package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.TCRenderTypes;
import thaumcraft.client.renderers.models.entity.ModelEldritchGuardian;
import thaumcraft.common.entities.monster.boss.EntityEldritchWarden;

/**
 * Рендер потустороннего стража и надзирателя (порт RenderEldritchGuardian). Страж полупрозрачен (0.6 ближе 16 блоков,
 * дальше тает к 32, на сложной сложности — к 24); в Чужих землях непрозрачен — проверка измерения появится на этапе 17.
 * Надзиратель непрозрачен, пока идёт таймер появления — поднимается из земли, глаз под капюшоном мерцает (аддитивно).
 */
public class RenderEldritchGuardian extends MobRenderer<Mob, ModelEldritchGuardian> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "eldritch_guardian"), "main");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/eldritch_guardian.png");
    private static final ResourceLocation TEX_WARDEN = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/eldritch_warden.png");

    public RenderEldritchGuardian(EntityRendererProvider.Context ctx) {
        this(ctx, 0.5F);
    }

    public RenderEldritchGuardian(EntityRendererProvider.Context ctx, float shadow) {
        super(ctx, new ModelEldritchGuardian(ctx.bakeLayer(LAYER)), shadow);
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, Mob e, float ls, float la, float pt, float age, float yaw, float pitch) {
                if (e instanceof EntityEldritchWarden) {
                    pose.pushPose();
                    pose.scale(1.01F, 1.01F, 1.01F);
                    int j = (int) (195.0F + Mth.sin(e.tickCount / 3.0F) * 15.0F + 15.0F);
                    RenderEldritchGuardian.this.model.hoodEye.render(pose, buffers.getBuffer(TCRenderTypes.additiveEntity(TEX_WARDEN)), j, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        });
    }

    @Override
    public void render(Mob guardian, float yaw, float pt, PoseStack pose, MultiBufferSource buffers, int light) {
        float base = 1.0F;
        pose.pushPose();
        if (guardian instanceof EntityEldritchWarden warden) {
            pose.translate(0.0, -guardian.getBbHeight() * (warden.getSpawnTimer() / 150.0F), 0.0);
        } else {
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
        }
        this.model.alpha = base;
        super.render(guardian, yaw, pt, pose, buffers, light);
        this.model.alpha = 1.0F;
        pose.popPose();
    }

    @Override
    protected RenderType getRenderType(Mob entity, boolean visible, boolean translucent, boolean glowing) {
        return visible ? RenderType.entityTranslucent(this.getTextureLocation(entity)) : super.getRenderType(entity, visible, translucent, glowing);
    }

    @Override
    public ResourceLocation getTextureLocation(Mob entity) {
        return entity instanceof EntityEldritchWarden ? TEX_WARDEN : TEX;
    }
}
