package thaumcraft.client.renderers.entity.construct;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.IPartTransform;
import thaumcraft.api.golems.parts.PartModel;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;

/** Колесо (порт PartModelWheel): поворот считает GolemLegWheels (в оригинале — статическая карта, здесь — поле голема). */
public class PartModelWheel extends PartModel {
    public PartModelWheel(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
        super(objModel, objTexture, attachPoint);
    }

    @Override
    public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, IPartTransform pose) {
        if (partName.equals("wheel")) {
            float lastRot = golem.getGolemEntity() instanceof EntityThaumcraftGolem g ? g.wheelRot : 0.0F;
            pose.translate(0.0, -0.375, 0.0);
            pose.rotate(lastRot, -1.0F, 0.0F, 0.0F);
        }
    }
}
