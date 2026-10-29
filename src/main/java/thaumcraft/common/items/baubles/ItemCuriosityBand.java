package thaumcraft.common.items.baubles;

import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Повязка любопытства (порт ItemCuriosityBand): слот головы; половина опыта из сфер превращается в знания — PlayerEvents.pickupXP. Рендер на голове — GogglesCurioRenderer. */
public class ItemCuriosityBand extends Item implements ICurioItem {
    public ItemCuriosityBand(Properties props) {
        super(props);
    }
}
