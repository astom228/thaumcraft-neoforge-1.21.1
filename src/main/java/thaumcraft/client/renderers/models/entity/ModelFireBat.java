package thaumcraft.client.renderers.models.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Модель огненной летучей мыши (порт ModelFireBat): голова с ушами, тело с хвостом, двухсекционные крылья. Используется и
 * летучей мышью заклинания. Висящая поза — у огненной летучей мыши (этап 16).
 */
public class ModelFireBat<T extends Entity> extends EntityModel<T> {
    private final ModelPart batHead;
    private final ModelPart batBody;
    private final ModelPart batRightWing;
    private final ModelPart batLeftWing;
    private final ModelPart batOuterRightWing;
    private final ModelPart batOuterLeftWing;
    /** Цвет (ARGB) поверх текстуры — летучая мышь заклинания окрашивается цветом эффектов. */
    public int tint = -1;

    public ModelFireBat(ModelPart root) {
        this.batHead = root.getChild("head");
        this.batBody = root.getChild("body");
        this.batRightWing = this.batBody.getChild("right_wing");
        this.batLeftWing = this.batBody.getChild("left_wing");
        this.batOuterRightWing = this.batRightWing.getChild("outer_right_wing");
        this.batOuterLeftWing = this.batLeftWing.getChild("outer_left_wing");
    }

    public static ModelPart create() {
        MeshDefinition md = new MeshDefinition();
        PartDefinition root = md.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6)
                .texOffs(24, 0).addBox(-4.0F, -6.0F, -2.0F, 3, 4, 1)
                .texOffs(24, 0).mirror().addBox(1.0F, -6.0F, -2.0F, 3, 4, 1), PartPose.ZERO);
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3.0F, 4.0F, -3.0F, 6, 12, 6)
                .texOffs(0, 34).addBox(-5.0F, 16.0F, 0.0F, 10, 6, 1), PartPose.ZERO);
        PartDefinition right = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(42, 0).addBox(-12.0F, 1.0F, 1.5F, 10, 16, 1),
                PartPose.ZERO);
        right.addOrReplaceChild("outer_right_wing", CubeListBuilder.create().texOffs(24, 16).addBox(-8.0F, 1.0F, 0.0F, 8, 12, 1),
                PartPose.offset(-12.0F, 1.0F, 1.5F));
        PartDefinition left = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(42, 0).mirror().addBox(2.0F, 1.0F, 1.5F, 10, 16, 1),
                PartPose.ZERO);
        left.addOrReplaceChild("outer_left_wing", CubeListBuilder.create().texOffs(24, 16).mirror().addBox(0.0F, 1.0F, 0.0F, 8, 12, 1),
                PartPose.offset(12.0F, 1.0F, 1.5F));
        return LayerDefinition.create(md, 64, 64).bakeRoot();
    }

    /** Поза: висит вниз головой (огненная летучая мышь на потолке) или в полёте (render оригинала). */
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity instanceof thaumcraft.common.entities.monster.EntityFireBat bat && bat.getIsBatHanging()) {
            this.batHead.xRot = headPitch * Mth.DEG_TO_RAD;
            this.batHead.yRot = (float) Math.PI - netHeadYaw * Mth.DEG_TO_RAD;
            this.batHead.zRot = (float) Math.PI;
            this.batHead.setPos(0.0F, -2.0F, 0.0F);
            this.batRightWing.setPos(-3.0F, 0.0F, 3.0F);
            this.batLeftWing.setPos(3.0F, 0.0F, 3.0F);
            this.batBody.xRot = (float) Math.PI;
            this.batRightWing.xRot = (float) (-Math.PI / 20);
            this.batRightWing.yRot = (float) (-Math.PI * 2.0 / 5.0);
            this.batOuterRightWing.yRot = -1.7278761F;
            this.batLeftWing.xRot = this.batRightWing.xRot;
            this.batLeftWing.yRot = -this.batRightWing.yRot;
            this.batOuterLeftWing.yRot = -this.batOuterRightWing.yRot;
            return;
        }
        this.batHead.xRot = headPitch * Mth.DEG_TO_RAD;
        this.batHead.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.batHead.zRot = 0.0F;
        this.batHead.setPos(0.0F, 0.0F, 0.0F);
        this.batRightWing.setPos(0.0F, 0.0F, 0.0F);
        this.batLeftWing.setPos(0.0F, 0.0F, 0.0F);
        this.batBody.xRot = (float) (Math.PI / 4) + Mth.cos(ageInTicks * 0.1F) * 0.15F;
        this.batBody.yRot = 0.0F;
        this.batRightWing.yRot = Mth.cos(ageInTicks * 1.3F) * (float) Math.PI * 0.25F;
        this.batLeftWing.yRot = -this.batRightWing.yRot;
        this.batOuterRightWing.yRot = this.batRightWing.yRot * 0.5F;
        this.batOuterLeftWing.yRot = -this.batRightWing.yRot * 0.5F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        int c = this.tint == -1 ? color : multiply(color, this.tint);
        this.batHead.render(pose, buffer, light, overlay, c);
        this.batBody.render(pose, buffer, light, overlay, c);
    }

    private static int multiply(int a, int b) {
        int al = (a >>> 24) * (b >>> 24) / 255;
        int r = (a >> 16 & 255) * (b >> 16 & 255) / 255;
        int g = (a >> 8 & 255) * (b >> 8 & 255) / 255;
        int bl = (a & 255) * (b & 255) / 255;
        return al << 24 | r << 16 | g << 8 | bl;
    }
}
