package thaumcraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Рецепты, на которые ссылаются страницы исследований (в оригинале — каталог CommonInternals.getCatalogRecipe /
 * getCatalogRecipeFake, затем CraftingManager). Пока — только рецепты RecipeManager клиента; каталог Thaumcraft
 * (магический верстак, тигель, инфузия, «поддельные» рецепты для показа) добавляется на этапах 8, 9, 11.
 */
public final class ResearchRecipes {
    /** Результат рецепта или null, если такого рецепта нет. */
    public static ItemStack getRecipeOutput(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        return mc.level.getRecipeManager().byKey(id).map(h -> h.value().getResultItem(mc.level.registryAccess())).orElse(null);
    }

    private ResearchRecipes() {
    }
}
