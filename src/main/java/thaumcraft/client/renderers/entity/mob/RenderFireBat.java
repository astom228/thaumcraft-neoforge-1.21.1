package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelFireBat;
import thaumcraft.common.entities.monster.EntityFireBat;

/** Огненная летучая мышь (порт RenderFireBat): модель ×0.35, в полёте покачивается, висящая — чуть ниже; полная яркость. */
public class RenderFireBat extends MobRenderer<EntityFireBat, ModelFireBat<EntityFireBat>> {
    private static final ResourceLocation RL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/firebat.png");

    public RenderFireBat(EntityRendererProvider.Context context) {
        super(context, new ModelFireBat<>(ModelFireBat.create()), 0.25F);
    }

    @Override
    protected void scale(EntityFireBat bat, PoseStack pose, float partialTicks) {
        pose.scale(0.35F, 0.35F, 0.35F);
    }

    @Override
    protected void setupRotations(EntityFireBat bat, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
        if (!bat.getIsBatHanging()) {
            pose.translate(0.0F, Mth.cos(bob * 0.3F) * 0.1F, 0.0F);
        } else {
            pose.translate(0.0F, -0.1F, 0.0F);
        }
        super.setupRotations(bat, pose, bob, yBodyRot, partialTick, scale);
    }

    @Override
    protected int getBlockLightLevel(EntityFireBat entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFireBat entity) {
        return RL;
    }
}
