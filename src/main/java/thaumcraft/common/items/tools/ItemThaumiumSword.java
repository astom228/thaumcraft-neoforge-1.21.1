package thaumcraft.common.items.tools;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Таумиумный меч (порт ItemThaumiumSword): урон 3 + материал, скорость −2.4; чинится таумиумом (материал). */
public class ItemThaumiumSword extends SwordItem {
    public ItemThaumiumSword(Tier tier, Properties props) {
        super(tier, props.attributes(SwordItem.createAttributes(tier, 3, -2.4F)));
    }
}
