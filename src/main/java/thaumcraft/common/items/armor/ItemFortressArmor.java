package thaumcraft.common.items.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.common.lib.TCDataComponents;

/**
 * «Таумиумная крепость» (порт ItemFortressArmor). Шлем принимает очки откровения (компонент thaumcraft:fortress_goggles) и
 * одну из масок (thaumcraft:fortress_mask: 0 — «Ухмыляющийся дьявол», 1 — «Злой призрак», 2 — «Пьющий изверг»).
 * Защита — см. FortressArmorEvents (замена ISpecialArmor, DECISIONS 107). Модель — ModelFortressArmor (клиент).
 */
public class ItemFortressArmor extends ArmorItem implements IGoggles, IRevealer {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/fortress_armor.png");

    public ItemFortressArmor(Holder<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEX;
    }

    public static int getMask(ItemStack stack) {
        return stack.getOrDefault(TCDataComponents.FORTRESS_MASK.get(), -1);
    }

    public static boolean hasGoggles(ItemStack stack) {
        return stack.has(TCDataComponents.FORTRESS_GOGGLES.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (hasGoggles(stack)) {
            tooltip.add(Component.translatable("item.thaumcraft.goggles").withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (getMask(stack) >= 0) {
            tooltip.add(Component.translatable("item.fortress_helm.mask." + getMask(stack)).withStyle(ChatFormatting.GOLD));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity player) {
        return hasGoggles(stack);
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity player) {
        return hasGoggles(stack);
    }
}
