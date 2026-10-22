package thaumcraft.api.crafting;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Рецепт тигля: брошенный в тигель катализатор и растворённые в нём аспекты превращаются в результат.
 * Регистрируется в каталоге кодом (ThaumcraftApi.addCrucibleRecipe), как в оригинале.
 */
public class CrucibleRecipe implements IThaumcraftRecipe {
    private ItemStack recipeOutput;
    private Ingredient catalyst;
    private AspectList aspects;
    private String research;
    private String name;
    /** Отпечаток рецепта (тауматориум запоминает выбранные рецепты по нему); одинаков на клиенте и сервере. */
    public int hash;
    private String group = "";

    /**
     * @param catalyst ItemStack (с компонентами — совпадение по ним), Item, Block, TagKey&lt;Item&gt; или Ingredient
     */
    public CrucibleRecipe(String researchKey, ItemStack result, Object catalyst, AspectList tags) {
        this.recipeOutput = result;
        this.name = "";
        this.setAspects(tags);
        this.research = researchKey;
        this.setCatalyst(ThaumcraftApiHelper.getIngredient(catalyst));
        if (this.getCatalyst() == null) {
            throw new RuntimeException("Invalid crucible recipe catalyst: " + catalyst);
        }
        this.generateHash();
    }

    /**
     * В оригинале — из исследования, результата и всех подходящих катализаторов. Подходящие предметы по тегам при загрузке
     * ещё неизвестны, поэтому в порте берётся описание катализатора (предметы/теги), а не их список.
     */
    private void generateHash() {
        String hc = this.research;
        hc = hc + BuiltInRegistries.ITEM.getKey(this.recipeOutput.getItem()) + "x" + this.recipeOutput.getCount();
        if (!this.recipeOutput.getComponentsPatch().isEmpty()) {
            hc = hc + this.recipeOutput.getComponentsPatch();
        }
        try {
            JsonElement json = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, this.getCatalyst()).getOrThrow();
            hc = hc + json;
        } catch (Exception e) {
            hc = hc + this.getCatalyst();
        }
        this.hash = hc.hashCode();
    }

    public boolean matches(AspectList itags, ItemStack cat) {
        if (!this.getCatalyst().test(cat)) {
            return false;
        }
        if (itags == null) {
            return false;
        }
        for (Aspect tag : this.getAspects().getAspects()) {
            if (itags.getAmount(tag) < this.getAspects().getAmount(tag)) {
                return false;
            }
        }
        return true;
    }

    public boolean catalystMatches(ItemStack cat) {
        return this.getCatalyst().test(cat);
    }

    public AspectList removeMatching(AspectList itags) {
        AspectList temptags = new AspectList();
        temptags.aspects.putAll(itags.aspects);
        for (Aspect tag : this.getAspects().getAspects()) {
            temptags.remove(tag, this.getAspects().getAmount(tag));
        }
        return temptags;
    }

    public ItemStack getRecipeOutput() {
        return this.recipeOutput;
    }

    @Override
    public String getResearch() {
        return this.research;
    }

    public Ingredient getCatalyst() {
        return this.catalyst;
    }

    public void setCatalyst(Ingredient catalyst) {
        this.catalyst = catalyst;
    }

    public AspectList getAspects() {
        return this.aspects;
    }

    public void setAspects(AspectList aspects) {
        this.aspects = aspects;
    }

    @Override
    public String getGroup() {
        return this.group;
    }

    public CrucibleRecipe setGroup(ResourceLocation s) {
        this.group = s.toString();
        return this;
    }
}
