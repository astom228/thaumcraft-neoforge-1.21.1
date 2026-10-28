package thaumcraft.common.items.tools;

import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;

/** Таумиумная кирка (порт ItemThaumiumPickaxe): ItemPickaxe 1.12 — урон 1 + материал, скорость −2.8. */
public class ItemThaumiumPickaxe extends PickaxeItem {
    public ItemThaumiumPickaxe(Tier tier, Properties props) {
        super(tier, props.attributes(PickaxeItem.createAttributes(tier, 1.0F, -2.8F)));
    }
}
