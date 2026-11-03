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
            } else if (recipe instanceof thaumcraft.common.lib.crafting.InfusionEnchantmentRecipe enchantment) {
                data.put("kind", "enchantment");
                data.put("enchantment", enchantment.getEnchantment().name());
                data.put("aspects", aspects(enchantment.getAspects()));
                data.put("instability", enchantment.getInstability(null, ItemStack.EMPTY, java.util.List.of()));
                data.put("components", enchantment.getComponents().size());
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
                if (infusion.getRecipeOutput() instanceof ItemStack stack) data.put("count", stack.getCount());
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
                result.put(holder.id().toString(), data);
            }
        }
        Files.writeString(Path.of("../build/recipe-audit-actual.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result));
        Thaumcraft.log.info("[TC-RECIPE-DUMP] {} recipes exported", result.size());
    }

    private static Map<String, Integer> aspects(AspectList list) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (list != null) for (var aspect : list.getAspects()) result.put(aspect.getTag(), list.getAmount(aspect));
        return result;
    }
}
