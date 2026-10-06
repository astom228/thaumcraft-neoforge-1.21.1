package thaumcraft.common.items.tools;

import net.minecraft.world.item.Item;
import thaumcraft.api.items.IScribeTools;

/** Письменные принадлежности: 100 единиц чернил (прочность), не складываются. */
public class ItemScribingTools extends Item implements IScribeTools {
    public ItemScribingTools(Properties properties) {
        super(properties.stacksTo(1).durability(100));
    }
}
