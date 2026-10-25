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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.client.lib.UtilsFX;
import thaumcraft.common.config.ModConfig;
import thaumcraft.common.tiles.essentia.TileJar;
import thaumcraft.common.tiles.essentia.TileJarBrain;
import thaumcraft.common.tiles.essentia.TileJarFillable;

/**
 * Содержимое банок (порт TileJarRenderer): жидкость эссенции (светящаяся текстура цвета аспекта), скоба, переходник под трубу,
 * ярлык с тёмным значком аспекта (слегка кривой, если включено в настройках); у мозга — плавающий мозг в рассоле.
 * Модели ModelJar / ModelBrain оригинала собраны из тех же кубов.
 */
public class TileJarRenderer implements BlockEntityRenderer<TileJar> {
    private static final ResourceLocation TEX_LABEL = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/label.png");
    private static final ResourceLocation TEX_BRAIN = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/brain2.png");
    private static final ResourceLocation TEX_BRINE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "textures/models/jarbrine.png");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "blocks/animatedglow");
    private final ModelPart jar;
    private final ModelPart brain;

    public TileJarRenderer(BlockEntityRendererProvider.Context ctx) {
        MeshDefinition md = new MeshDefinition();
        PartDefinition root = md.getRoot();
        root.addOrReplaceChild("brine", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -11.0F, -4.0F, 8, 10, 8), PartPose.ZERO);
        root.addOrReplaceChild("lid", CubeListBuilder.create().texOffs(32, 24).addBox(-3.0F, 0.0F, -3.0F, 6, 2, 6), PartPose.offset(0.0F, -14.0F, 0.0F));
        root.addOrReplaceChild("lid_extension", CubeListBuilder.create().texOffs(0, 23).addBox(-2.0F, -16.0F, -2.0F, 4, 2, 4), PartPose.ZERO);
        this.jar = LayerDefinition.create(md, 64, 32).bakeRoot();
        MeshDefinition mb = new MeshDefinition();
        PartDefinition rb = mb.getRoot();
        rb.addOrReplaceChild("shape1", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, 0.0F, 0.0F, 12, 10, 16), PartPose.offset(-6.0F, 8.0F, -8.0F));
        rb.addOrReplaceChild("shape2", CubeListBuilder.create().texOffs(64, 0).addBox(0.0F, 0.0F, 0.0F, 8, 3, 7), PartPose.offset(-4.0F, 18.0F, 0.0F));
        rb.addOrReplaceChild("shape3", CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, 0.0F, 0.0F, 2, 6, 2),
                PartPose.offsetAndRotation(-1.0F, 18.0F, -2.0F, 0.4089647F, 0.0F, 0.0F));
        this.brain = LayerDefinition.create(mb, 128, 64).bakeRoot();
    }

    @Override
    public void render(TileJar tile, float f, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5F, 0.01F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        if (tile instanceof TileJarBrain b) {
            this.renderBrain(b, f, pose, buffers, light);
        } else if (tile instanceof TileJarFillable jarTile) {
            if (jarTile.blocked) {
                pose.pushPose();
                pose.scale(1.001F, 1.001F, 1.001F);
                this.jar.getChild("lid").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BRINE)), light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            if (ThaumcraftApiHelper.getConnectableTile(tile.getLevel(), tile.getBlockPos(), Direction.UP) != null) {
                pose.pushPose();
                pose.scale(0.9F, 1.0F, 0.9F);
                this.jar.getChild("lid_extension").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BRINE)), light,
                        OverlayTexture.NO_OVERLAY);
                pose.popPose();
            }
            if (jarTile.aspectFilter != null) {
                this.renderLabel(jarTile, pose, buffers, light);
            }
            if (jarTile.amount > 0) {
                this.renderLiquid(jarTile, pose, buffers);
            }
        }
        pose.popPose();
    }

    private void renderLabel(TileJarFillable tile, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        switch (tile.facing) {
            case 3 -> pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            case 4 -> pose.mulPose(Axis.YP.rotationDegrees(270.0F));
            case 5 -> pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            default -> {
            }
        }
        float rot = (tile.aspectFilter.getTag().hashCode() + tile.getBlockPos().getX() + tile.facing) % 4 - 2;
        boolean crooked = ModConfig.CONFIG_GRAPHICS.crooked.get();
        // ярлык: квадрат 0.5 по центру (UtilsFX.renderQuadCentered, яркость -99 — освещение блока)
        pose.pushPose();
        pose.translate(0.0F, -0.4F, 0.315F);
        if (crooked) {
            pose.mulPose(Axis.ZP.rotationDegrees(rot));
        }
        UtilsFX.renderQuadCenteredWorld(pose, buffers.getBuffer(RenderType.entityTranslucent(TEX_LABEL)), 0.5F, 1.0F, 1.0F, 1.0F, 1.0F, light);
        pose.popPose();
        // значок аспекта (drawTag(bw): тёмный, прозрачность 0.8)
        pose.pushPose();
        pose.translate(0.0F, -0.4F, 0.316F);
        if (crooked) {
            pose.mulPose(Axis.ZP.rotationDegrees(rot));
        }
        pose.scale(0.021F, 0.021F, 0.021F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        UtilsFX.drawTagWorld(pose, buffers, -8, -8, tile.aspectFilter, light);
        pose.popPose();
        pose.popPose();
    }

    /** Жидкость: куб 0.25..0.75 × 0.0625..(0.0625 + уровень) с текстурой animatedglow цвета аспекта, яркость 200 (RenderCubes). */
    private void renderLiquid(TileJarFillable te, PoseStack pose, MultiBufferSource buffers) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(180.0F));
        float level = te.amount / 250.0F * 0.625F;
        Color co = new Color(0);
        if (te.aspect != null) {
            co = new Color(te.aspect.getColor());
        }
        float r = co.getRed() / 255.0F;
        float g = co.getGreen() / 255.0F;
        float b = co.getBlue() / 255.0F;
        TextureAtlasSprite icon = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(GLOW);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        float x0 = -0.25F;
        float x1 = 0.25F;
        float y0 = 0.0625F;
        float y1 = 0.0625F + level;
        float z0 = -0.25F;
        float z1 = 0.25F;
        int light = 200;
        PoseStack.Pose p = pose.last();
        Matrix4f m = p.pose();
        // низ/верх: u по x, v по z; стороны: u по x/z, v по высоте (как RenderBlocks)
        float ua = icon.getU(0.25F);
        float ub = icon.getU(0.75F);
        float va = icon.getV(0.25F);
        float vb = icon.getV(0.75F);
        float vy0 = icon.getV(1.0F - 0.0625F);
        float vy1 = icon.getV(1.0F - Mth.clamp(0.0625F + level, 0.0F, 1.0F));
        face(vc, p, m, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, ua, vb, ua, va, ub, va, ub, vb, r, g, b, light, 0, -1, 0);
        face(vc, p, m, x1, y1, z1, x1, y1, z0, x0, y1, z0, x0, y1, z1, ub, vb, ub, va, ua, va, ua, vb, r, g, b, light, 0, 1, 0);
        face(vc, p, m, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, ub, vy1, ua, vy1, ua, vy0, ub, vy0, r, g, b, light, 0, 0, -1);
        face(vc, p, m, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, ua, vy1, ua, vy0, ub, vy0, ub, vy1, r, g, b, light, 0, 0, 1);
        face(vc, p, m, x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, ub, vy1, ua, vy1, ua, vy0, ub, vy0, r, g, b, light, -1, 0, 0);
        face(vc, p, m, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, ua, vy0, ub, vy0, ub, vy1, ua, vy1, r, g, b, light, 1, 0, 0);
        pose.popPose();
    }

    private static void face(VertexConsumer vc, PoseStack.Pose p, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy,
                             float cz, float dx, float dy, float dz, float ua, float va, float ub, float vb, float uc, float vc2, float ud, float vd, float r,
                             float g, float b, int light, float nx, float ny, float nz) {
        vc.addVertex(m, ax, ay, az).setColor(r, g, b, 1.0F).setUv(ua, va).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, bx, by, bz).setColor(r, g, b, 1.0F).setUv(ub, vb).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, cx, cy, cz).setColor(r, g, b, 1.0F).setUv(uc, vc2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
        vc.addVertex(m, dx, dy, dz).setColor(r, g, b, 1.0F).setUv(ud, vd).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    private void renderBrain(TileJarBrain te, float f, PoseStack pose, MultiBufferSource buffers, int light) {
        float bob = Mth.sin(Minecraft.getInstance().player.tickCount / 14.0F) * 0.03F + 0.03F;
        pose.pushPose();
        pose.translate(0.0F, -0.8F + bob, 0.0F);
        float f2 = te.rota - te.rotb;
        while (f2 >= 3.141593F) {
            f2 -= 6.283185F;
        }
        while (f2 < -3.141593F) {
            f2 += 6.283185F;
        }
        float f3 = te.rotb + f2 * f;
        pose.mulPose(Axis.YP.rotationDegrees(f3 * 180.0F / 3.141593F));
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.scale(0.4F, 0.4F, 0.4F);
        this.brain.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX_BRAIN)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        this.jar.getChild("brine").render(pose, buffers.getBuffer(RenderType.entityTranslucent(TEX_BRINE)), light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
