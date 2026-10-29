package thaumcraft.common.items;

import net.minecraft.world.item.CreativeModeTab;

/** Предмет со своими стеками во вкладке творческого режима (getSubItems оригинала). */
public interface ICreativeVariants {
    void addCreativeVariants(CreativeModeTab.Output output);
}
