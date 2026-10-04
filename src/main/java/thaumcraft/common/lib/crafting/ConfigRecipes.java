package thaumcraft.common.lib.crafting;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.Thaumcraft;

/** Регистр сериализаторов рецептов (особые рецепты оригинала; свои типы рецептов — на этапах 8, 9, 11). */
public class ConfigRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Thaumcraft.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<RecipeTripleMeatTreat>> TRIPLE_MEAT_TREAT =
            SERIALIZERS.register("triple_meat_treat", () -> new SimpleCraftingRecipeSerializer<>(RecipeTripleMeatTreat::new));
}
