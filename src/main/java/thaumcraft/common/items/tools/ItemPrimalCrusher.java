package thaumcraft.common.items.tools;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.SimpleTier;
import thaumcraft.api.items.IWarpingGear;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.common.items.ICreativeVariants;
import thaumcraft.common.lib.enchantment.EnumInfusionEnchantment;

/**
 * Первозданный сокрушитель (порт ItemPrimalCrusher): кирка и лопата сразу (урон 3.5 + материал, скорость −2.8), материал
 * PRIMALVOID — уровень 5 (добывает всё), прочность 500, скорость 8; самопочинка, Искажение 2. В 1.12 скорость 8 — по камню,
 * металлу и наковальням (материалы) и списку «эффективных» блоков (земля, песок, снег…) — в 1.21 это блоки кирки и лопаты.
 * В оригинале рецепта нет (только творческий режим) — так и оставлено.
 */
public class ItemPrimalCrusher extends TieredItem implements IWarpingGear, ICreativeVariants {
    public static final Tier MATERIAL = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 500, 8.0F, 4.0F, 20,
            () -> Ingredient.of(ItemsTC.ingotVoid.get()));

    public ItemPrimalCrusher(Properties props) {
        super(MATERIAL, props.attributes(DiggerItem.createAttributes(MATERIAL, 3.5F, -2.8F))
                .component(DataComponents.TOOL, new Tool(List.of(Tool.Rule.deniesDrops(MATERIAL.getIncorrectBlocksForDrops()),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, MATERIAL.getSpeed()),
                        Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, MATERIAL.getSpeed())), 1.0F, 1)));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility) || ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 2;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        VoidMetalHelper.repairTick(stack, level, entity);
    }

    @Override
    public void addCreativeVariants(CreativeModeTab.Output output) {
        ItemStack w1 = new ItemStack(this);
        EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.DESTRUCTIVE, 1);
        EnumInfusionEnchantment.addInfusionEnchantment(w1, EnumInfusionEnchantment.REFINING, 1);
        output.accept(w1);
    }
}
