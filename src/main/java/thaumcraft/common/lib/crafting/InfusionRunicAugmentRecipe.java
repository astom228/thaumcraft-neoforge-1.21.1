package thaumcraft.common.lib.crafting;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.lib.TCDataComponents;

/**
 * Руническое усиление (порт InfusionRunicAugmentRecipe): доспех или безделушка получает +1 заряда рунической защиты;
 * нужны Салис Мундус и янтарь, плюс по янтарю на каждый уже имеющийся заряд. Стоимость удваивается с каждым зарядом.
 * Действие защиты — PlayerEvents.handleRunicArmor.
 */
public class InfusionRunicAugmentRecipe extends InfusionRecipe {

    public InfusionRunicAugmentRecipe() {
        super("RUNICSHIELDING", null, 0, null, Ingredient.EMPTY, ItemsTC.amber.get(), ItemsTC.salisMundus.get());
    }

    public InfusionRunicAugmentRecipe(ItemStack in) {
        super("RUNICSHIELDING", null, 0, null, in, new ItemStack(ItemsTC.salisMundus.get()), new ItemStack(ItemsTC.amber.get()));
        int fc = getRunicCharge(in);
        if (fc > 0) {
            this.components.clear();
            this.components.add(Ingredient.of(ItemsTC.salisMundus.get()));
            this.components.add(Ingredient.of(ItemsTC.amber.get()));
            for (int c = 0; c < fc; c++) {
                this.components.add(Ingredient.of(ItemsTC.amber.get()));
            }
        }
    }

    /** PlayerEvents.getRunicCharge оригинала. */
    public static int getRunicCharge(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.RUNIC_CHARGE.get(), 0);
    }

    @Override
    public boolean matches(List<ItemStack> input, ItemStack central, Level world, Player player) {
        if (this.getRecipeInput() == null || !ThaumcraftCapabilities.knowsResearch(player, this.research)) {
            return false;
        }
        if (!(central.getItem() instanceof ArmorItem) && !thaumcraft.common.lib.utils.BaubleUtils.isBauble(central)) {
            return false;
        }
        return (this.getRecipeInput() == Ingredient.EMPTY || this.getRecipeInput().test(central))
                && RecipeMatcher.findMatches(input, this.getComponents(central)) != null;
    }

    @Override
    public Object getRecipeOutput(Player player, ItemStack input, List<ItemStack> comps) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        ItemStack out = input.copy();
        out.set(TCDataComponents.RUNIC_CHARGE.get(), getRunicCharge(input) + 1);
        return out;
    }

    @Override
    public AspectList getAspects(Player player, ItemStack input, List<ItemStack> comps) {
        AspectList out = new AspectList();
        int vis = 20 + (int) (20.0 * Math.pow(2.0, getRunicCharge(input)));
        if (vis > 0) {
            out.add(Aspect.PROTECT, vis);
            out.add(Aspect.CRYSTAL, vis / 2);
            out.add(Aspect.ENERGY, vis / 2);
        }
        return out;
    }

    @Override
    public int getInstability(Player player, ItemStack input, List<ItemStack> comps) {
        return 5 + getRunicCharge(input) / 2;
    }

    public NonNullList<Ingredient> getComponents(ItemStack input) {
        NonNullList<Ingredient> com = NonNullList.create();
        com.add(Ingredient.of(ItemsTC.salisMundus.get()));
        com.add(Ingredient.of(ItemsTC.amber.get()));
        int fc = getRunicCharge(input);
        for (int c = 0; c < fc; c++) {
            com.add(Ingredient.of(ItemsTC.amber.get()));
        }
        return com;
    }
}
