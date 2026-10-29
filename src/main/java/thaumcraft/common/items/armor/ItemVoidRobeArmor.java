package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;

/**
 * Пустотная роба тауматурга (порт ItemVoidRobeArmor): скидка Вис 5%, Искажение 3, капюшон — очки откровения; самопочинка
 * (как у пустотного металла: надетая — 2 ед./с); окрашивается (окрашиваемый слой — void_robe_armor_overlay, сверху —
 * void_robe_armor, как в 1.12), отмывается в котле. Модель — ModelRobe. Защита от магии — FortressArmorEvents.
 */
public class ItemVoidRobeArmor extends ArmorItem implements IVisDiscountGear, IGoggles, IRevealer, IWarpingGear {
    public static final int DEFAULT_COLOR = 6961280;

    public ItemVoidRobeArmor(Holder<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
    }

    public static int getColor(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, DEFAULT_COLOR);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath("thaumcraft",
                layer.dyeable() ? "textures/entity/armor/void_robe_armor_overlay.png" : "textures/entity/armor/void_robe_armor.png");
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || entity.tickCount % 20 != 0) {
            return;
        }
        if (stack.isDamaged() && entity instanceof LivingEntity) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
        if (stack.isDamaged() && ItemVoidArmor.isWorn(stack, entity, slot)) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity player) {
        return this.getType() == Type.HELMET;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity player) {
        return this.getType() == Type.HELMET;
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 5;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 3;
    }
}
