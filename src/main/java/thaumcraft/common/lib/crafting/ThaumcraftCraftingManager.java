package thaumcraft.common.lib.crafting;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectHelper;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.ArcaneCraftingInput;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.internal.CommonInternals;

/**
 * Аспекты предметов: поиск в таблице, бонусы и генерация из рецептов (порт части ThaumcraftCraftingManager).
 * Поиск рецептов магического верстака; тигля и инфузии — на этапах 9, 11.
 */
public class ThaumcraftCraftingManager {

    /**
     * Первый магический рецепт, подходящий к содержимому верстака, исследование которого игрок знает.
     * В оригинале исследование не проверялось нигде (только книгой) — проверка добавлена (DEVIATIONS.md).
     */
    public static IArcaneRecipe findMatchingArcaneRecipe(ArcaneCraftingInput matrix, Player player) {
        RecipeHolder<IArcaneRecipe> h = findMatchingArcaneRecipeHolder(matrix, player);
        return h == null ? null : h.value();
    }

    public static RecipeHolder<IArcaneRecipe> findMatchingArcaneRecipeHolder(ArcaneCraftingInput matrix, Player player) {
        if (matrix.isEmpty()) {
            return null;
        }
        for (RecipeHolder<IArcaneRecipe> h : player.level().getRecipeManager().getAllRecipesFor(ConfigRecipes.ARCANE_CRAFTING.get())) {
            String research = h.value().getResearch();
            if ((research == null || research.isEmpty() || ThaumcraftCapabilities.knowsResearchStrict(player, research))
                    && h.value().matches(matrix, player.level())) {
                return h;
            }
        }
        return null;
    }

    public static ItemStack findMatchingArcaneRecipeResult(ArcaneCraftingInput awb, Player player) {
        IArcaneRecipe var13 = findMatchingArcaneRecipe(awb, player);
        return var13 == null ? null : var13.assemble(awb, player.level().registryAccess());
    }

    public static AspectList findMatchingArcaneRecipeCrystals(ArcaneCraftingInput awb, Player player) {
        IArcaneRecipe var13 = findMatchingArcaneRecipe(awb, player);
        return var13 == null ? null : var13.getCrystals();
    }

    public static int findMatchingArcaneRecipeVis(ArcaneCraftingInput awb, Player player) {
        IArcaneRecipe var13 = findMatchingArcaneRecipe(awb, player);
        return var13 == null ? 0 : var13.getVis();
    }

    public static AspectList getObjectTags(ItemStack itemstack) {
        return getObjectTags(itemstack, null);
    }

    /**
     * Порядок поиска как в оригинале: точный стек → (metadata 32767 — в порте совпадает с точным, т.к. повреждение
     * не входит в ключ) → стек без NBT/компонентов → генерация из рецептов. Затем бонусы и ограничение 500.
     */
    public static AspectList getObjectTags(ItemStack itemstack, ArrayList<String> history) {
        if (itemstack.isEmpty()) {
            return null;
        }
        var tags = CommonInternals.objectTags();
        AspectList tmp = tags.get(CommonInternals.generateUniqueItemstackId(itemstack));
        if (tmp == null) {
            try {
                tmp = tags.get(CommonInternals.generateUniqueItemstackIdStripped(itemstack));
                if (tmp == null) {
                    tmp = generateTags(itemstack, history);
                }
            } catch (Exception ignored) {
            }
        }
        return capAspects(getBonusTags(itemstack, tmp), 500);
    }

    private static AspectList capAspects(AspectList sourcetags, int amount) {
        if (sourcetags == null) {
            return null;
        }
        AspectList out = new AspectList();
        for (Aspect aspect : sourcetags.getAspects()) {
            if (aspect != null) {
                out.merge(aspect, Math.min(amount, sourcetags.getAmount(aspect)));
            }
        }
        return out;
    }

    /** В 1.12 — ToolMaterial.getHarvestLevel(): WOOD/GOLD 0, STONE 1, IRON 2, DIAMOND 3. В 1.21 — по тегу «неподходящих блоков» тира. */
    private static int harvestLevel(Tier tier) {
        TagKey<Block> t = tier.getIncorrectBlocksForDrops();
        if (t == BlockTags.INCORRECT_FOR_WOODEN_TOOL || t == BlockTags.INCORRECT_FOR_GOLD_TOOL) {
            return 0;
        }
        if (t == BlockTags.INCORRECT_FOR_STONE_TOOL) {
            return 1;
        }
        if (t == BlockTags.INCORRECT_FOR_IRON_TOOL) {
            return 2;
        }
        if (t == BlockTags.INCORRECT_FOR_DIAMOND_TOOL) {
            return 3;
        }
        if (t == BlockTags.INCORRECT_FOR_NETHERITE_TOOL) {
            return 4;
        }
        return -1;
    }

    private static AspectList getBonusTags(ItemStack itemstack, AspectList sourcetags) {
        AspectList tmp = new AspectList();
        if (itemstack.isEmpty()) {
            return tmp;
        }
        Item item = itemstack.getItem();
        if (item instanceof IEssentiaContainerItem container && !container.ignoreContainedAspects()) {
            if (sourcetags != null) {
                sourcetags.aspects.clear();
            }
            tmp = container.getAspects(itemstack);
            if (tmp != null && tmp.size() > 0) {
                for (Aspect tag : tmp.copy().getAspects()) {
                    if (tmp.getAmount(tag) <= 0) {
                        tmp.remove(tag);
                    }
                }
            }
        }
        if (tmp == null) {
            tmp = new AspectList();
        }
        if (sourcetags != null) {
            for (Aspect tag : sourcetags.getAspects()) {
                if (tag != null) {
                    tmp.add(tag, sourcetags.getAmount(tag));
                }
            }
        }

        if (item instanceof ArmorItem armor) {
            tmp.merge(Aspect.PROTECT, armor.getDefense() * 4);
        } else if (item instanceof SwordItem sword && sword.getTier().getAttackDamageBonus() + 1.0F > 0.0F) {
            // ItemSword.getAttackDamage() в 1.12 = ToolMaterial.getAttackDamage() = Tier.getAttackDamageBonus()
            tmp.merge(Aspect.AVERSION, (int) (sword.getTier().getAttackDamageBonus() + 1.0F) * 4);
        } else if (item instanceof BowItem) {
            tmp.merge(Aspect.AVERSION, 10).merge(Aspect.FLIGHT, 5);
        } else if (item instanceof DiggerItem digger && !(item instanceof HoeItem)) {
            // ItemTool (кирка, топор, лопата); мотыга в 1.12 не ItemTool
            int level = harvestLevel(digger.getTier());
            if (level >= 0) {
                tmp.merge(Aspect.TOOL, (level + 1) * 4);
            }
        } else if (item instanceof ShearsItem || item instanceof HoeItem) {
            int max = itemstack.getMaxDamage();
            if (max <= Tiers.WOOD.getUses()) {
                tmp.merge(Aspect.TOOL, 4);
            } else if (max <= Tiers.STONE.getUses() || max <= Tiers.GOLD.getUses()) {
                tmp.merge(Aspect.TOOL, 8);
            } else if (max <= Tiers.IRON.getUses()) {
                tmp.merge(Aspect.TOOL, 12);
            } else {
                tmp.merge(Aspect.TOOL, 16);
            }
        }

        // Красители OreDictionary (dyeBlack…dyeWhite) → тег c:dyes
        if (itemstack.is(Tags.Items.DYES)) {
            tmp.merge(Aspect.SENSES, 5);
        }

        ItemEnchantments ench = itemstack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (ench.isEmpty()) {
            ench = itemstack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        if (!ench.isEmpty()) {
            int var5 = 0;
            for (var entry : ench.entrySet()) {
                Holder<Enchantment> e = entry.getKey();
                int lvl = entry.getIntValue() * 3;
                if (e.is(Enchantments.AQUA_AFFINITY)) {
                    tmp.merge(Aspect.WATER, lvl);
                } else if (e.is(Enchantments.BANE_OF_ARTHROPODS)) {
                    tmp.merge(Aspect.BEAST, lvl / 2).merge(Aspect.AVERSION, lvl / 2);
                } else if (e.is(Enchantments.BLAST_PROTECTION)) {
                    tmp.merge(Aspect.PROTECT, lvl / 2).merge(Aspect.ENTROPY, lvl / 2);
                } else if (e.is(Enchantments.EFFICIENCY)) {
                    tmp.merge(Aspect.TOOL, lvl);
                } else if (e.is(Enchantments.FEATHER_FALLING)) {
                    tmp.merge(Aspect.FLIGHT, lvl);
                } else if (e.is(Enchantments.FIRE_ASPECT)) {
                    tmp.merge(Aspect.FIRE, lvl / 2).merge(Aspect.AVERSION, lvl / 2);
                } else if (e.is(Enchantments.FIRE_PROTECTION)) {
                    tmp.merge(Aspect.PROTECT, lvl / 2).merge(Aspect.FIRE, lvl / 2);
                } else if (e.is(Enchantments.FLAME)) {
                    tmp.merge(Aspect.FIRE, lvl);
                } else if (e.is(Enchantments.FORTUNE)) {
                    tmp.merge(Aspect.DESIRE, lvl);
                } else if (e.is(Enchantments.INFINITY)) {
                    tmp.merge(Aspect.CRAFT, lvl);
                } else if (e.is(Enchantments.KNOCKBACK)) {
                    tmp.merge(Aspect.AIR, lvl);
                } else if (e.is(Enchantments.LOOTING)) {
                    tmp.merge(Aspect.DESIRE, lvl);
                } else if (e.is(Enchantments.POWER)) {
                    tmp.merge(Aspect.AVERSION, lvl);
                } else if (e.is(Enchantments.PROJECTILE_PROTECTION)) {
                    tmp.merge(Aspect.PROTECT, lvl);
                } else if (e.is(Enchantments.PROTECTION)) {
                    tmp.merge(Aspect.PROTECT, lvl);
                } else if (e.is(Enchantments.PUNCH)) {
                    tmp.merge(Aspect.AIR, lvl);
                } else if (e.is(Enchantments.RESPIRATION)) {
                    tmp.merge(Aspect.AIR, lvl);
                } else if (e.is(Enchantments.SHARPNESS)) {
                    tmp.merge(Aspect.AVERSION, lvl);
                } else if (e.is(Enchantments.SILK_TOUCH)) {
                    tmp.merge(Aspect.EXCHANGE, lvl);
                } else if (e.is(Enchantments.THORNS)) {
                    tmp.merge(Aspect.AVERSION, lvl);
                } else if (e.is(Enchantments.SMITE)) {
                    tmp.merge(Aspect.UNDEAD, lvl / 2).merge(Aspect.AVERSION, lvl / 2);
                } else if (e.is(Enchantments.UNBREAKING)) {
                    tmp.merge(Aspect.EARTH, lvl);
                } else if (e.is(Enchantments.DEPTH_STRIDER)) {
                    tmp.merge(Aspect.WATER, lvl);
                } else if (e.is(Enchantments.LUCK_OF_THE_SEA)) {
                    tmp.merge(Aspect.DESIRE, lvl);
                } else if (e.is(Enchantments.LURE)) {
                    tmp.merge(Aspect.BEAST, lvl);
                } else if (e.is(Enchantments.FROST_WALKER)) {
                    tmp.merge(Aspect.COLD, lvl);
                } else if (e.is(Enchantments.MENDING)) {
                    tmp.merge(Aspect.CRAFT, lvl);
                }
                // Редкость 1.12 ↔ вес 1.21: UNCOMMON = 5, RARE = 2, VERY_RARE = 1
                int weight = e.value().getWeight();
                if (weight == 5) {
                    var5 += 2;
                } else if (weight == 2) {
                    var5 += 4;
                } else if (weight == 1) {
                    var5 += 6;
                }
                var5 += lvl;
            }
            if (var5 > 0) {
                tmp.merge(Aspect.MAGIC, var5);
            }
        }
        return AspectHelper.cullTags(tmp);
    }

    public static AspectList generateTags(ItemStack is) {
        return generateTags(is, new ArrayList<>());
    }

    public static AspectList generateTags(ItemStack is, ArrayList<String> history) {
        if (history == null) {
            history = new ArrayList<>();
        }
        ItemStack stack = is.copyWithCount(1);
        if (ThaumcraftApi.exists(stack)) {
            return getObjectTags(stack, history);
        }
        String ss = CommonInternals.generateUniqueItemstackId(stack).toString();
        if (history.contains(ss)) {
            return null;
        }
        history.add(ss);
        if (history.size() < 100) {
            AspectList ret = generateTagsFromRecipes(stack, history);
            ret = capAspects(ret, 500);
            ThaumcraftApi.registerObjectTag(is, ret);
            return ret;
        }
        return null;
    }

    private static AspectList generateTagsFromRecipes(ItemStack stack, ArrayList<String> history) {
        // TODO этап 9: generateTagsFromCrucibleRecipes; этап 11: generateTagsFromInfusionRecipes
        return generateTagsFromCraftingRecipes(stack, history);
    }

    private static AspectList generateTagsFromCraftingRecipes(ItemStack stack, ArrayList<String> history) {
        CommonInternals.SideState state = CommonInternals.state();
        RecipeManager recipes = state.recipes;
        RegistryAccess registries = state.registries;
        if (recipes == null || registries == null) {
            return null;
        }
        AspectList ret = null;
        int value = Integer.MAX_VALUE;
        // в 1.12 магические рецепты лежали в общем реестре рецептов верстака
        List<Recipe<?>> all = new ArrayList<>();
        for (RecipeHolder<CraftingRecipe> holder : recipes.getAllRecipesFor(RecipeType.CRAFTING)) {
            all.add(holder.value());
        }
        for (RecipeHolder<IArcaneRecipe> holder : recipes.getAllRecipesFor(ConfigRecipes.ARCANE_CRAFTING.get())) {
            all.add(holder.value());
        }
        for (Recipe<?> recipe : all) {
            ItemStack out = recipe.getResultItem(registries);
            if (out.isEmpty() || out.getItem() != stack.getItem()) {
                continue;
            }
            try {
                AspectList ph = getAspectsFromIngredients(recipe.getIngredients(), out, recipe, history);
                if (recipe instanceof IArcaneRecipe ar && ar.getVis() > 0) {
                    ph.add(Aspect.MAGIC, (int) (Math.sqrt(1 + ar.getVis() / 2) / out.getCount()));
                }
                for (Aspect as : ph.copy().getAspects()) {
                    if (ph.getAmount(as) <= 0) {
                        ph.remove(as);
                    }
                }
                if (ph.visSize() < value && ph.visSize() > 0) {
                    ret = ph;
                    value = ph.visSize();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * Сумма аспектов ингредиентов (первый подходящий предмет каждого) минус остатки рецепта (вёдра и т.п.), × 0.75 / количество.
     * В оригинале при recipe == null (инфузия) остатки не вычислялись и цикл по ним падал с NPE — поведение сохранено.
     */
    static AspectList getAspectsFromIngredients(List<Ingredient> ingredients, ItemStack recipeOut, Recipe<?> recipe, ArrayList<String> history) {
        AspectList out = new AspectList();
        AspectList mid = new AspectList();
        NonNullList<ItemStack> exlist = null;
        if (recipe != null) {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                items.add(ItemStack.EMPTY);
            }
            int index = 0;
            for (Ingredient is : ingredients) {
                ItemStack[] matching = is.getItems();
                if (matching.length > 0 && index < 9) {
                    items.set(index, matching[0]);
                }
                index++;
            }
            if (recipe instanceof IArcaneRecipe ar) {
                exlist = ar.getRemainingItems(ArcaneCraftingInput.of(items));
            } else if (recipe instanceof CraftingRecipe cr) {
                exlist = cr.getRemainingItems(CraftingInput.of(3, 3, items));
            }
        }
        for (Ingredient is : ingredients) {
            ItemStack[] matching = is.getItems();
            if (matching.length > 0) {
                AspectList obj = getObjectTags(matching[0], history);
                if (obj != null) {
                    for (Aspect as : obj.getAspects()) {
                        if (as != null) {
                            mid.add(as, obj.getAmount(as));
                        }
                    }
                }
            }
        }
        for (ItemStack ri : exlist) {
            if (!ri.isEmpty()) {
                AspectList obj = getObjectTags(ri, history);
                for (Aspect as : obj.getAspects()) {
                    mid.reduce(as, obj.getAmount(as));
                }
            }
        }
        for (Aspect as : mid.getAspects()) {
            if (as != null) {
                float v = mid.getAmount(as) * 0.75F / recipeOut.getCount();
                if (v < 1.0F && v > 0.75) {
                    v = 1.0F;
                }
                out.add(as, (int) v);
            }
        }
        for (Aspect as : out.getAspects()) {
            if (out.getAmount(as) <= 0) {
                out.remove(as);
            }
        }
        return out;
    }
}
