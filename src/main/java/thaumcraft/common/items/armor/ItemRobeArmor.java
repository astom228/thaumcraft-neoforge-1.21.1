package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import thaumcraft.api.items.IVisDiscountGear;

/**
 * Роба тауматурга (порт ItemRobeArmor): окрашивается как кожаная броня (тег minecraft:dyeable, цвет по умолчанию 6961280),
 * отмывается в котле (ConfigItems.registerCauldron), скидка Вис 3% (ботинки — 2%), чинится зачарованной тканью.
 */
public class ItemRobeArmor extends ArmorItem implements IVisDiscountGear {
    public static final int DEFAULT_COLOR = 6961280;

    public ItemRobeArmor(Holder<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
    }

    public static int getColor(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, DEFAULT_COLOR);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        String n = this.getType() == Type.LEGGINGS ? "robes_2" : "robes_1";
        return ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/" + n + (layer.dyeable() ? "" : "_overlay") + ".png");
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return this.getType() == Type.BOOTS ? 2 : 3;
    }
}
