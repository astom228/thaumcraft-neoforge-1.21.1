package thaumcraft.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.crafting.ConfigRecipes;

/**
 * Магический рецепт без формы (ShapelessOreRecipe оригинала). Сопоставление сетки — как у ванильного бесформенного рецепта.
 * JSON: {"type": "thaumcraft:arcane_shapeless", "group", "research", "vis", "crystals": {"aer": 1}, "ingredients", "result"}.
 */
public class ShapelessArcaneRecipe implements IArcaneRecipe {
    final String group;
    private final String research;
    private final int vis;
    private final AspectList crystals;
    final NonNullList<Ingredient> ingredients;
    final ItemStack result;
    /** Ванильный рецепт с теми же ингредиентами — только для сопоставления сетки. */
    private final ShapelessRecipe matcher;

    public ShapelessArcaneRecipe(String group, String research, int vis, AspectList crystals, NonNullList<Ingredient> ingredients, ItemStack result) {
        this.group = group;
        this.research = research;
        this.vis = vis;
        this.crystals = crystals == null || crystals.size() == 0 ? null : crystals;
        this.ingredients = ingredients;
        this.result = result;
        this.matcher = new ShapelessRecipe(group, CraftingBookCategory.MISC, result, ingredients);
    }

    @Override
    public boolean matches(ArcaneCraftingInput inv, Level level) {
        return IArcaneRecipe.hasCrystals(this.crystals, inv) && this.matcher.matches(inv.grid(), level);
    }

    @Override
    public ItemStack assemble(ArcaneCraftingInput inv, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= this.ingredients.size();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.ingredients;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    @Override
    public int getVis() {
        return this.vis;
    }

    @Override
    public String getResearch() {
        return this.research;
    }

    @Override
    public AspectList getCrystals() {
        return this.crystals;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ConfigRecipes.ARCANE_SHAPELESS.get();
    }

    private AspectList crystalsOrEmpty() {
        return this.crystals == null ? new AspectList() : this.crystals;
    }

    public static class Serializer implements RecipeSerializer<ShapelessArcaneRecipe> {
        private static final Codec<NonNullList<Ingredient>> INGREDIENTS_CODEC = Ingredient.CODEC_NONEMPTY.listOf().flatXmap(list -> {
            Ingredient[] arr = list.stream().filter(ing -> !ing.isEmpty()).toArray(Ingredient[]::new);
            if (arr.length == 0) {
                return DataResult.error(() -> "No ingredients for arcane shapeless recipe");
            }
            if (arr.length > 9) {
                return DataResult.error(() -> "Too many ingredients for arcane shapeless recipe. The maximum is: 9");
            }
            return DataResult.success(NonNullList.of(Ingredient.EMPTY, arr));
        }, DataResult::success);

        public static final MapCodec<ShapelessArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Codec.STRING.optionalFieldOf("research", "").forGetter(ShapelessArcaneRecipe::getResearch),
                Codec.INT.optionalFieldOf("vis", 0).forGetter(ShapelessArcaneRecipe::getVis),
                CRYSTALS_CODEC.optionalFieldOf("crystals", new AspectList()).forGetter(ShapelessArcaneRecipe::crystalsOrEmpty),
                INGREDIENTS_CODEC.fieldOf("ingredients").forGetter(r -> r.ingredients),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(i, ShapelessArcaneRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShapelessArcaneRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, r.group);
                    ByteBufCodecs.STRING_UTF8.encode(buf, r.research);
                    ByteBufCodecs.VAR_INT.encode(buf, r.vis);
                    AspectList.STREAM_CODEC.encode(buf, r.crystalsOrEmpty());
                    ByteBufCodecs.VAR_INT.encode(buf, r.ingredients.size());
                    for (Ingredient ing : r.ingredients) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ing);
                    }
                    ItemStack.STREAM_CODEC.encode(buf, r.result);
                },
                buf -> {
                    String group = ByteBufCodecs.STRING_UTF8.decode(buf);
                    String research = ByteBufCodecs.STRING_UTF8.decode(buf);
                    int vis = ByteBufCodecs.VAR_INT.decode(buf);
                    AspectList crystals = AspectList.STREAM_CODEC.decode(buf);
                    int n = ByteBufCodecs.VAR_INT.decode(buf);
                    NonNullList<Ingredient> ings = NonNullList.withSize(n, Ingredient.EMPTY);
                    ings.replaceAll(ing -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                    return new ShapelessArcaneRecipe(group, research, vis, crystals, ings, ItemStack.STREAM_CODEC.decode(buf));
                });

        @Override
        public MapCodec<ShapelessArcaneRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapelessArcaneRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
