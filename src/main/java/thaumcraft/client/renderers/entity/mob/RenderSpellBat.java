package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelFireBat;
import thaumcraft.common.entities.monster.EntitySpellBat;

/**
 * Летучая мышь заклинания (порт RenderSpellBat): модель огненной летучей мыши ×0.35, полупрозрачная (0.5) цвета эффектов
 * заклинания, полная яркость.
 */
public class RenderSpellBat extends MobRenderer<EntitySpellBat, ModelFireBat<EntitySpellBat>> {
    private static final ResourceLocation RL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/spellbat.png");

    public RenderSpellBat(EntityRendererProvider.Context context) {
        super(context, new ModelFireBat<>(ModelFireBat.create()), 0.25F);
    }

    @Override
    public void render(EntitySpellBat bat, float yaw, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light) {
        this.model.tint = 0x80000000 | bat.color & 0xFFFFFF;
        super.render(bat, yaw, partialTicks, pose, buffer, light);
        this.model.tint = -1;
    }

    @Override
    protected RenderType getRenderType(EntitySpellBat bat, boolean bodyVisible, boolean translucent, boolean glowing) {
        return RenderType.entityTranslucent(RL);
    }

    @Override
    protected void scale(EntitySpellBat bat, PoseStack pose, float partialTicks) {
        pose.scale(0.35F, 0.35F, 0.35F);
    }

    @Override
    protected void setupRotations(EntitySpellBat bat, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
        pose.translate(0.0F, -0.1F, 0.0F);
        super.setupRotations(bat, pose, bob, yBodyRot, partialTick, scale);
    }

    @Override
    protected int getBlockLightLevel(EntitySpellBat entity, BlockPos pos) {
        return 15;
    }

    @Override
    public ResourceLocation getTextureLocation(EntitySpellBat entity) {
        return RL;
    }
}
