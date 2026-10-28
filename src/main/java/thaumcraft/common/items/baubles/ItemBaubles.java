package thaumcraft.common.items.baubles;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.items.IVisDiscountGear;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * Простые безделушки (порт ItemBaubles, варианты metadata 0–6 — отдельные предметы, ID_MAPPING.md): амулеты, кольца, пояса.
 * Слот Curios задаётся тегами curios:necklace / ring / belt (BaubleType оригинала, DEVIATIONS 1).
 * Кольцо ученика (meta 3) — скидка Вис 5%.
 */
public class ItemBaubles extends Item implements ICurioItem, IVisDiscountGear {
    private final int meta;

    public ItemBaubles(int meta, Properties props) {
        super(props);
        this.meta = meta;
    }

    @Override
    public int getVisDiscount(ItemStack stack, Player player) {
        return this.meta == 3 ? 5 : 0;
    }
}
