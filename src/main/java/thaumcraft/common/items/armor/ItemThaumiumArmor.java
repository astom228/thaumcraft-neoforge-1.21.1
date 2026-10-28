package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/** Таумиумная броня (порт ItemThaumiumArmor): текстуры оригинала, чинится таумиумом (материал). */
public class ItemThaumiumArmor extends ArmorItem {
    private static final ResourceLocation TEX_1 = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/thaumium_1.png");
    private static final ResourceLocation TEX_2 = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/thaumium_2.png");

    public ItemThaumiumArmor(Holder<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return this.getType() == Type.LEGGINGS ? TEX_2 : TEX_1;
    }
}
