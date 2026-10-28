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
import thaumcraft.api.items.IGoggles;
import thaumcraft.api.items.IRevealer;
import thaumcraft.api.items.ItemsTC;
import thaumcraft.api.items.IVisDiscountGear;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Очки откровения (порт ItemGoggles): шлем (материал SPECIAL, прочность 350) или безделушка на голову (Curios head);
 * скидка Вис 5%, показывают аспекты в мире (IGoggles) и скрытое (IRevealer); чинятся алхимической латунью.
 * Рендер на голове как безделушки — GogglesCurioRenderer (клиент).
 */
public class ItemGoggles extends ArmorItem implements IVisDiscountGear, IRevealer, IGoggles, ICurioItem {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/goggles.png");

    public ItemGoggles(Holder<ArmorMaterial> material, Properties props) {
        super(material, Type.HELMET, props);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return TEX;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ItemsTC.ingotBrass.get()) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return 5;
    }

    @Override
    public boolean showNodes(ItemStack stack, LivingEntity player) {
        return true;
    }

    @Override
    public boolean showIngamePopups(ItemStack stack, LivingEntity player) {
        return true;
    }
}
