package thaumcraft.client.renderers.entity.construct;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.IPartTransform;
import thaumcraft.api.golems.parts.PartModel;
import thaumcraft.common.entities.construct.golem.EntityThaumcraftGolem;

/**
 * Дробилки (порт PartModelBreakers): вращаются, разгоняясь от взмаха. Скорость и угол в оригинале — в карте по id
 * сущности; здесь — в полях голема (клиентское состояние отрисовки).
 */
public class PartModelBreakers extends PartModel {
    public PartModelBreakers(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
        super(objModel, objTexture, attachPoint);
    }

    @Override
    public void preRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, IPartTransform pose) {
        if (partName.equals("grinder") && golem.getGolemEntity() instanceof EntityThaumcraftGolem g) {
            float f = Math.max(g.grinderSpeed, g.getAttackAnim(partialTicks) * 20.0F);
            float rot = g.grinderRot + f;
            g.grinderSpeed = f * 0.99F;
            g.grinderRot = rot;
            pose.translate(0.0, -0.34, 0.0);
            pose.rotate((g.tickCount + partialTicks) / 2.0F + rot + (side == EnumLimbSide.LEFT ? 22 : 0), side == EnumLimbSide.LEFT ? -1.0F : 1.0F, 0.0F, 0.0F);
        }
    }
}
