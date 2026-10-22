package thaumcraft.client.gui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IThaumcraftRecipe;
import thaumcraft.api.internal.CommonInternals;

/**
 * Рецепты, на которые ссылаются страницы исследований. Порядок поиска как в оригинале: каталог Thaumcraft
 * (CommonInternals.getCatalogRecipe / getCatalogRecipeFake — магический верстак, тигель, инфузия, «поддельные» рецепты
 * для показа; добавляются на этапах 8, 9, 11), затем рецепт по ID, затем группа рецептов (ConfigRecipes.recipeGroups:
 * рецепты с одинаковым полем group, кроме minecraft:*). Только клиент: рецепты берутся из RecipeManager клиента.
 */
public final class ResearchRecipes {
    private static RecipeManager groupsFor;
    private static Map<String, List<ResourceLocation>> groups = new LinkedHashMap<>();

    /** RecipeHolder, «поддельный» рецепт (Recipe), список ID (группа) или null. */
    public static Object resolve(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        RecipeManager rm = mc.level.getRecipeManager();
        Object catalog = CommonInternals.getCatalogRecipe(id);
        if (catalog != null) {
            return catalog;
        }
        Object fake = CommonInternals.getCatalogRecipeFake(id);
        if (fake != null) {
            return fake;
        }
        RecipeHolder<?> holder = rm.byKey(id).orElse(null);
        if (holder != null) {
            return holder;
        }
        return groups(rm).get(id.toString());
    }

    /** Группы пересобираются при смене RecipeManager (вход в мир, /reload). */
    private static synchronized Map<String, List<ResourceLocation>> groups(RecipeManager rm) {
        if (groupsFor != rm) {
            Map<String, List<ResourceLocation>> g = new LinkedHashMap<>();
            for (RecipeHolder<?> h : rm.getRecipes()) {
                String group = h.value().getGroup();
                if (!group.trim().isEmpty() && !group.startsWith("minecraft")) {
                    g.computeIfAbsent(group, k -> new ArrayList<>()).add(h.id());
                }
            }
            // compileGroups оригинала: затем группы рецептов каталога (тигель, наполнение); у «поддельных» группа не заполнялась
            synchronized (CommonInternals.craftingRecipeCatalog) {
                for (Map.Entry<ResourceLocation, IThaumcraftRecipe> e : CommonInternals.craftingRecipeCatalog.entrySet()) {
                    String group = e.getValue().getGroup();
                    if (group != null && !group.trim().isEmpty()) {
                        g.computeIfAbsent(group, k -> new ArrayList<>()).add(e.getKey());
                    }
                }
            }
            groups = g;
            groupsFor = rm;
        }
        return groups;
    }

    /** Результат рецепта (для поиска в книге) или null. Для группы — результат первого рецепта. */
    public static ItemStack getRecipeOutput(ResourceLocation id) {
        Object r = resolve(id);
        if (r instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof ResourceLocation first) {
            r = resolve(first);
        }
        if (r instanceof RecipeHolder<?> h) {
            r = h.value();
        }
        if (r instanceof CrucibleRecipe cr) {
            return cr.getRecipeOutput();
        }
        if (r instanceof Recipe<?> rec) {
            Minecraft mc = Minecraft.getInstance();
            return rec.getResultItem(mc.level.registryAccess());
        }
        return null;
    }

    private ResearchRecipes() {
    }
}
