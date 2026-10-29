package thaumcraft.common.items.curios;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Изначальная жемчужина (порт ItemPrimordialPearl): прочность 8; при каждом крафте возвращается с повреждением +1 и по мере
 * расхода становится узелком (3–5) и пылинкой (6–7); пылинка 7 расходуется полностью. Не чинится. Модель — свойство type
 * (0 — жемчужина, 1 — узелок, 2 — пылинка), название — по тому же признаку.
 */
public class ItemPrimordialPearl extends Item {
    public ItemPrimordialPearl(Properties props) {
        super(props);
    }

    public static int getStage(ItemStack stack) {
        return stack.getDamageValue() < 3 ? 0 : (stack.getDamageValue() < 6 ? 1 : 2);
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return super.getDescriptionId(stack) + switch (getStage(stack)) {
            case 0 -> ".pearl";
            case 1 -> ".nodule";
            default -> ".mote";
        };
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return false;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return stack.getDamageValue() < 7;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        if (!this.hasCraftingRemainingItem(stack)) {
            return ItemStack.EMPTY;
        }
        ItemStack out = stack.copyWithCount(1);
        out.setDamageValue(stack.getDamageValue() + 1);
        return out;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
