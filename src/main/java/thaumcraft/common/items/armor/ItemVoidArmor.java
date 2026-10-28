package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.items.IWarpingGear;

/**
 * Броня из пустотного металла (порт ItemVoidArmor): Искажение 1, самопочинка. В 1.12 срабатывали и onUpdate (в любом слоте),
 * и onArmorTick (надетая) — надетая чинится на 2 ед. в секунду, лежащая в инвентаре — на 1.
 */
public class ItemVoidArmor extends ArmorItem implements IWarpingGear {
    private static final ResourceLocation TEX_1 = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/void_1.png");
    private static final ResourceLocation TEX_2 = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/void_2.png");

    public ItemVoidArmor(Holder<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return this.getType() == Type.LEGGINGS ? TEX_2 : TEX_1;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || entity.tickCount % 20 != 0) {
            return;
        }
        if (stack.isDamaged() && entity instanceof net.minecraft.world.entity.LivingEntity) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
        if (stack.isDamaged() && isWorn(stack, entity, slot)) {
            stack.setDamageValue(stack.getDamageValue() - 1);
        }
    }

    /** Надет ли этот стек (inventoryTick вызывается и для слотов брони — с их номером 0–3). */
    public static boolean isWorn(ItemStack stack, Entity entity, int slot) {
        return entity instanceof Player p && slot >= 0 && slot < p.getInventory().armor.size() && p.getInventory().armor.get(slot) == stack;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return 1;
    }
}
