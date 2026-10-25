package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import thaumcraft.common.tiles.crafting.TilePedestal;

/** Рендер пьедестала (порт TilePedestalRenderer): предмет над пьедесталом, увеличенный в 1,25 раза, медленно вращается. */
public class TilePedestalRenderer implements BlockEntityRenderer<TilePedestal> {

    public TilePedestalRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TilePedestal ped, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Entity view = Minecraft.getInstance().getCameraEntity();
        if (ped.getStackInSlot(0).isEmpty() || view == null) {
            return;
        }
        float ticks = view.tickCount + partialTicks;
        pose.pushPose();
        pose.translate(0.5F, 0.75F, 0.5F);
        pose.scale(1.25F, 1.25F, 1.25F);
        pose.mulPose(Axis.YP.rotationDegrees(ticks % 360.0F));
        ItemStack is = ped.getStackInSlot(0).copyWithCount(1);
        // EntityItem с hoverStart = 0 и возрастом 0: подъём 0.1 + четверть высоты модели «на земле»
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(is, ped.getLevel(), null, 0);
        float groundY = model.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
        pose.translate(0.0F, 0.1F + 0.25F * groundY, 0.0F);
        itemRenderer.render(is, ItemDisplayContext.GROUND, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }

    /** getRenderBoundingBox оригинала: блок и блок над ним. */
    @Override
    public AABB getRenderBoundingBox(TilePedestal ped) {
        return new AABB(ped.getBlockPos()).expandTowards(0, 1, 0);
    }
}
