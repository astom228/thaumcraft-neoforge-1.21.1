package thaumcraft.client.renderers.entity.construct;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.IPartTransform;
import thaumcraft.api.golems.parts.PartModel;

/** Когти (порт PartModelClaws): смыкаются при взмахе. */
public class PartModelClaws extends PartModel {
    public PartModelClaws(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
        super(objModel, objTexture, attachPoint);
    }

    @Override
    public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, IPartTransform pose) {
        if (partName.startsWith("claw")) {
            float f = golem.getGolemEntity().getAttackAnim(partialTicks) * 4.1F;
            f = f * f;
            pose.translate(0.0, -0.2, 0.0);
            pose.rotate(f, partName.endsWith("1") ? 1.0F : -1.0F, 0.0F, 0.0F);
        }
    }
}
