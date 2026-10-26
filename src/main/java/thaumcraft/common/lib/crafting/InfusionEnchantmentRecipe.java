package thaumcraft.common.lib.crafting;

import java.util.List;
import java.util.Random;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.lib.TCDataComponents;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

/**
 * Рецепт наполняющего зачарования (порт InfusionEnchantmentRecipe): центральный предмет — любой подходящего вида, уровень
 * зачарования +1. Стоимость растёт с уровнем и числом уже наложенных зачарований; с вероятностью (число зачарований)/10
 * предмет получает +1 Искажения.
 */
public class InfusionEnchantmentRecipe extends InfusionRecipe {
    final EnumInfusionEnchantment enchantment;

    public InfusionEnchantmentRecipe(EnumInfusionEnchantment ench, AspectList as, Object... components) {
        super(ench.research, null, 4, as, Ingredient.EMPTY, components);
        this.enchantment = ench;
    }

    /** «Поддельный» рецепт для книги: тот же, но с показанным центральным предметом. */
    public InfusionEnchantmentRecipe(InfusionEnchantmentRecipe recipe, ItemStack in) {
        super(recipe.enchantment.research, null, recipe.instability, recipe.aspects, in, recipe.components.toArray());
        this.enchantment = recipe.enchantment;
    }

    public EnumInfusionEnchantment getEnchantment() {
        return this.enchantment;
    }

    /** Подходит ли предмет под виды зачарования (toolClasses). TODO этап 13: безделушки (Curios) и заряжаемые предметы. */
    static boolean fits(EnumInfusionEnchantment ench, ItemStack central) {
        if (ench.toolClasses.contains("all")) {
            return true;
        }
        if (ench.toolClasses.contains("weapon")
                && central.getAttributeModifiers().modifiers().stream().anyMatch(e -> e.attribute().value() == Attributes.ATTACK_DAMAGE.value())) {
            return true;
        }
        if (ench.toolClasses.contains("axe") && central.canPerformAction(ItemAbilities.AXE_DIG)
                || ench.toolClasses.contains("pickaxe") && central.canPerformAction(ItemAbilities.PICKAXE_DIG)
                || ench.toolClasses.contains("shovel") && central.canPerformAction(ItemAbilities.SHOVEL_DIG)) {
            return true;
        }
        if (central.getItem() instanceof ArmorItem armor) {
            String at = switch (armor.getType()) {
                case HELMET -> "helm";
                case CHESTPLATE -> "chest";
                case LEGGINGS -> "legs";
                case BOOTS -> "boots";
                default -> "none";
            };
            return ench.toolClasses.contains("armor") || ench.toolClasses.contains(at);
        }
        return false;
    }

    @Override
    public boolean matches(List<ItemStack> input, ItemStack central, Level world, Player player) {
        if (central == null || central.isEmpty() || !ThaumcraftCapabilities.knowsResearch(player, this.research)) {
            return false;
        }
        if (EnumInfusionEnchantment.getInfusionEnchantmentLevel(central, this.enchantment) >= this.enchantment.maxLevel) {
            return false;
        }
        if (!fits(this.enchantment, central)) {
            return false;
        }
        return (this.getRecipeInput() == Ingredient.EMPTY || this.getRecipeInput().test(central))
                && RecipeMatcher.findMatches(input, this.getComponents()) != null;
    }

    @Override
    public Object getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        ItemStack out = input.copy();
        int cl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(out, this.enchantment);
        if (cl >= this.enchantment.maxLevel) {
            return null;
        }
        List<EnumInfusionEnchantment> el = EnumInfusionEnchantment.getInfusionEnchantments(input);
        Random rand = new Random(System.nanoTime());
        if (rand.nextInt(10) < el.size()) {
            int base = 1 + input.getOrDefault(TCDataComponents.WARPING.get(), 0);
            out.set(TCDataComponents.WARPING.get(), base);
        }
        EnumInfusionEnchantment.addInfusionEnchantment(out, this.enchantment, cl + 1);
        return out;
    }

    @Override
    public AspectList getAspects(Player player, ItemStack input, List<ItemStack> comps) {
        AspectList out = new AspectList();
        if (input == null || input.isEmpty()) {
            return out;
        }
        int cl = EnumInfusionEnchantment.getInfusionEnchantmentLevel(input, this.enchantment) + 1;
        if (cl > this.enchantment.maxLevel) {
            return out;
        }
        List<EnumInfusionEnchantment> el = EnumInfusionEnchantment.getInfusionEnchantments(input);
        int otherEnchantments = el.size();
        if (el.contains(this.enchantment)) {
            otherEnchantments--;
        }
        float modifier = cl + otherEnchantments * 0.33F;
        for (Aspect a : this.getAspects().getAspects()) {
            out.add(a, (int) (this.getAspects().getAmount(a) * modifier));
        }
        return out;
    }
}
