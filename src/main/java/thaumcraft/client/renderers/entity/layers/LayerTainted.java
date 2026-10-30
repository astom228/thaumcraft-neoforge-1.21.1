package thaumcraft.client.renderers.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.client.lib.TCRenderTypes;

/**
 * Волокна заражения на заражённом чемпионе (порт LayerTainted): модель ещё раз с медленно ползущей текстурой
 * taint_fibres, прозрачность 0.5. В оригинале слой добавлялся рендеру при первом показе такого существа; здесь он
 * добавлен всем рендерам живых существ и рисуется только для CHAMPION_MOD = 13.
 */
public class LayerTainted<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final ResourceLocation TAINT_TEXTURE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/taint_fibres.png");

    public LayerTainted(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount, float pt, float age, float yaw,
            float pitch) {
        Integer t = ThaumcraftApiHelper.getChampionMod(entity);
        if (t == null || t != 13) {
            return;
        }
        float f = entity.tickCount + pt;
        float f1 = Mth.cos(f * 2.5E-4F);
        float f2 = f * 0.001F;
        this.getParentModel().renderToBuffer(pose, buffers.getBuffer(TCRenderTypes.taintFibres(TAINT_TEXTURE, f1, f2)), light, OverlayTexture.NO_OVERLAY,
                0x80FFFFFF);
    }
}
