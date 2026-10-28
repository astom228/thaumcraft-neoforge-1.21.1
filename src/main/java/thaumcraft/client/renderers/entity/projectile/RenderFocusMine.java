package thaumcraft.client.renderers.entity.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import thaumcraft.Thaumcraft;
import thaumcraft.common.entities.projectile.EntityFocusMine;

/**
 * Мина фокуса (порт RenderFocusMine + ModelGrappler): кубик с тремя штырями, текстура mine, полная яркость; пока не сработала,
 * мигает от белого к красному (цикл 8 тиков счётчика).
 */
public class RenderFocusMine extends EntityRenderer<EntityFocusMine> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/entity/mine.png");
    private final ModelPart model;

    public RenderFocusMine(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
        this.model = createModel();
    }

    private static ModelPart createModel() {
        MeshDefinition md = new MeshDefinition();
        PartDefinition root = md.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("prong1", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5), PartPose.ZERO);
        root.addOrReplaceChild("prong2", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5),
                PartPose.rotation(0.0F, (float) (Math.PI / 2), 0.0F));
        root.addOrReplaceChild("prong3", CubeListBuilder.create().texOffs(0, 10).addBox(-0.5F, -0.5F, -2.5F, 1, 1, 5),
                PartPose.rotation((float) (Math.PI / 2), (float) (Math.PI / 2), 0.0F));
        return LayerDefinition.create(md, 64, 32).bakeRoot();
    }

    @Override
    public void render(EntityFocusMine mine, float entityYaw, float pticks, PoseStack pose, MultiBufferSource buffer, int light) {
        pose.pushPose();
        float f = (mine.counter + pticks) % 8.0F / 8.0F;
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pticks, mine.yRotO, mine.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pticks, mine.xRotO, mine.getXRot())));
        int color = FastColor.ARGB32.colorFromFloat(1.0F, 1.0F, 1.0F - f, 1.0F - f);
        this.model.render(pose, buffer.getBuffer(RenderType.entityTranslucent(TEX)), 0xF000F0, OverlayTexture.NO_OVERLAY, color);
        pose.popPose();
        super.render(mine, entityYaw, pticks, pose, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityFocusMine entity) {
        return TEX;
    }
}
