package thaumcraft.common.lib.crafting;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCTags;

/**
 * Тройное мясное угощение: сахар + 3 разных кусочка мяса, в любом порядке.
 * В оригинале пустые слоты сетки не проходили проверку, и рецепт никогда не срабатывал — исправлено (DEVIATIONS.md №10).
 */
public class RecipeTripleMeatTreat extends CustomRecipe {
    public RecipeTripleMeatTreat(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean sugar = false;
        Set<Item> meats = new HashSet<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.SUGAR)) {
                if (sugar) {
                    return false;
                }
                sugar = true;
            } else {
                if (!stack.is(TCTags.Items.NUGGETS_MEAT) || meats.contains(stack.getItem()) || meats.size() >= 3) {
                    return false;
                }
                meats.add(stack.getItem());
            }
        }
        return sugar && meats.size() == 3;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(ItemsTC.tripleMeatTreat.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 4;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ConfigRecipes.TRIPLE_MEAT_TREAT.get();
    }
}
