package thaumcraft.client.renderers.entity.construct;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.golems.IGolemAPI;
import thaumcraft.api.golems.parts.IPartTransform;
import thaumcraft.api.golems.parts.PartModel;

/** Короб носильщика (порт PartModelHauler): второй несомый предмет виден в коробе на спине. */
public class PartModelHauler extends PartModel {
    public PartModelHauler(ResourceLocation objModel, ResourceLocation objTexture, EnumAttachPoint attachPoint) {
        super(objModel, objTexture, attachPoint);
    }

    @Override
    public void postRenderObjectPart(String partName, IGolemAPI golem, float partialTicks, EnumLimbSide side, IPartTransform pose) {
        if (golem.getCarrying().size() > 1) {
            ItemStack itemstack = golem.getCarrying().get(1);
            if (!itemstack.isEmpty()) {
                pose.push();
                pose.scale(0.375F, 0.375F, 0.375F);
                pose.translate(0.0, 0.33, 0.825);
                if (!(itemstack.getItem() instanceof BlockItem)) {
                    pose.translate(0.0, 0.0, -0.25);
                }
                pose.renderItemHead(itemstack);
                pose.pop();
            }
        }
    }
}
