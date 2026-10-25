package thaumcraft.api.crafting;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;

/**
 * Рецепт наполнения (порт InfusionRecipe): центральный предмет на пьедестале под матрицей, компоненты на окружающих
 * пьедесталах (порядок неважен), эссенция и нестабильность. Результат — ItemStack или (в 1.12 — {"имя NBT", значение})
 * пара {DataComponentType, значение}, которая записывается в центральный предмет.
 */
public class InfusionRecipe implements IThaumcraftRecipe {
    public AspectList aspects;
    public String research;
    private String name;
    protected NonNullList<Ingredient> components = NonNullList.create();
    public Ingredient sourceInput;
    public Object recipeOutput;
    public int instability;
    private String group = "";

    public InfusionRecipe(String research, Object outputResult, int inst, AspectList aspects2, Object centralItem, Object... recipe) {
        this.name = "";
        this.research = research;
        this.recipeOutput = outputResult;
        this.aspects = aspects2;
        this.instability = inst;
        this.sourceInput = ThaumcraftApiHelper.getIngredient(centralItem);
        if (this.sourceInput == null) {
            throw new RuntimeException("Invalid infusion central item: " + centralItem);
        }
        for (Object in : recipe) {
            Ingredient ing = ThaumcraftApiHelper.getIngredient(in);
            if (ing == null) {
                StringBuilder ret = new StringBuilder("Invalid infusion recipe: ");
                for (Object tmp : recipe) {
                    ret.append(tmp).append(", ");
                }
                ret.append(outputResult);
                throw new RuntimeException(ret.toString());
            }
            this.components.add(ing);
        }
    }

    public boolean matches(List<ItemStack> input, ItemStack central, Level world, Player player) {
        if (this.getRecipeInput() == null) {
            return false;
        }
        if (!ThaumcraftCapabilities.knowsResearch(player, this.research)) {
            return false;
        }
        return (this.getRecipeInput() == Ingredient.EMPTY || this.getRecipeInput().test(central))
                && RecipeMatcher.findMatches(input, this.getComponents()) != null;
    }

    @Override
    public String getResearch() {
        return this.research;
    }

    public Ingredient getRecipeInput() {
        return this.sourceInput;
    }

    public NonNullList<Ingredient> getComponents() {
        return this.components;
    }

    public Object getRecipeOutput() {
        return this.recipeOutput;
    }

    public AspectList getAspects() {
        return this.aspects;
    }

    public Object getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
        return this.recipeOutput;
    }

    public AspectList getAspects(Player player, ItemStack input, List<ItemStack> comps) {
        return this.aspects;
    }

    public int getInstability(Player player, ItemStack input, List<ItemStack> comps) {
        return this.instability;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    public InfusionRecipe setGroup(ResourceLocation s) {
        this.group = s.toString();
        return this;
    }
}
