package thaumcraft.client.renderers.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.client.lib.UtilsFX;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/**
 * Очки откровения в слоте головы (onPlayerBaubleRender оригинала): объёмная текстура goggles_bauble (толщина 0.1, масштаб 0.55)
 * перед лицом; при надетом шлеме — чуть дальше от лица. Привязка к голове — через часть модели head (в 1.12 — повороты
 * Baubles по yaw/pitch головы и сдвиги Helper.translateToHeadLevel / translateToFace).
 */
public class GogglesCurioRenderer implements ICurioRenderer {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/items/goggles_bauble.png");

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack pose,
            RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(parent.getModel() instanceof HumanoidModel<?> model)) {
            return;
        }
        boolean armor = !slotContext.entity().getItemBySlot(EquipmentSlot.HEAD).isEmpty();
        pose.pushPose();
        model.head.translateAndRotate(pose);
        // пространство головы: ось y вниз, лицо — в сторону −z; центр очков — середина головы по высоте, на передней грани
        pose.translate(0.0, -0.25, -0.25 - (armor ? 0.066 : 0.0) - 0.001);
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        pose.scale(0.55F, 0.55F, 0.55F);
        pose.translate(-0.5, -0.5, 0.0);
        UtilsFX.renderTextureIn3D(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)), 0.0F, 0.0F, 1.0F, 1.0F, 16, 16, 0.1F, light);
        pose.popPose();
    }
}
