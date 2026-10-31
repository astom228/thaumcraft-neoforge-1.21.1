package thaumcraft.client.renderers.entity.construct;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.Thaumcraft;
import thaumcraft.api.golems.EnumGolemTrait;
import thaumcraft.api.golems.IGolemProperties;
import thaumcraft.api.golems.parts.IPartTransform;
import thaumcraft.api.golems.parts.PartModel;
import thaumcraft.client.lib.obj.WavefrontObject;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;

/**
 * Голем (порт RenderThaumcraftGolem): база golem_base.obj (грудь, пояс, флажок цвета, руки) и OBJ-детали по точкам
 * крепления; части «bm…» — текстурой материала. Анимация рук/ног, наклон на ходу, взмах — как в оригинале. Детали
 * берутся из состава каждый кадр (в оригинале — кэш по id сущности без сброса).
 */
public class RenderThaumcraftGolem extends EntityRenderer<EntityThaumcraftGolem> {
    private static final ResourceLocation BASE = ResourceLocation.fromNamespaceAndPath(Thaumcraft.MODID, "models/obj/golem_base.obj");
    private final Map<ResourceLocation, WavefrontObject> models = new HashMap<>();

    public RenderThaumcraftGolem(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.3F;
    }

    private WavefrontObject model(ResourceLocation loc) {
        return this.models.computeIfAbsent(loc, WavefrontObject::load);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityThaumcraftGolem entity) {
        return entity.getProperties().getMaterial().texture;
    }

    @Override
    public void render(EntityThaumcraftGolem entity, float entityYaw, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        boolean visible = !entity.isInvisible();
        boolean ghost = !visible && mc.player != null && !entity.isInvisibleTo(mc.player);
        if (visible || ghost) {
            pose.pushPose();
            float bodyRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
            float headRot = Mth.rotLerp(partialTicks, entity.yHeadRotO, entity.yHeadRot);
            float netHeadYaw = headRot - bodyRot;
            float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());
            float age = entity.tickCount + partialTicks;
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
            if (entity.deathTime > 0) {
                float f = Mth.sqrt((entity.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F);
                pose.mulPose(Axis.ZP.rotationDegrees(Math.min(f, 1.0F) * 90.0F));
            }
            float limbSpeed = Math.min(entity.walkAnimation.speed(partialTicks), 1.0F);
            float limbPos = entity.walkAnimation.position(partialTicks);
            int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
            Parts parts = new Parts(entity, pose, buffers, light, overlay, ghost ? 0.15F : 1.0F, partialTicks);
            parts.renderAll(limbPos, limbSpeed, age, netHeadYaw, pitch);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTicks, pose, buffers, light);
    }

    /** Состояние одного кадра; реализует IPartTransform для хуков деталей. */
    private final class Parts implements IPartTransform {
        final EntityThaumcraftGolem entity;
        final PoseStack pose;
        final MultiBufferSource buffers;
        final int light;
        final int overlay;
        final float alpha;
        final float partialTicks;
        final ResourceLocation matTexture;
        final Map<PartModel.EnumAttachPoint, List<PartModel>> byPoint = new EnumMap<>(PartModel.EnumAttachPoint.class);

        Parts(EntityThaumcraftGolem entity, PoseStack pose, MultiBufferSource buffers, int light, int overlay, float alpha, float partialTicks) {
            this.entity = entity;
            this.pose = pose;
            this.buffers = buffers;
            this.light = light;
            this.overlay = overlay;
            this.alpha = alpha;
            this.partialTicks = partialTicks;
            IGolemProperties props = entity.getProperties();
            this.matTexture = props.getMaterial().texture;
            for (PartModel.EnumAttachPoint p : PartModel.EnumAttachPoint.values()) {
                this.byPoint.put(p, new ArrayList<>());
            }
            PartModel[] ms = {props.getHead().model, props.getArms().model, props.getLegs().model, props.getAddon().model};
            for (PartModel m : ms) {
                if (m != null) {
                    this.byPoint.get(m.getAttachPoint()).add(m);
                }
            }
        }

        void base(String part, float r, float g, float b) {
            model(BASE).renderPartEntity(part, this.buffers.getBuffer(RenderType.entityTranslucent(this.matTexture)), this.pose.last(), this.light,
                    this.overlay, r, g, b, this.alpha);
        }

        void renderAll(float limbSwing, float limbAmount, float rotFloat, float headYaw, float headPitch) {
            IGolemProperties props = this.entity.getProperties();
            boolean holding = !this.entity.getMainHandItem().isEmpty();
            boolean aflag = props.hasTrait(EnumGolemTrait.WHEELED) || props.hasTrait(EnumGolemTrait.FLYER);
            double dx = this.entity.getX() - this.entity.xo;
            double dz = this.entity.getZ() - this.entity.zo;
            double speed = dx * dx + dz * dz;
            float swingProgress = this.entity.getAttackAnim(this.partialTicks);
            float f1;
            float bry = 0.0F;
            float rx = (float) Math.toDegrees(Mth.sin(rotFloat * 0.067F) * 0.03F);
            float rz = (float) Math.toDegrees(Mth.cos(rotFloat * 0.09F) * 0.05F + 0.05F);
            float rrx = 0.0F, rry = 0.0F, rrz = 0.0F, rlx = 0.0F, rly = 0.0F, rlz = 0.0F;
            if (holding) {
                rrx = 90.0F - rz / 2.0F;
                rrz = -2.0F;
                rlx = 90.0F - rz / 2.0F;
                rlz = 2.0F;
            } else {
                if (aflag) {
                    rrx = rx * 2.0F;
                    rlx = -rx * 2.0F;
                } else {
                    f1 = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbAmount * 0.5F;
                    rrx = (float) (Math.toDegrees(f1) + rx);
                    f1 = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbAmount * 0.5F;
                    rlx = (float) (Math.toDegrees(f1) - rx);
                }
                rrz += rz + 2.0F;
                rlz -= rz + 2.0F;
            }
            if (swingProgress > 0.0F) {
                float wiggle = -Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI * 2.0F) * 0.2F;
                bry = (float) Math.toDegrees(wiggle);
                rrz = -((float) Math.toDegrees(Mth.sin(wiggle) * 3.0F));
                rrx = (float) Math.toDegrees(-Mth.cos(wiggle) * 5.0F);
                rry += bry;
            }
            this.rotate(bry, 0.0F, 1.0F, 0.0F);
            float lean = aflag ? 75.0F : 25.0F;
            this.rotate((float) (speed * lean), -1.0F, 0.0F, 0.0F);
            this.rotate((float) (speed * lean * 0.06 * (this.entity.getYRot() - this.entity.yRotO)), 0.0F, 0.0F, -1.0F);

            this.push();
            this.translate(0.0, 0.5, 0.0);
            this.base("chest", 1.0F, 1.0F, 1.0F);
            this.base("waist", 1.0F, 1.0F, 1.0F);
            if (this.entity.getGolemColor() > 0) {
                int c = DyeColor.byId(this.entity.getGolemColor() - 1).getTextureDiffuseColor();
                this.base("flag", (c >> 16 & 255) / 255.0F, (c >> 8 & 255) / 255.0F, (c & 255) / 255.0F);
            }
            for (PartModel part : this.byPoint.get(PartModel.EnumAttachPoint.BODY)) {
                this.renderPart(part, PartModel.EnumLimbSide.MIDDLE);
            }
            this.pop();

            this.push();
            this.translate(0.0, 0.75, -0.03125);
            this.rotate(headYaw, 0.0F, -1.0F, 0.0F);
            this.rotate(headPitch, -1.0F, 0.0F, 0.0F);
            for (PartModel part : this.byPoint.get(PartModel.EnumAttachPoint.HEAD)) {
                this.renderPart(part, PartModel.EnumLimbSide.MIDDLE);
            }
            this.base("head", 1.0F, 1.0F, 1.0F);
            this.pop();

            List<PartModel> arms = this.byPoint.get(PartModel.EnumAttachPoint.ARMS);
            this.push();
            this.translate(0.20625, 0.6875, 0.0);
            if (!arms.isEmpty()) {
                PartModel part = arms.get(0);
                rrx = part.preRenderArmRotationX(this.entity, this.partialTicks, PartModel.EnumLimbSide.RIGHT, rrx);
                rry = part.preRenderArmRotationY(this.entity, this.partialTicks, PartModel.EnumLimbSide.RIGHT, rry);
                rrz = part.preRenderArmRotationZ(this.entity, this.partialTicks, PartModel.EnumLimbSide.RIGHT, rrz);
            }
            this.rotate(rrx, 1.0F, 0.0F, 0.0F);
            this.rotate(rry, 0.0F, 1.0F, 0.0F);
            this.rotate(rrz, 0.0F, 0.0F, 1.0F);
            this.base("arm", 1.0F, 1.0F, 1.0F);
            for (PartModel part : arms) {
                this.renderPart(part, PartModel.EnumLimbSide.RIGHT);
            }
            this.pop();

            this.push();
            this.translate(-0.20625, 0.6875, 0.0);
            if (!arms.isEmpty()) {
                PartModel part = arms.get(0);
                rlx = part.preRenderArmRotationX(this.entity, this.partialTicks, PartModel.EnumLimbSide.LEFT, rlx);
                rly = part.preRenderArmRotationY(this.entity, this.partialTicks, PartModel.EnumLimbSide.LEFT, rly);
                rlz = part.preRenderArmRotationZ(this.entity, this.partialTicks, PartModel.EnumLimbSide.LEFT, rlz);
            }
            this.rotate(rlx, 1.0F, 0.0F, 0.0F);
            this.rotate(rly + 180.0F, 0.0F, 1.0F, 0.0F);
            this.rotate(rlz, 0.0F, 0.0F, -1.0F);
            this.base("arm", 1.0F, 1.0F, 1.0F);
            for (PartModel part : arms) {
                this.renderPart(part, PartModel.EnumLimbSide.LEFT);
            }
            this.pop();

            this.push();
            this.translate(0.09375, 0.375, 0.0);
            f1 = Mth.cos(limbSwing * 0.6662F) * limbAmount;
            this.rotate((float) Math.toDegrees(f1), 1.0F, 0.0F, 0.0F);
            for (PartModel part : this.byPoint.get(PartModel.EnumAttachPoint.LEGS)) {
                this.renderPart(part, PartModel.EnumLimbSide.RIGHT);
            }
            this.pop();

            this.push();
            this.translate(-0.09375, 0.375, 0.0);
            f1 = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * limbAmount;
            this.rotate((float) Math.toDegrees(f1), 1.0F, 0.0F, 0.0F);
            for (PartModel part : this.byPoint.get(PartModel.EnumAttachPoint.LEGS)) {
                this.renderPart(part, PartModel.EnumLimbSide.LEFT);
            }
            this.pop();

            this.push();
            this.translate(0.0, 0.625, 0.0);
            this.rotate(90.0F - rz * 0.5F, 1.0F, 0.0F, 0.0F);
            this.drawHeldItem();
            this.pop();
        }

        private void drawHeldItem() {
            ItemStack stack = this.entity.getMainHandItem();
            if (!stack.isEmpty()) {
                this.push();
                this.rotate(90.0F, -1.0F, 0.0F, 0.0F);
                this.scale(0.375F, 0.375F, 0.375F);
                this.translate(0.0, 0.0, -1.5);
                if (!(stack.getItem() instanceof BlockItem)) {
                    this.translate(0.0, -0.6, 0.0);
                }
                this.renderItemHead(stack);
                this.pop();
            }
        }

        private void renderPart(PartModel part, PartModel.EnumLimbSide side) {
            WavefrontObject obj = model(part.getObjModel());
            for (String op : obj.getPartNames()) {
                this.push();
                ResourceLocation tex = part.useMaterialTextureForObjectPart(op) || part.getTexture() == null ? this.matTexture : part.getTexture();
                part.preRenderObjectPart(op, this.entity, this.partialTicks, side, this);
                obj.renderPartEntity(op, this.buffers.getBuffer(RenderType.entityTranslucent(tex)), this.pose.last(), this.light, this.overlay, 1.0F, 1.0F,
                        1.0F, this.alpha);
                part.postRenderObjectPart(op, this.entity, this.partialTicks, side, this);
                this.pop();
            }
        }

        @Override
        public void translate(double x, double y, double z) {
            this.pose.translate(x, y, z);
        }

        /** GlStateManager.rotate: угол в градусах вокруг оси (ось нормализуется). */
        @Override
        public void rotate(float degrees, float x, float y, float z) {
            if (degrees == 0.0F) {
                return;
            }
            Vector3f axis = new Vector3f(x, y, z);
            if (axis.lengthSquared() == 0.0F) {
                return;
            }
            axis.normalize();
            this.pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(degrees), axis));
        }

        @Override
        public void scale(float x, float y, float z) {
            this.pose.scale(x, y, z);
        }

        @Override
        public void push() {
            this.pose.pushPose();
        }

        @Override
        public void pop() {
            this.pose.popPose();
        }

        @Override
        public void renderItemHead(ItemStack stack) {
            Minecraft.getInstance().getItemRenderer().renderStatic(this.entity, stack, ItemDisplayContext.HEAD, false, this.pose, this.buffers,
                    this.entity.level(), this.light, OverlayTexture.NO_OVERLAY, this.entity.getId());
        }
    }
}
