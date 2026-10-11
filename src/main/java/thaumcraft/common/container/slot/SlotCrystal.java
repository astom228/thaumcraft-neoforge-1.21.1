package thaumcraft.common.container.slot;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.items.resources.ItemCrystalEssence;

/** Слот вис-кристалла одного аспекта. */
public class SlotCrystal extends SlotItemHandler {
    private final Aspect aspect;

    public SlotCrystal(Aspect aspect, IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.aspect = aspect;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return isValidCrystal(stack, this.aspect);
    }

    public static boolean isValidCrystal(ItemStack stack, Aspect aspect) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemCrystalEssence crystal)) {
            return false;
        }
        AspectList al = crystal.getAspects(stack);
        return al != null && al.size() > 0 && al.getAspects()[0] == aspect;
    }
}
