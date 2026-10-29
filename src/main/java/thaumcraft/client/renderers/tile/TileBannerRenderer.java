package thaumcraft.client.renderers.tile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.misc.TileBanner;

/**
 * Знамя (порт TileBannerRenderer + ModelBanner): шест (если не на стене), перекладина, крепления цвета ткани и колышущееся
 * полотно; у знамени культа — своя текстура; нарисованный аспект — поверх полотна цветом аспекта.
 */
public class TileBannerRenderer implements BlockEntityRenderer<TileBanner> {
    private static final ResourceLocation TEX_CULT = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/banner_cultist.png");
    private static final ResourceLocation TEX_BLANK = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/banner_blank.png");
    private final ModelPart b1;
    private final ModelPart b2;
    private final ModelPart beam;
    private final ModelPart banner;
    private final ModelPart pole;

    public TileBannerRenderer(BlockEntityRendererProvider.Context ctx) {
        MeshDefinition md = new MeshDefinition();
        PartDefinition root = md.getRoot();
        root.addOrReplaceChild("b1", CubeListBuilder.create().texOffs(0, 29).mirror().addBox(-5.0F, -7.5F, -1.5F, 2, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("b2", CubeListBuilder.create().texOffs(0, 29).mirror().addBox(3.0F, -7.5F, -1.5F, 2, 3, 3), PartPose.ZERO);
        root.addOrReplaceChild("beam", CubeListBuilder.create().texOffs(30, 0).mirror().addBox(-7.0F, -7.0F, -1.0F, 14, 2, 2), PartPose.ZERO);
        root.addOrReplaceChild("banner", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-7.0F, 0.0F, -0.5F, 14, 28, 1), PartPose.offset(0.0F, -5.0F, 0.0F));
        root.addOrReplaceChild("pole", CubeListBuilder.create().texOffs(62, 0).mirror().addBox(0.0F, 0.0F, -1.0F, 2, 31, 2), PartPose.offset(-1.0F, -7.0F, -2.0F));
        ModelPart m = LayerDefinition.create(md, 128, 64).bakeRoot();
        this.b1 = m.getChild("b1");
        this.b2 = m.getChild("b2");
        this.beam = m.getChild("beam");
        this.banner = m.getChild("banner");
        this.pole = m.getChild("pole");
    }

    @Override
    public void render(TileBanner banner, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        ResourceLocation tex = banner.getAspect() == null && banner.getColor() == -1 ? TEX_CULT : TEX_BLANK;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(tex));
        pose.translate(0.5F, 1.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        if (banner.getLevel() != null) {
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.mulPose(Axis.YP.rotationDegrees(banner.getBannerFacing() * 360 / 16.0F));
        }
        if (!banner.getWall()) {
            this.pole.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        } else {
            pose.translate(0.0, 1.0, -0.4125);
        }
        this.beam.render(pose, vc, light, OverlayTexture.NO_OVERLAY);
        int color = 0xFFFFFFFF;
        if (banner.getColor() != -1) {
            color = 0xFF000000 | banner.getColor();
        }
        this.b1.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
        this.b2.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
        float ticks = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.tickCount : 0;
        float f3 = banner.getBlockPos().getX() * 7 + banner.getBlockPos().getY() * 9 + banner.getBlockPos().getZ() * 13 + ticks + pt;
        float rx = 0.02F - Mth.sin(f3 / 11.0F) * 0.02F;
        this.banner.xRot = rx;
        this.banner.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
        Aspect aspect = banner.getAspect();
        if (aspect != null && aspect.getImage() != null) {
            pose.pushPose();
            pose.translate(0.0F, 0.0F, 0.05001F);
            pose.scale(0.0375F, 0.0375F, 0.0375F);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.mulPose(Axis.XP.rotationDegrees(-rx * (180.0F / (float) Math.PI) * 2.0F));
            drawTag(pose, buffers, -8, 0, aspect, light);
            pose.popPose();
        }
        pose.popPose();
    }

    /** UtilsFX.drawTag(x, y, aspect, 0, 0, 0, 771, 0.75F, false) оригинала: значок 16×16 цветом аспекта, прозрачность 0.75. */
    private static void drawTag(PoseStack pose, MultiBufferSource buffers, int x, int y, Aspect aspect, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(aspect.getImage()));
        Matrix4f m = pose.last().pose();
        Color c = new Color(aspect.getColor());
        float r = c.getRed() / 255.0F, g = c.getGreen() / 255.0F, b = c.getBlue() / 255.0F, a = 0.75F;
        vc.addVertex(m, x, y + 16.0F, 0.0F).setColor(r, g, b, a).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, 1);
        vc.addVertex(m, x + 16.0F, y + 16.0F, 0.0F).setColor(r, g, b, a).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, 1);
        vc.addVertex(m, x + 16.0F, y, 0.0F).setColor(r, g, b, a).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, 1);
        vc.addVertex(m, x, y, 0.0F).setColor(r, g, b, a).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(), 0, 0, 1);
    }

    @Override
    public AABB getRenderBoundingBox(TileBanner banner) {
        return new AABB(banner.getBlockPos()).expandTowards(0, 1, 0).expandTowards(0, -1, 0);
    }
}
