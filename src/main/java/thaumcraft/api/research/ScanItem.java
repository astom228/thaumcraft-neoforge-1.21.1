package thaumcraft.api.research;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApiHelper;

/** Предмет (стек или выброшенный предмет), сравнение как в рецептах. */
public class ScanItem implements IScanThing {
    String research;
    ItemStack stack;

    public ScanItem(String research, ItemStack stack) {
        this.research = research;
        this.stack = stack;
    }

    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj == null) {
            return false;
        }
        ItemStack is = null;
        if (obj instanceof ItemStack s) {
            is = s;
        }
        if (obj instanceof ItemEntity ie) {
            is = ie.getItem();
        }
        return is != null && !is.isEmpty() && ThaumcraftApiHelper.areItemStacksEqualForCrafting(is, this.stack);
    }

    @Override
    public String getResearchKey(Player player, Object object) {
        return this.research;
    }
}
