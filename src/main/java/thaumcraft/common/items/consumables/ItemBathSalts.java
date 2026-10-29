package thaumcraft.common.items.consumables;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Очищающие соли для ванн (порт ItemBathSalts): брошенные, живут 200 тиков; истаяв в источнике воды, превращают его в очищающую жидкость (EntityEvents). */
public class ItemBathSalts extends Item {
    public ItemBathSalts(Properties props) {
        super(props);
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, Level level) {
        return 200;
    }
}
