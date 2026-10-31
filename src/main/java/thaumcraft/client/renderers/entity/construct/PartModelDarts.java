package thaumcraft.client.renderers.entity.construct;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.PartModel;

/** Дротикомёты (порт PartModelDarts): в бою руки наводятся на цель. */
public class PartModelDarts extends PartModel {
    public PartModelDarts(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
        super(objModel, objTexture, attachPoint);
    }

    @Override
    public float preRenderArmRotationX(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        if (golem.isInCombat()) {
            inputRot = 90.0F - golem.getGolemEntity().xRotO + inputRot / 10.0F;
        }
        return inputRot;
    }

    @Override
    public float preRenderArmRotationY(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        if (golem.isInCombat()) {
            inputRot /= 10.0F;
        }
        return inputRot;
    }

    @Override
    public float preRenderArmRotationZ(IGolemAPI golem, float partialTicks, EnumLimbSide side, float inputRot) {
        if (golem.isInCombat()) {
            inputRot /= 10.0F;
        }
        return inputRot;
    }
}
