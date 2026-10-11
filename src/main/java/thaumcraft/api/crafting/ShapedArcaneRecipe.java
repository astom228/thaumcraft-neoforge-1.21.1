package thaumcraft.api.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.crafting.ConfigRecipes;

/**
 * Магический рецепт с формой (ShapedOreRecipe оригинала: зеркальное отражение допускается).
 * JSON: {"type": "thaumcraft:arcane_shaped", "group", "research", "vis", "crystals": {"aer": 1}, "key", "pattern", "result"}.
 */
public class ShapedArcaneRecipe implements IArcaneRecipe {
    final String group;
    private final String research;
    private final int vis;
    private final AspectList crystals;
    final ShapedRecipePattern pattern;
    final ItemStack result;

    public ShapedArcaneRecipe(String group, String research, int vis, AspectList crystals, ShapedRecipePattern pattern, ItemStack result) {
        this.group = group;
        this.research = research;
        this.vis = vis;
        this.crystals = crystals == null || crystals.size() == 0 ? null : crystals;
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public boolean matches(ArcaneCraftingInput inv, Level level) {
        return IArcaneRecipe.hasCrystals(this.crystals, inv) && this.pattern.matches(inv.grid());
    }

    @Override
    public ItemStack assemble(ArcaneCraftingInput inv, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.pattern.width() && height >= this.pattern.height();
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return this.pattern.ingredients();
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

    public int getWidth() {
        return this.pattern.width();
    }

    public int getHeight() {
        return this.pattern.height();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ConfigRecipes.ARCANE_SHAPED.get();
    }

    private AspectList crystalsOrEmpty() {
        return this.crystals == null ? new AspectList() : this.crystals;
    }

    public static class Serializer implements RecipeSerializer<ShapedArcaneRecipe> {
        public static final MapCodec<ShapedArcaneRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(r -> r.group),
                Codec.STRING.optionalFieldOf("research", "").forGetter(ShapedArcaneRecipe::getResearch),
                Codec.INT.optionalFieldOf("vis", 0).forGetter(ShapedArcaneRecipe::getVis),
                CRYSTALS_CODEC.optionalFieldOf("crystals", new AspectList()).forGetter(ShapedArcaneRecipe::crystalsOrEmpty),
                ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(i, ShapedArcaneRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ShapedArcaneRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, r) -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, r.group);
                    ByteBufCodecs.STRING_UTF8.encode(buf, r.research);
                    ByteBufCodecs.VAR_INT.encode(buf, r.vis);
                    AspectList.STREAM_CODEC.encode(buf, r.crystalsOrEmpty());
                    ShapedRecipePattern.STREAM_CODEC.encode(buf, r.pattern);
                    ItemStack.STREAM_CODEC.encode(buf, r.result);
                },
                buf -> new ShapedArcaneRecipe(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf), AspectList.STREAM_CODEC.decode(buf), ShapedRecipePattern.STREAM_CODEC.decode(buf),
                        ItemStack.STREAM_CODEC.decode(buf)));

        @Override
        public MapCodec<ShapedArcaneRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ShapedArcaneRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
