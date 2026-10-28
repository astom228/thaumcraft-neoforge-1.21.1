package thaumcraft.common.items.tools;

import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tier;

/**
 * Таумиумная мотыга (порт ItemThaumiumHoe). ItemHoe 1.12: урон не меняется (рука), скорость = бонус материала + 1 − 4;
 * createAttributes 1.21 прибавляет бонус материала к урону — он вычитается.
 */
public class ItemThaumiumHoe extends HoeItem {
    public ItemThaumiumHoe(Tier tier, Properties props) {
        super(tier, props.attributes(hoeAttributes(tier)));
    }

    static net.minecraft.world.item.component.ItemAttributeModifiers hoeAttributes(Tier tier) {
        return HoeItem.createAttributes(tier, -tier.getAttackDamageBonus(), tier.getAttackDamageBonus() + 1.0F - 4.0F);
    }
}
