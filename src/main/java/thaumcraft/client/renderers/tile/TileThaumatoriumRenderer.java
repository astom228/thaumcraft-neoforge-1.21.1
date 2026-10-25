package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.common.tiles.crafting.TileThaumatorium;

/**
 * Рендер тауматориума (порт TileThaumatoriumRenderer): над лицевой стороной — результат выбранного рецепта (несколько
 * рецептов сменяются раз в 2 секунды), как неподвижный предмет на земле.
 */
public class TileThaumatoriumRenderer implements BlockEntityRenderer<TileThaumatorium> {

    public TileThaumatoriumRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TileThaumatorium tile, float f, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Entity view = Minecraft.getInstance().getCameraEntity();
        if (tile.getLevel() == null || view == null || tile.recipeHash == null || tile.recipeHash.isEmpty()) {
            return;
        }
        Direction facing = tile.getFacing();
        int stack = view.tickCount / 40 % tile.recipeHash.size();
        CrucibleRecipe recipe = ThaumcraftApi.getCrucibleRecipeFromHash(tile.recipeHash.get(stack));
        if (recipe == null) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5F + facing.getStepX() / 1.99F, 1.125F, 0.5F + facing.getStepZ() / 1.99F);
        switch (facing) {
            case NORTH -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        pose.scale(0.75F, 0.75F, 0.75F);
        ItemStack is = recipe.getRecipeOutput().copy();
        is.setCount(1);
        // EntityItem с hoverStart = 0 и возрастом 0: подъём 0.1 + четверть высоты модели «на земле», без вращения
        var itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(is, tile.getLevel(), null, 0);
        float groundY = model.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
        pose.translate(0.0F, 0.1F + 0.25F * groundY, 0.0F);
        itemRenderer.render(is, ItemDisplayContext.GROUND, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }
}
