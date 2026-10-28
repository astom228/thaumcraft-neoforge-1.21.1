package thaumcraft.common.items.tools;

import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;

/** Таумиумная лопата (порт ItemThaumiumShovel): ItemSpade 1.12 — урон 1.5 + материал, скорость −3. */
public class ItemThaumiumShovel extends ShovelItem {
    public ItemThaumiumShovel(Tier tier, Properties props) {
        super(tier, props.attributes(ShovelItem.createAttributes(tier, 1.5F, -3.0F)));
    }
}
