package thaumcraft.common.items.curios;

import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.TCDataComponents;

/**
 * «Любой зачарованный предмет» — заполнитель в требованиях исследований (порт ItemEnchantmentPlaceholder).
 * Требуемые зачарования — компонент PLACEHOLDER_ENCHANTMENTS; строки зачарований в подсказке рисуются им же
 * (в 1.12 их выводила ванильная подсказка по NBT "ench", после строки addInformation).
 */
public class ItemEnchantmentPlaceholder extends Item {
    public ItemEnchantmentPlaceholder(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("item.enchanted_placeholder.text").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_AQUA));
        Map<ResourceKey<Enchantment>, Integer> ench = stack.get(TCDataComponents.PLACEHOLDER_ENCHANTMENTS.get());
        if (ench == null) {
            return;
        }
        HolderLookup.Provider registries = context.registries();
        for (Map.Entry<ResourceKey<Enchantment>, Integer> e : ench.entrySet()) {
            Holder<Enchantment> holder = registries == null ? null
                    : registries.lookup(Registries.ENCHANTMENT).flatMap(l -> l.get(e.getKey())).orElse(null);
            if (holder != null) {
                tooltip.add(Enchantment.getFullname(holder, e.getValue()));
            } else {
                tooltip.add(Component.translatable("enchantment." + e.getKey().location().getNamespace() + "." + e.getKey().location().getPath())
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
