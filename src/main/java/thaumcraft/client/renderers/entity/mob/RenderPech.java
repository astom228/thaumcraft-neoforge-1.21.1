package thaumcraft.client.renderers.entity.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import thaumcraft.Thaumcraft;
import thaumcraft.client.renderers.models.entity.ModelPech;
import thaumcraft.common.entities.monster.EntityPech;

/** Рендер пеха (порт RenderPech + LayerHeldItemPech): три окраса по типу (собиратель, тауматург, следопыт), предмет в руках. */
public class RenderPech extends MobRenderer<EntityPech, ModelPech> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "pech"), "main");
    private static final ResourceLocation[] SKIN = {
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/pech_forage.png"),
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/pech_thaum.png"),
            ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/pech_stalker.png")};

    public RenderPech(EntityRendererProvider.Context ctx) {
        super(ctx, new ModelPech(ctx.bakeLayer(LAYER)), 0.5F);
        ItemInHandRenderer items = ctx.getItemInHandRenderer();
        this.addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, EntityPech e, float ls, float la, float pt, float age, float yaw, float pitch) {
                boolean right = e.getMainArm() == HumanoidArm.RIGHT;
                ItemStack off = right ? e.getOffhandItem() : e.getMainHandItem();
                ItemStack main = right ? e.getMainHandItem() : e.getOffhandItem();
                held(pose, buffers, light, e, main, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, HumanoidArm.RIGHT);
                held(pose, buffers, light, e, off, ItemDisplayContext.THIRD_PERSON_LEFT_HAND, HumanoidArm.LEFT);
            }

            private void held(PoseStack pose, MultiBufferSource buffers, int light, EntityPech e, ItemStack stack, ItemDisplayContext ctxType, HumanoidArm side) {
                if (stack.isEmpty()) {
                    return;
                }
                pose.pushPose();
                this.getParentModel().translateToHand(side, pose);
                pose.translate(0.0F, -0.1F, 0.0625F);
                if (stack.getItem() instanceof BowItem) {
                    pose.translate(-0.075F, -0.1, 0.0);
                }
                if (e.isCrouching()) {
                    pose.translate(0.0F, 0.2F, 0.0F);
                }
                pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
                pose.mulPose(Axis.YP.rotationDegrees(180.0F));
                boolean left = side == HumanoidArm.LEFT;
                pose.translate(left ? -0.0625F : 0.0625F, 0.125F, -0.625F);
                items.renderItem(e, stack, ctxType, left, pose, buffers, light);
                pose.popPose();
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(EntityPech entity) {
        return SKIN[Math.floorMod(entity.getPechType(), SKIN.length)];
    }
}
