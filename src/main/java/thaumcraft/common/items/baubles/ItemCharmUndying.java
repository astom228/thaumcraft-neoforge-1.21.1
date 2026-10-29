package thaumcraft.common.items.baubles;

import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Оберег бессмертия (порт ItemCharmUndying): слот оберега; спасает от смерти, как тотем, и исчезает — PlayerEvents.onDeath. */
public class ItemCharmUndying extends Item implements ICurioItem {
    public ItemCharmUndying(Properties props) {
        super(props);
    }
}
