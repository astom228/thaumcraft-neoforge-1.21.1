package thaumcraft.common.lib.crafting;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.internal.CommonInternals;

/** Рецепт postAspects оригинала: неизменяемое описание и стоимость из таблицы аспектов текущей логической стороны. */
public final class DerivedCrucibleRecipe extends CrucibleRecipe {
    private final String key;
    private final ItemStack costItem;
    private final ItemStack subtract;

    public DerivedCrucibleRecipe(String key, String research, ItemStack output, Object catalyst, ItemStack subtract) {
        super(research, output, catalyst, new AspectList());
        this.key = key;
        this.costItem = output.copyWithCount(1);
        this.subtract = subtract == null ? ItemStack.EMPTY : subtract.copy();
    }

    @Override
    public AspectList getAspects() {
        AspectList cost = CommonInternals.state().derivedCrucibleCosts.get(this.key);
        return cost == null ? new AspectList() : cost.copy();
    }

    @Override
    public boolean matches(AspectList aspects, ItemStack catalyst) {
        return CommonInternals.state().derivedCrucibleCosts.containsKey(this.key) && super.matches(aspects, catalyst);
    }

    /** После ConfigAspects.initItems; публикация готовой таблицы одной заменой ссылки, без изменения общего каталога. */
    public static void rebuildCosts() {
        Map<String, AspectList> costs = new LinkedHashMap<>();
        synchronized (CommonInternals.craftingRecipeCatalog) {
            for (var recipe : ThaumcraftApi.getCraftingRecipes().values()) {
                if (recipe instanceof DerivedCrucibleRecipe derived) {
                    AspectList cost = new AspectList(derived.costItem);
                    if (!derived.subtract.isEmpty()) cost.remove(new AspectList(derived.subtract));
                    costs.put(derived.key, cost);
                }
            }
        }
        CommonInternals.state().derivedCrucibleCosts = Map.copyOf(costs);
    }
}
