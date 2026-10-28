package thaumcraft.common.items.tools;

import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tier;

/**
 * Таумиумный топор (порт ItemThaumiumAxe): в 1.12 ItemAxe(material, 8, −3) — урон ровно 8 без бонуса материала;
 * в 1.21 createAttributes прибавляет бонус материала, поэтому он вычитается.
 */
public class ItemThaumiumAxe extends AxeItem {
    public ItemThaumiumAxe(Tier tier, Properties props) {
        super(tier, props.attributes(AxeItem.createAttributes(tier, 8.0F - tier.getAttackDamageBonus(), -3.0F)));
    }
}
