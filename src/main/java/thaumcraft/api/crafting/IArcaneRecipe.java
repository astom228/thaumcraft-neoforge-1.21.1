package thaumcraft.api.crafting;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.crafting.ConfigRecipes;

/**
 * Рецепт магического верстака: стоимость в Вис, исследование (в оригинале при создании не проверяется — DECISIONS.md)
 * и кристаллы, которые должны лежать в слотах кристаллов.
 */
public interface IArcaneRecipe extends Recipe<ArcaneCraftingInput> {
    /** Кристаллы в JSON: {"aer": 1, "ordo": 2}. */
    Codec<AspectList> CRYSTALS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT).xmap(map -> {
        AspectList list = new AspectList();
        map.forEach((tag, amount) -> {
            Aspect a = Aspect.getAspect(tag);
            if (a != null) {
                list.add(a, amount);
            }
        });
        return list;
    }, list -> {
        java.util.Map<String, Integer> map = new java.util.LinkedHashMap<>();
        for (Aspect a : list.getAspects()) {
            map.put(a.getTag(), list.getAmount(a));
        }
        return map;
    });

    int getVis();

    String getResearch();

    /** null, если кристаллы не нужны (как в оригинале). */
    AspectList getCrystals();

    @Override
    default RecipeType<?> getType() {
        return ConfigRecipes.ARCANE_CRAFTING.get();
    }

    /** Проверка кристаллов из matches оригинала: для каждого аспекта — стопка кристалла не меньше нужного количества. */
    static boolean hasCrystals(AspectList crystals, ArcaneCraftingInput inv) {
        if (crystals == null) {
            return true;
        }
        for (Aspect aspect : crystals.getAspects()) {
            ItemStack cs = ThaumcraftApiHelper.makeCrystal(aspect, crystals.getAmount(aspect));
            boolean b = false;
            for (int i = 0; i < ArcaneCraftingInput.CRYSTAL_SLOTS; i++) {
                ItemStack itemstack1 = inv.getCrystal(i);
                if (itemstack1.getItem() == ItemsTC.crystalEssence.get() && itemstack1.getCount() >= cs.getCount()
                        && ItemStack.isSameItemSameComponents(cs, itemstack1)) {
                    b = true;
                }
            }
            if (!b) {
                return false;
            }
        }
        return true;
    }
}
