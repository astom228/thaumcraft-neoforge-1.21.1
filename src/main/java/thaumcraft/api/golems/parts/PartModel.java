package thaumcraft.api.golems.parts;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;

/**
 * OBJ-модель детали (порт PartModel): точка крепления; части с именем на «bm» берут текстуру материала. Хуки вызываются
 * при отрисовке каждой части модели (pose — преобразования, см. IPartTransform).
 */
public class PartModel {
    private final ResourceLocation objModel;
    private final ResourceLocation texture;
    private final EnumAttachPoint attachPoint;

    public PartModel(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
        this.objModel = objModel;
        this.texture = objTexture;
        this.attachPoint = attachPoint;
    }

    public ResourceLocation getObjModel() {
        return this.objModel;
    }

    public ResourceLocation getTexture() {
        return this.texture;
    }

    public EnumAttachPoint getAttachPoint() {
        return this.attachPoint;
    }

    public boolean useMaterialTextureForObjectPart(String partName) {
        return partName.startsWith("bm");
    }

    public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, IPartTransform pose) {
    }

    public void postRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, IPartTransform pose) {
    }

    public float preRenderArmRotationX(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        return inputRot;
    }

    public float preRenderArmRotationY(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        return inputRot;
    }

    public float preRenderArmRotationZ(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        return inputRot;
    }

    public enum EnumAttachPoint {
        ARMS,
        LEGS,
        BODY,
        HEAD
    }

    public enum EnumLimbSide {
        LEFT,
        RIGHT,
        MIDDLE
    }
}
