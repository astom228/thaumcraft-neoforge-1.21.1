package thaumcraft.common.lib.crafting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

/** Чистое описание для просмотра: без крафта, случайности, изменения входа и проверки знаний игрока. */
public record InfusionPreview(ItemStack input, ItemStack output, List<List<ItemStack>> components,
                              AspectList aspects, int instability, int warpChance) {
    public static Optional<InfusionPreview> of(InfusionRecipe recipe, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        ItemStack central = input.copyWithCount(1);
        ItemStack output;
        int warpChance = 0;
        List<List<ItemStack>> components = new ArrayList<>();
        if (recipe instanceof InfusionRunicAugmentRecipe) {
            if (!(central.getItem() instanceof ArmorItem) && !thaumcraft.common.lib.utils.BaubleUtils.isBauble(central)) return Optional.empty();
            int charge = InfusionRunicAugmentRecipe.getRunicCharge(central);
            if (charge < 0 || charge == Integer.MAX_VALUE) return Optional.empty();
            output = central.copy();
            output.set(TCDataComponents.RUNIC_CHARGE.get(), charge + 1);
            // Повторяющийся янтарь объединяется в одну стопку; число компонентов не обрезается размером экрана.
            components.add(List.of(new ItemStack(ItemsTC.salisMundus.get())));
            components.add(List.of(new ItemStack(ItemsTC.amber.get(), charge + 1)));
        } else {
            if (recipe instanceof InfusionEnchantmentRecipe enchantment) {
                var type = enchantment.getEnchantment();
                int level = EnumInfusionEnchantment.getInfusionEnchantmentLevel(central, type);
                if (!InfusionEnchantmentRecipe.fits(type, central) || level >= type.maxLevel) return Optional.empty();
                output = central.copy();
                // getRecipeOutput содержит Random и может добавить Искажение: для превью его не вызываем.
                warpChance = Math.min(10, EnumInfusionEnchantment.getInfusionEnchantments(central).size()) * 10;
                EnumInfusionEnchantment.addInfusionEnchantment(output, type, level + 1);
            } else {
                if (!recipe.getRecipeInput().test(central)) return Optional.empty();
                Object result = recipe.getRecipeOutput();
                if (result instanceof ItemStack stack) output = stack.copy();
                else if (result instanceof Object[] change && change.length == 2 && change[0] instanceof DataComponentType<?> type) {
                    output = central.copy();
                    apply(output, type, change[1]);
                } else return Optional.empty();
            }
            for (var ingredient : recipe.getComponents()) {
                components.add(Arrays.stream(ingredient.getItems()).map(ItemStack::copy).toList());
            }
        }
        return Optional.of(new InfusionPreview(central, output, List.copyOf(components),
                recipe.getAspects(null, central, List.of()).copy(), recipe.getInstability(null, central, List.of()), warpChance));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void apply(ItemStack stack, DataComponentType type, Object value) {
        stack.set(type, value);
    }
}
