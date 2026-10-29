package thaumcraft.common.items.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.items.IVisDiscountGear;
import thaumcraft.api.items.IWarpingGear;

/**
 * Броня Багрового культа (порты ItemCultistPlateArmor, ItemCultistRobeArmor, ItemCultistLeaderArmor, ItemCultistBoots —
 * отличаются только текстурой, моделью и бонусами, поэтому один класс). Чинится железом. Роба и ботинки — скидка Вис 1% и
 * Искажение 1. Модели: латы — ModelKnightArmor, роба — ModelRobe, претор — ModelLeaderArmor, ботинки — обычная.
 */
public class ItemCultistArmor extends ArmorItem implements IVisDiscountGear, IWarpingGear {
    public enum Kind {
        PLATE("cultist_plate_armor", false),
        ROBE("cultist_robe_armor", true),
        LEADER("cultist_leader_armor", false),
        BOOTS("cultistboots", true);

        final ResourceLocation texture;
        final boolean occult;

        Kind(String tex, boolean occult) {
            this.texture = ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/entity/armor/" + tex + ".png");
            this.occult = occult;
        }
    }

    public final Kind kind;

    public ItemCultistArmor(Kind kind, Holder<ArmorMaterial> material, Type type, Properties props) {
        super(material, type, props);
        this.kind = kind;
    }

    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        // TODO этап 16: на EntityInhabitedZombie у лат — textures/entity/armor/zombie_plate_armor.png
        return this.kind.texture;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(Items.IRON_INGOT) || super.isValidRepairItem(stack, repair);
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return this.kind.occult ? 1 : 0;
    }

    @Override
    public int getWarp(ItemStack stack, Player player) {
        return this.kind.occult ? 1 : 0;
    }
}
