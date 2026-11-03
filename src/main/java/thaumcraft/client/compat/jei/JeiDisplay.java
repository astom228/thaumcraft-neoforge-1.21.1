package thaumcraft.client.compat.jei;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.component.DataComponents;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.lib.crafting.InfusionPreview;

/** Снимок данных для одного отображения; не содержит изменяемого состояния игрового рецепта. */
public record JeiDisplay(ResourceLocation id, ResourceLocation source, Kind kind, List<List<ItemStack>> inputs, int width, boolean shapeless,
                         List<ItemStack> central, List<ItemStack> outputs, AspectList aspects,
                         int vis, int instability, String research, int warpChance) {
    public enum Kind { ARCANE, CRUCIBLE, INFUSION, ASPECTS }

    public static List<ItemStack> stacks(Ingredient ingredient) {
        return Arrays.stream(ingredient.getItems()).map(ItemStack::copy).toList();
    }

    public static ResourceLocation variantId(ResourceLocation recipe, ItemStack input) {
        var item = BuiltInRegistries.ITEM.getKey(input.getItem());
        ItemStack normalized = input.copyWithCount(1);
        if (normalized.getOrDefault(TCDataComponents.RUNIC_CHARGE.get(), 0) == 0) normalized.remove(TCDataComponents.RUNIC_CHARGE.get());
        var enchantments = normalized.get(TCDataComponents.INFUSION_ENCHANTMENTS.get());
        if (enchantments != null) normalized.set(TCDataComponents.INFUSION_ENCHANTMENTS.get(), enchantments.stream().sorted(java.util.Comparator.comparing(e -> e.id())).toList());
        var originalPatch = normalized.getComponentsPatch();
        var patch = originalPatch.forget(t -> t == DataComponents.DAMAGE || t == DataComponents.CUSTOM_NAME || t == DataComponents.LORE
                || originalPatch.get(t).isEmpty());
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "jei/" + recipe.getNamespace() + "/" + recipe.getPath()
                + "/" + item.getNamespace() + "/" + item.getPath() + "/"
                + Integer.toHexString(patch.toString().hashCode()));
    }

    public static JeiDisplay infusion(ResourceLocation id, InfusionRecipe recipe, InfusionPreview preview) {
        return new JeiDisplay(variantId(id, preview.input()), id, Kind.INFUSION, preview.components(), 9, false,
                List.of(preview.input()), List.of(preview.output()), preview.aspects(), 0,
                preview.instability(), recipe.getResearch(), preview.warpChance());
    }
}
