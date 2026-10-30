package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import thaumcraft.common.entities.monster.EntityPech;

/** Пех (порт ModelPech): челюсти «бормочут», рюкзак раскачивается при ходьбе, руки чуть покачиваются в покое. */
public class ModelPech extends EntityModel<EntityPech> implements ArmedModel {
    private final ModelPart body, rightLeg, leftLeg, head, jowls, lowerPack, upperPack, rightArm, leftArm;

    public ModelPech(ModelPart root) {
        this.body = root.getChild("bipedBody");
        this.rightLeg = root.getChild("bipedRightLeg");
        this.leftLeg = root.getChild("bipedLeftLeg");
        this.head = root.getChild("bipedHead");
        this.jowls = root.getChild("Jowls");
        this.lowerPack = root.getChild("LowerPack");
        this.upperPack = root.getChild("UpperPack");
        this.rightArm = root.getChild("bipedRightArm");
        this.leftArm = root.getChild("bipedLeftArm");
    }

    @Override
    public void setupAnim(EntityPech entity, float par1, float par2, float par3, float par4, float par5) {
        this.head.yRot = par4 / (180.0F / (float) Math.PI);
        this.head.xRot = par5 / (180.0F / (float) Math.PI);
        float mumble = entity.mumble;
        this.jowls.yRot = this.head.yRot;
        this.jowls.xRot = this.head.xRot + ((float) (Math.PI / 12) + Mth.cos(par1 * 0.6662F) * par2 * 0.25F) + 0.34906587F * Math.abs(Mth.sin(mumble / 8.0F));
        this.rightArm.xRot = Mth.cos(par1 * 0.6662F + (float) Math.PI) * 2.0F * par2 * 0.5F;
        this.leftArm.xRot = Mth.cos(par1 * 0.6662F) * 2.0F * par2 * 0.5F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightLeg.xRot = Mth.cos(par1 * 0.6662F) * 1.4F * par2;
        this.leftLeg.xRot = Mth.cos(par1 * 0.6662F + (float) Math.PI) * 1.4F * par2;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;
        this.lowerPack.yRot = Mth.cos(par1 * 0.6662F) * 2.0F * par2 * 0.125F;
        this.lowerPack.zRot = Mth.cos(par1 * 0.6662F) * 2.0F * par2 * 0.125F;
        if (this.riding) {
            this.rightArm.xRot += (float) (-Math.PI / 5);
            this.leftArm.xRot += (float) (-Math.PI / 5);
            this.rightLeg.xRot = (float) (-Math.PI * 2.0 / 5.0);
            this.leftLeg.xRot = (float) (-Math.PI * 2.0 / 5.0);
            this.rightLeg.yRot = (float) (Math.PI / 10);
            this.leftLeg.yRot = (float) (-Math.PI / 10);
        }
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        if (this.attackTime > -9990.0F) {
            this.rightArm.yRot += this.body.yRot;
            this.leftArm.yRot += this.body.yRot;
            this.leftArm.xRot += this.body.yRot;
            float f6 = 1.0F - this.attackTime;
            f6 *= f6;
            f6 *= f6;
            f6 = 1.0F - f6;
            float f7 = Mth.sin(f6 * (float) Math.PI);
            float f8 = Mth.sin(this.attackTime * (float) Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;
            this.rightArm.xRot = (float) (this.rightArm.xRot - (f7 * 1.2 + f8));
            this.rightArm.yRot += this.body.yRot * 2.0F;
            this.rightArm.zRot = Mth.sin(this.attackTime * (float) Math.PI) * -0.4F;
        }
        if (entity.isCrouching()) {
            this.rightArm.xRot += 0.4F;
            this.leftArm.xRot += 0.4F;
        }
        this.rightArm.zRot += Mth.cos(par3 * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= Mth.cos(par3 * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += Mth.sin(par3 * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(par3 * 0.067F) * 0.05F;
    }

    @Override
    public void translateToHand(HumanoidArm side, PoseStack pose) {
        (side == HumanoidArm.RIGHT ? this.rightArm : this.leftArm).translateAndRotate(pose);
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer vc, int light, int overlay, int color) {
        this.body.render(pose, vc, light, overlay, color);
        this.rightLeg.render(pose, vc, light, overlay, color);
        this.leftLeg.render(pose, vc, light, overlay, color);
        this.head.render(pose, vc, light, overlay, color);
        this.jowls.render(pose, vc, light, overlay, color);
        this.lowerPack.render(pose, vc, light, overlay, color);
        this.upperPack.render(pose, vc, light, overlay, color);
        this.rightArm.render(pose, vc, light, overlay, color);
        this.leftArm.render(pose, vc, light, overlay, color);
    }
}
