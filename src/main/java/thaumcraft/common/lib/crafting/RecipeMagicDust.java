package thaumcraft.common.lib.crafting;

import java.util.ArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.items.resources.ItemCrystalEssence;

/**
 * Соль мира: миска, кремень, редстоун и три вис-кристалла разных аспектов, в любом порядке.
 * Миска и кремень остаются в сетке (по одному).
 */
public class RecipeMagicDust extends CustomRecipe {

    public RecipeMagicDust(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        boolean bowl = false;
        boolean flint = false;
        boolean redstone = false;
        ArrayList<String> crystals = new ArrayList<>();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(Items.BOWL) && bowl) {
                return false;
            }
            if (stack.is(Items.BOWL) && !bowl) {
                bowl = true;
            } else {
                if (stack.is(Items.FLINT) && flint) {
                    return false;
                }
                if (stack.is(Items.FLINT) && !flint) {
                    flint = true;
                } else {
                    if (stack.is(Items.REDSTONE) && redstone) {
                        return false;
                    }
                    if (stack.is(Items.REDSTONE) && !redstone) {
                        redstone = true;
                    } else {
                        if (!(stack.getItem() instanceof ItemCrystalEssence ice)) {
                            return false;
                        }
                        AspectList al = ice.getAspects(stack);
                        if (al == null || al.size() == 0) {
                            return false;
                        }
                        String tag = al.getAspects()[0].getTag();
                        if (crystals.contains(tag) || crystals.size() >= 3) {
                            return false;
                        }
                        crystals.add(tag);
                    }
                }
            }
        }
        return bowl && redstone && flint && crystals.size() == 3;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, HolderLookup.Provider registries) {
        return new ItemStack(ItemsTC.salisMundus.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 6;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(ItemsTC.salisMundus.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput inv) {
        NonNullList<ItemStack> ret = NonNullList.withSize(inv.size(), ItemStack.EMPTY);
        for (int i = 0; i < ret.size(); i++) {
            ItemStack itemstack = inv.getItem(i);
            ItemStack itemstack2 = itemstack.getCraftingRemainingItem();
            if (!itemstack.isEmpty() && (itemstack.is(Items.FLINT) || itemstack.is(Items.BOWL))) {
                itemstack2 = itemstack.copyWithCount(1);
            }
            ret.set(i, itemstack2);
        }
        return ret;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ConfigRecipes.SALIS_MUNDUS.get();
    }
}
