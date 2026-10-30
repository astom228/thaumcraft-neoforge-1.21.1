package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import thaumcraft.common.entities.monster.tainted.EntityTaintSeed;

/**
 * Модель семени порчи (порт ModelTaintSeed + ModelRendererTaintSeed): шар и цепочка из 7 звеньев (4 больших, 3 малых),
 * покачивающихся по синусу. Перед каждым дочерним звеном — пульсирующий масштаб (scale = 1.6 − sin(π·q/7), q — общий
 * счётчик вызовов 0–7, как в оригинале). При появлении семя «вырастает» из земли за height·10 тиков.
 */
public class ModelTaintSeed extends EntityModel<EntityTaintSeed> {
    private static final int LENGTH = 8;
    /** Только поток отрисовки клиента (static int q оригинала). */
    private static int q = 0;
    private final ModelPart root;
    private final ModelPart[] tents = new ModelPart[LENGTH - 1];
    private final float[] rotX = new float[LENGTH - 1];
    private final float[] rotZ = new float[LENGTH - 1];
    private EntityTaintSeed entity;

    public ModelTaintSeed(ModelPart part) {
        this.root = part.getChild("tentacle");
        for (int k = 0; k < LENGTH - 1; k++) {
            this.tents[k] = part.getChild("tent" + k);
        }
    }

    /** Звенья — без поворота и опорной точки: их переносы и повороты делаются вручную, как в ModelRendererTaintSeed. */
    public static LayerDefinition create() {
        MeshDefinition md = new MeshDefinition();
        md.getRoot().addOrReplaceChild("tentacle", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8), PartPose.ZERO);
        for (int k = 0; k < LENGTH - 1; k++) {
            CubeListBuilder b = CubeListBuilder.create().texOffs(0, k < LENGTH - 4 ? 16 : (k == LENGTH - 4 ? 48 : 56));
            if (k < LENGTH - 4) {
                b.addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
            } else {
                b.addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4);
            }
            md.getRoot().addOrReplaceChild("tent" + k, b, PartPose.ZERO);
        }
        return LayerDefinition.create(md, 64, 64);
    }

    private static float pivotY(int k) {
        return k < LENGTH - 4 || k == LENGTH - 4 ? -8.0F : -4.0F;
    }

    @Override
    public void setupAnim(EntityTaintSeed seed, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.entity = seed;
        float ht = seed.hurtTime / 200.0F;
        float flail = 0.1F;
        float mod = 0.0625F * 0.2F;
        float fi = (flail + (!(ht > 0.0F) ? -mod : mod)) * 3.0F;
        for (int k = 0; k < LENGTH - 1; k++) {
            this.rotX[k] = 0.1F / fi * Mth.sin(ageInTicks * 0.06F - k / 2.0F) / 5.0F + ht + seed.attackAnim;
            this.rotZ[k] = 0.1F / fi * Mth.sin(ageInTicks * 0.05F - k / 2.0F) / 5.0F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        if (this.entity == null) {
            return;
        }
        float h = this.entity.getBbHeight();
        float height = 0.0F;
        float hc = h * 10.0F;
        if (this.entity.tickCount < hc) {
            height = (hc - this.entity.tickCount) / hc * h;
        }
        float tt = this.entity.tickCount + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        pose.translate(0.0F, (h == 3.0F ? 0.6F : 1.2F) + height, 0.0F);
        pose.scale(h / 2.0F, h / 2.0F, h / 2.0F);
        // корень: опорная точка (0, 12, 0), без поворота
        pose.translate(0.0F, 12.0F / 16.0F, 0.0F);
        this.root.render(pose, vc, light, overlay, color);
        this.renderChild(pose, vc, light, overlay, color, 0, tt);
        pose.popPose();
    }

    /** Звено k под масштабом родителя (render(par1, tt, 1.6) ModelRendererTaintSeed). */
    private void renderChild(PoseStack pose, VertexConsumer vc, int light, int overlay, int color, int k, float tt) {
        if (k >= LENGTH - 1) {
            return;
        }
        float qq = (float) (Math.PI * (q / 7.0F));
        float scale = 1.6F - (float) Math.sin(qq);
        float pulse = (float) Math.sin(tt / 12.0F - qq) * 0.33F;
        pulse *= pulse;
        q++;
        if (q > 7) {
            q = 0;
        }
        pose.pushPose();
        pose.scale(scale + pulse, scale * 0.9F, scale + pulse);
        pose.translate(0.0F, pivotY(k) / 16.0F, 0.0F);
        if (this.rotZ[k] != 0.0F) {
            pose.mulPose(Axis.ZP.rotation(this.rotZ[k]));
        }
        if (this.rotX[k] != 0.0F) {
            pose.mulPose(Axis.XP.rotation(this.rotX[k]));
        }
        this.tents[k].render(pose, vc, light, overlay, color);
        this.renderChild(pose, vc, light, overlay, color, k + 1, tt);
        pose.popPose();
    }
}
