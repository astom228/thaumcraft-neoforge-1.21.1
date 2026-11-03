package thaumcraft.common.lib.events;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import com.google.gson.GsonBuilder;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import thaumcraft.Thaumcraft;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import thaumcraft.api.crafting.IArcaneRecipe;
import thaumcraft.api.crafting.InfusionRecipe;

/** Шаблон временного дампа для scripts/check-recipes.ps1; в основной код и итоговый jar не включается. */
@EventBusSubscriber(modid = Thaumcraft.MODID)
public class DebugRecipeDump {
    @SubscribeEvent
    public static void started(ServerStartedEvent event) throws java.io.IOException {
        if (!Files.exists(Path.of("tc-recipe-dump"))) return;
        Map<String, Object> result = new LinkedHashMap<>();
        ThaumcraftApi.getCraftingRecipes().forEach((id, recipe) -> {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("research", recipe.getResearch());
            if (recipe instanceof CrucibleRecipe crucible) {
                data.put("kind", "crucible");
                data.put("aspects", aspects(crucible.getAspects()));
                data.put("count", crucible.getRecipeOutput().getCount());
                data.put("resultItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(crucible.getRecipeOutput().getItem()).toString());
                data.put("catalyst", ingredient(crucible.getCatalyst(), event.getServer().registryAccess()));
                var resultAspects = crucible.getRecipeOutput().get(thaumcraft.common.lib.TCDataComponents.ASPECTS.get());
                if (resultAspects != null) data.put("resultAspects", aspects(resultAspects));
            } else if (recipe instanceof thaumcraft.common.lib.crafting.InfusionEnchantmentRecipe enchantment) {
                data.put("kind", "enchantment");
                data.put("enchantment", enchantment.getEnchantment().name());
                data.put("aspects", aspects(enchantment.getAspects()));
                data.put("instability", enchantment.getInstability(null, ItemStack.EMPTY, java.util.List.of()));
                data.put("components", enchantment.getComponents().size());
                data.put("ingredients", enchantment.getComponents().stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
            } else if (recipe instanceof thaumcraft.common.lib.crafting.InfusionRunicAugmentRecipe runic) {
                data.put("kind", "runic");
                var samples = new java.util.ArrayList<Map<String, Object>>();
                for (int charge = 0; charge <= 6; charge++) {
                    var input = new ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE);
                    input.set(thaumcraft.common.lib.TCDataComponents.RUNIC_CHARGE.get(), charge);
                    Map<String, Object> sample = new LinkedHashMap<>();
                    sample.put("charge", charge);
                    sample.put("instability", runic.getInstability(null, input, java.util.List.of()));
                    sample.put("aspects", aspects(runic.getAspects(null, input, java.util.List.of())));
                    sample.put("components", runic.getComponents(input).size());
                    sample.put("ingredients", runic.getComponents(input).stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
                    var output = (ItemStack) runic.getRecipeOutput(null, input, java.util.List.of());
                    sample.put("outputCharge", output.get(thaumcraft.common.lib.TCDataComponents.RUNIC_CHARGE.get()));
                    samples.add(sample);
                }
                data.put("samples", samples);
            } else if (recipe.getClass() == InfusionRecipe.class) {
                var infusion = (InfusionRecipe) recipe;
                data.put("kind", "infusion");
                data.put("aspects", aspects(infusion.getAspects()));
                data.put("instability", infusion.getInstability(null, ItemStack.EMPTY, java.util.List.of()));
                data.put("components", infusion.getComponents().size());
                data.put("central", ingredient(infusion.getRecipeInput(), event.getServer().registryAccess()));
                data.put("ingredients", infusion.getComponents().stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
                if (infusion.getRecipeOutput() instanceof ItemStack stack) {
                    data.put("count", stack.getCount());
                    data.put("resultItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                } else if (infusion.getRecipeOutput() instanceof Object[] change
                        && change[0] instanceof net.minecraft.core.component.DataComponentType<?> type) {
                    data.put("resultMutation", Map.of(net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type).toString(), change[1]));
                }
            } else return;
            result.put(id.toString(), data);
        });
        for (var holder : event.getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof IArcaneRecipe arcane) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("kind", "arcane");
                data.put("research", arcane.getResearch());
                data.put("aspects", aspects(arcane.getCrystals()));
                data.put("vis", arcane.getVis());
                data.put("count", arcane.getResultItem(event.getServer().registryAccess()).getCount());
                data.put("resultItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(arcane.getResultItem(event.getServer().registryAccess()).getItem()).toString());
                data.put("ingredients", arcane.getIngredients().stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
                if (arcane instanceof thaumcraft.api.crafting.ShapedArcaneRecipe) {
                    data.put("ingredientMode", "shaped");
                    for (int width = 1; width <= 3; width++) if (arcane.canCraftInDimensions(width, 3)) { data.put("width", width); break; }
                    for (int height = 1; height <= 3; height++) if (arcane.canCraftInDimensions(3, height)) { data.put("height", height); break; }
                } else data.put("ingredientMode", "unordered");
                result.put(holder.id().toString(), data);
            } else if (holder.id().getNamespace().equals("thaumcraft") &&
                    (holder.value() instanceof net.minecraft.world.item.crafting.ShapedRecipe
                     || holder.value() instanceof net.minecraft.world.item.crafting.ShapelessRecipe
                     || holder.value() instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe)) {
                var recipe = holder.value();
                Map<String, Object> data = new LinkedHashMap<>();
                var output = recipe.getResultItem(event.getServer().registryAccess());
                data.put("resultItem", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(output.getItem()).toString());
                data.put("count", output.getCount());
                var resultAspects = output.get(thaumcraft.common.lib.TCDataComponents.ASPECTS.get());
                if (resultAspects != null) data.put("resultAspects", aspects(resultAspects));
                if (recipe instanceof net.minecraft.world.item.crafting.AbstractCookingRecipe cooking) {
                    data.put("kind", "smelting");
                    data.put("catalyst", ingredient(cooking.getIngredients().getFirst(), event.getServer().registryAccess()));
                    data.put("experience", cooking.getExperience());
                    data.put("cookingTime", cooking.getCookingTime());
                } else {
                    data.put("kind", "crafting");
                    data.put("ingredients", recipe.getIngredients().stream().map(i -> ingredient(i, event.getServer().registryAccess())).toList());
                    if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
                        data.put("ingredientMode", "shaped");
                        data.put("width", shaped.getWidth());
                        data.put("height", shaped.getHeight());
                    } else data.put("ingredientMode", "unordered");
                }
                result.put(holder.id().toString(), data);
            }
        }
        Files.writeString(Path.of("../build/recipe-audit-actual.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
        var sample = thaumcraft.api.ThaumcraftApiHelper.makeCrystal(thaumcraft.api.aspects.Aspect.AIR);
        var componentIngredient = thaumcraft.api.ThaumcraftApiHelper.getIngredient(sample);
        var named = sample.copy();
        named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Recipe audit"));
        named.set(net.minecraft.core.component.DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(net.minecraft.network.chat.Component.literal("Extra lore"))));
        if (!componentIngredient.test(named) || componentIngredient.test(thaumcraft.api.ThaumcraftApiHelper.makeCrystal(thaumcraft.api.aspects.Aspect.FIRE))) {
            throw new IllegalStateException("[TC-RECIPE-DUMP] component ingredient regression");
        }
        var wrongAmount = sample.copy();
        wrongAmount.set(thaumcraft.common.lib.TCDataComponents.ASPECTS.get(), new AspectList().add(thaumcraft.api.aspects.Aspect.AIR, 2));
        if (componentIngredient.test(wrongAmount)) throw new IllegalStateException("[TC-RECIPE-DUMP] aspect amount regression");
        Thaumcraft.log.info("[TC-RECIPE-DUMP] PASS: named/lore crystal accepted, wrong aspect and amount rejected");
        var phial = (net.minecraft.world.item.crafting.ShapedRecipe) event.getServer().getRecipeManager()
                .byKey(net.minecraft.resources.ResourceLocation.parse("thaumcraft:phial")).orElseThrow().value();
        for (var glass : java.util.List.of(net.minecraft.world.item.Items.GLASS, net.minecraft.world.item.Items.WHITE_STAINED_GLASS,
                net.minecraft.world.item.Items.BLACK_STAINED_GLASS, net.minecraft.world.item.Items.TINTED_GLASS)) {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(ItemStack.EMPTY,
                    new ItemStack(net.minecraft.world.item.Items.CLAY_BALL), ItemStack.EMPTY, new ItemStack(glass), ItemStack.EMPTY,
                    new ItemStack(glass), ItemStack.EMPTY, new ItemStack(glass), ItemStack.EMPTY));
            boolean wanted = glass != net.minecraft.world.item.Items.TINTED_GLASS;
            if (phial.matches(input, event.getServer().overworld()) != wanted) throw new IllegalStateException("[TC-RECIPE-DUMP] phial glass regression: " + glass);
        }
        Thaumcraft.log.info("[TC-RECIPE-DUMP] PASS: phials accept original glass and reject tinted glass");
        Thaumcraft.log.info("[TC-RECIPE-DUMP] {} recipes exported", result.size());
    }

    private static Map<String, Integer> aspects(AspectList list) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (list != null) for (var aspect : list.getAspects()) result.put(aspect.getTag(), list.getAmount(aspect));
        return result;
    }

    private static com.google.gson.JsonElement ingredient(net.minecraft.world.item.crafting.Ingredient value, net.minecraft.core.RegistryAccess registries) {
        return net.minecraft.world.item.crafting.Ingredient.CODEC.encodeStart(registries.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), value).getOrThrow();
    }
}
